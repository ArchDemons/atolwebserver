package ru.archdemon.atol.webserver.workers;

import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ru.atol.drivers10.fptr.Fptr;
import ru.atol.drivers10.fptr.IFptr;
import ru.archdemon.atol.webserver.Consts;
import ru.archdemon.atol.webserver.Utils;
import ru.archdemon.atol.webserver.db.DBException;
import ru.archdemon.atol.webserver.db.DBInstance;
import ru.archdemon.atol.webserver.entities.BlockRecord;
import ru.archdemon.atol.webserver.entities.SubtaskStatus;
import ru.archdemon.atol.webserver.entities.Task;
import ru.archdemon.atol.webserver.settings.Settings;

public class DriverWorker extends Thread {

    private static final Logger logger = LogManager.getLogger(DriverWorker.class);

    private final IFptr fptr = new Fptr();

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

    @Override
    public void run() {
        boolean opened = false;
        int sleepTimeout = 100;
        while (!isInterrupted()) {
            Task task = null;
            try {
                Thread.sleep(sleepTimeout);
            } catch (InterruptedException e) {
                return;
            }

            if (!opened) {
                try {
                    this.fptr.setSettings(loadDriverSettings());
                } catch (IOException | ParseException e) {
                    logger.error(e.getMessage());
                    return;
                }
                this.fptr.open();
                opened = this.fptr.isOpened();
            }

            if (!opened) {
                sleepTimeout = 5000;
                continue;
            }
            sleepTimeout = 100;

            try {
                BlockRecord block = DBInstance.db.getBlockState();
                if (!block.getUuid().isEmpty()) {
                    logger.info(String.format("Обнаружена блокировка очереди задачей '%s'", block.getUuid()));
                    this.fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_LAST_DOCUMENT);
                    if (this.fptr.fnQueryData() == 0) {
                        boolean closed = (this.fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENT_NUMBER) > block.getDocumentNumber());
                        this.fptr.continuePrint();

                        logger.info(String.format("Соединение восстановленно, задача '%s' %s",
                                block.getUuid(), closed ? "выполнена" : "не выполнена"));

                        List<SubtaskStatus> subtasks = DBInstance.db.getTaskStatus(block.getUuid());
                        task = DBInstance.db.getTask(block.getUuid());

                        for (int i = 0; i < subtasks.size(); i++) {
                            SubtaskStatus subtask = (SubtaskStatus) subtasks.get(i);
                            if (subtask.getStatus() == Consts.STATUS_BLOCKED) {
                                subtask.setStatus(closed ? Consts.STATUS_READY : Consts.STATUS_ERROR);
                                if (closed) {
                                    subtask.setErrorCode(IFptr.LIBFPTR_OK);
                                    subtask.setErrorDescription("Ошибок нет");

                                    JSONObject o = new JSONObject();
                                    o.put("type", "getLastFiscalParams");
                                    o.put("forReceipt", Utils.isReceipt((String) ((JSONObject) task.getDataJson().get(i)).get("type")));
                                    this.fptr.setParam(IFptr.LIBFPTR_PARAM_JSON_DATA, o.toString());
                                    if (this.fptr.processJson() == 0) {
                                        subtask.setResultData(this.fptr.getParamString(IFptr.LIBFPTR_PARAM_JSON_DATA));
                                    }
                                }
                                DBInstance.db.updateSubTaskStatus(task.getUuid(), i, subtask);

                                break;
                            }
                        }
                        logger.info(String.format("Обработка задачи '%s' завершена, разблокируем очередь", task.getUuid()));
                        DBInstance.db.setTaskReady(task.getUuid());
                        DBInstance.db.unblockDB();
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
                task = DBInstance.db.getNextTask();
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

                SubtaskStatus status = new SubtaskStatus();
                status.setStatus(Consts.STATUS_IN_PROGRESS);
                try {
                    DBInstance.db.updateSubTaskStatus(task.getUuid(), i, status);
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
                        this.fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_LAST_DOCUMENT);
                        if (this.fptr.fnQueryData() < 0) {
                            status.setStatus(Consts.STATUS_ERROR);
                            status.setErrorCode(this.fptr.errorCode());
                            status.setErrorDescription(this.fptr.errorDescription());
                            wasError = true;
                        }
                        lastDocumentNumber = this.fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENT_NUMBER);
                    }

                    if (!wasError) {
                        this.fptr.setParam(IFptr.LIBFPTR_PARAM_JSON_DATA, subtask.toJSONString());
                        if (this.fptr.processJson() < 0) {

                            if (isNeedBlock(this.fptr.errorCode()) && lastDocumentNumber != -1L) {
                                try {
                                    DBInstance.db.blockDB(new BlockRecord(task.getUuid(), lastDocumentNumber));
                                } catch (DBException e) {
                                    logger.error(e.getMessage(), e);
                                }
                                blocked = true;
                                status.setStatus(Consts.STATUS_BLOCKED);
                            } else {
                                status.setStatus(Consts.STATUS_ERROR);
                            }
                            wasError = true;
                        } else {
                            status.setStatus(Consts.STATUS_READY);
                            status.setResultData(this.fptr.getParamString(IFptr.LIBFPTR_PARAM_JSON_DATA));
                        }
                        status.setErrorCode(this.fptr.errorCode());
                        status.setErrorDescription(this.fptr.errorDescription());
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
                    DBInstance.db.updateSubTaskStatus(task.getUuid(), i, status);
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

    private String loadDriverSettings() throws IOException, ParseException {
        JSONObject settings = (JSONObject) Settings.load().get("devices");
        settings = (JSONObject) settings.get("main");
        settings.put("Model", 500);

        return settings.toString();
    }
}
