package ru.archdemon.atol.webserver.workers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.atol.drivers10.fptr.Fptr;
import ru.atol.drivers10.fptr.IFptr;
import ru.archdemon.atol.webserver.Consts;
import ru.archdemon.atol.webserver.Utils;
import ru.archdemon.atol.webserver.db.DBException;
import ru.archdemon.atol.webserver.db.DBInstance;
import ru.archdemon.atol.webserver.entities.*;

public class DriverWorker extends Thread {

    private static final Logger logger = LogManager.getLogger(DriverWorker.class);

    public static final Map<Device, IFptr> FPTRS = new HashMap<>();

    private boolean isNeedBlock(int error) {
        switch (error) {
            case IFptr.LIBFPTR_ERROR_NO_CONNECTION:
            case IFptr.LIBFPTR_ERROR_PORT_BUSY:
            case IFptr.LIBFPTR_ERROR_PORT_NOT_AVAILABLE:
            case IFptr.LIBFPTR_ERROR_FN_EXCHANGE:
            case IFptr.LIBFPTR_ERROR_FN_INVALID_FORMAT:
            case IFptr.LIBFPTR_ERROR_FN_INVALID_STATE:
            case IFptr.LIBFPTR_ERROR_FN_FAULT:
            case IFptr.LIBFPTR_ERROR_FN_CRYPTO_FAULT:
            case IFptr.LIBFPTR_ERROR_FN_EXPIRED:
            case IFptr.LIBFPTR_ERROR_FN_OVERFLOW:
            case IFptr.LIBFPTR_ERROR_FN_INVALID_DATE_TIME:
            case IFptr.LIBFPTR_ERROR_FN_TOTAL_OVERFLOW:
            case IFptr.LIBFPTR_ERROR_FN_INVALID_COMMAND:
            case IFptr.LIBFPTR_ERROR_FN_COMMAND_OVERFLOW:
            case IFptr.LIBFPTR_ERROR_FN_NO_TRANSPORT_CONNECTION:
            case IFptr.LIBFPTR_ERROR_FN_CRYPTO_HAS_EXPIRED:
            case IFptr.LIBFPTR_ERROR_FN_RESOURCE_HAS_EXPIRED:
            case IFptr.LIBFPTR_ERROR_INVALID_MESSAGE_FROM_OFD:
            case IFptr.LIBFPTR_ERROR_FN_SHIFT_EXPIRED:
            case IFptr.LIBFPTR_ERROR_FN_INVALID_TIME_DIFFERENCE:
            case IFptr.LIBFPTR_ERROR_FN_INTERFACE:
            case IFptr.LIBFPTR_ERROR_UNKNOWN:
                return true;
        }

        return false;
    }

    private IFptr getFptr(Device device) {
        if (!FPTRS.containsKey(device)) {
            FPTRS.put(device, new Fptr());
        }

        return FPTRS.get(device);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            List<Device> devices;

            try {
                devices = DBInstance.db.getDevices();
            } catch (DBException e) {
                logger.error(e.getMessage());
                return;
            }

            for (Device device : devices) {
                IFptr fptr = getFptr(device);

                if (!device.isActive()) {
                    if (fptr.isOpened()) {
                        fptr.close();
                    }
                    continue;
                }

                if (!fptr.isOpened()) {
                    fptr.setSettings(device.getSettings());
                    fptr.open();
                    if (!fptr.isOpened()) {
                        continue;
                    }
                }

                Request task = null;
                try {
                    BlockRecord block = DBInstance.db.getBlockState(device.getId());
                    if (block != null) {
                        logger.info(String.format("Обнаружена блокировка очереди задачей '%s'", block.getUuid()));
                        fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_LAST_DOCUMENT);
                        if (fptr.fnQueryData() == IFptr.LIBFPTR_OK) {
                            boolean closed = fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENT_NUMBER) > Long.parseLong(block.getData());
                            fptr.continuePrint();

                            logger.info(String.format("Соединение восстановленно, задача '%s' %s",
                                    block.getUuid(), closed ? "выполнена" : "не выполнена"));

                            task = DBInstance.db.getTask(block.getUuid());
                            List<Result> subtasks = DBInstance.db.getTaskStatus(task.getId());

                            for (int i = 0; i < subtasks.size(); i++) {
                                Result subtask = (Result) subtasks.get(i);
                                if (subtask.getStatus() == Consts.STATUS_BLOCKED) {
                                    subtask.setStatus(closed ? Consts.STATUS_READY : Consts.STATUS_ERROR);
                                    if (closed) {
                                        subtask.setErrorCode(IFptr.LIBFPTR_OK);
                                        subtask.setErrorDescription("Ошибок нет");

                                        JSONObject o = new JSONObject();
                                        o.put("type", "getLastFiscalParams");
                                        o.put("forReceipt", Utils.isReceipt((String) ((JSONObject) task.getDataJson().get(i)).get("type")));
                                        fptr.setParam(IFptr.LIBFPTR_PARAM_JSON_DATA, o.toString());
                                        if (fptr.processJson() == 0) {
                                            subtask.setResultData(fptr.getParamString(IFptr.LIBFPTR_PARAM_JSON_DATA));
                                        }
                                    }
                                    DBInstance.db.updateSubTaskStatus(subtask);
                                    break;
                                }
                            }
                            logger.info(String.format("Обработка задачи '%s' завершена, разблокируем очередь", task.getUuid()));
                            DBInstance.db.setTaskReady(task.getUuid());
                            DBInstance.db.unblockDB(device.getId());
                        } else {
                            logger.warn("Не удалось восстановить состояние, продолжаем попытки...");
                            continue;
                        }
                    }
                } catch (DBException e) {
                    logger.error(e.getMessage(), e);

                    continue;
                }

                try {
                    task = DBInstance.db.getNextTask(device.getId());
                } catch (DBException e) {
                    logger.error(e.getMessage(), task);
                    continue;
                }
                if (task == null) {
                    continue;
                }
                logger.info(String.format("Найдена задача с id = '%s'", task.getUuid()));

                JSONArray subTasks = task.getDataJson();
                if (subTasks == null) {
                    logger.error("Ошибка разбора JSON");

                    continue;
                }
                logger.info(String.format("Подзадач - %d", subTasks.size()));

                boolean wasError = false;
                boolean blocked = false;
                for (int i = 0; i < subTasks.size(); i++) {
                    logger.info(String.format("Подзадача #%d...", i + 1));

                    Result status = new Result();
                    status.setNumber(i);
                    status.setRequestId(task.getId());
                    status.setStatus(Consts.STATUS_IN_PROGRESS);
                    try {
                        DBInstance.db.updateSubTaskStatus(status);
                    } catch (DBException e) {
                        logger.error(e.getMessage(), e);
                    }

                    if (wasError) {
                        status.setStatus(Consts.STATUS_INTERRUPTED_BY_PREVIOUS_ERRORS);
                        status.setErrorCode(IFptr.LIBFPTR_ERROR_INTERRUPTED_BY_PREVIOUS_ERRORS);
                        status.setErrorDescription("Выполнение прервано из-за предыдущих ошибок");
                    } else {
                        JSONObject subtask = (JSONObject) subTasks.get(i);
                        long lastDocumentNumber = -1L;
                        if (subtask.containsKey("type") && Utils.isFiscalOperation((String) subtask.get("type"))) {
                            fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_LAST_DOCUMENT);
                            if (fptr.fnQueryData() != IFptr.LIBFPTR_OK) {
                                status.setStatus(Consts.STATUS_ERROR);
                                status.setErrorCode(fptr.errorCode());
                                status.setErrorDescription(fptr.errorDescription());
                                wasError = true;
                            }
                            lastDocumentNumber = fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENT_NUMBER);
                        }

                        if (!wasError) {
                            fptr.setParam(IFptr.LIBFPTR_PARAM_JSON_DATA, subtask.toJSONString());
                            if (fptr.processJson() == IFptr.LIBFPTR_OK) {
                                status.setStatus(Consts.STATUS_READY);
                                status.setResultData(fptr.getParamString(IFptr.LIBFPTR_PARAM_JSON_DATA));
                            } else {
                                if (isNeedBlock(fptr.errorCode()) && lastDocumentNumber != -1L) {
                                    try {
                                        BlockRecord block = new BlockRecord();
                                        block.setUuid(task.getUuid());
                                        block.setDeviceId(task.getDeviceId());
                                        block.setData(String.valueOf(lastDocumentNumber));
                                        DBInstance.db.blockDB(block);
                                    } catch (DBException e) {
                                        logger.error(e.getMessage(), e);
                                    }
                                    blocked = true;
                                    status.setStatus(Consts.STATUS_BLOCKED);
                                } else {
                                    status.setStatus(Consts.STATUS_ERROR);
                                }
                                wasError = true;
                            }
                            status.setErrorCode(fptr.errorCode());
                            status.setErrorDescription(fptr.errorDescription());
                        }
                    }

                    try {
                        switch (status.getStatus()) {
                            case Consts.STATUS_BLOCKED:
                                logger.info(String.format("Подзадача #%d заблокировала очередь", i + 1));
                                break;
                            case Consts.STATUS_READY:
                                logger.info(String.format("Подзадача #%d выполнена без ошибок", i + 1));
                                break;
                            case Consts.STATUS_ERROR:
                                logger.info(String.format("Подзадача #%d завершена с ошибкой", i + 1));
                                break;
                            case Consts.STATUS_INTERRUPTED_BY_PREVIOUS_ERRORS:
                                logger.info(String.format("Подзадача #%d прервана по причине предыдущих ошибок", i + 1));
                                break;
                        }
                        DBInstance.db.updateSubTaskStatus(status);
                    } catch (DBException e) {
                        logger.error(e.getMessage(), e);
                    }
                }

                if (!blocked) {
                    try {
                        DBInstance.db.setTaskReady(task.getUuid());
                        logger.info(String.format("Обработка задачи '%s' завершена", task.getUuid()));
                    } catch (DBException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

}
