package ru.archdemon.atol.webserver.servlets;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import ru.archdemon.atol.webserver.Utils;
import ru.archdemon.atol.webserver.db.DBException;
import ru.archdemon.atol.webserver.db.DBInstance;
import ru.archdemon.atol.webserver.db.NotFoundException;
import ru.archdemon.atol.webserver.entities.Device;
import ru.atol.drivers10.fptr.Fptr;
import ru.atol.drivers10.fptr.IFptr;

public class OperationServlet extends HttpServlet {

    private static final long TIMEOUT = 30;
    private static final Logger logger = LogManager.getLogger(OperationServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String deviceId = req.getParameter("deviceID");

        try {
            String command = req.getPathInfo().split("/")[1];
            Device device = DBInstance.db.getDevice(deviceId);

            JSONObject task = new JSONObject();
            switch (command) {
                case "queryDeviceInfo":
                    task.put("type", "getDeviceInfo");
                    break;
                case "queryDeviceStatus":
                    task.put("type", "getDeviceStatus");
                    break;
                case "queryShiftStatus":
                    task.put("type", "getShiftStatus");
                    break;
                case "queryShiftTotals":
                    task.put("type", "getShiftTotals");
                    break;
                case "queryIncomeTotals":
                    task.put("type", "getShiftTotals"); // incomeTotals
                    break;
                case "queryOutcomeTotals":
                    task.put("type", "getShiftTotals"); // outcomeTotals
                    break;
                case "queryReceiptTotals":
                    task.put("type", "getShiftTotals"); // receiptsTotals
                    break;
                case "queryFnInfo":
                    task.put("type", "getFnInfo");
                    break;
                case "queryFnStatus":
                    task.put("type", "getFnStatus");
                    break;
                case "queryOfdExchangeStatus":
                    task.put("type", "OfdExchangeStatus");
                    break;
                case "queryIsmExchangeStatus":
                    task.put("type", "ismExchangeStatus");
                    break;
                case "queryLicenses":
                    task.put("type", "getLicenses");
                    break;
                case "queryDeviceSettings":
                    task.put("type", "getDeviceSettings"); // -
                    break;
                default:
                    throw new NotFoundException("Command not found");
            }

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                IFptr fptr = null;
                try {
                    fptr = new Fptr();
                    fptr.setSettings(device.getSettings());

                    while (!fptr.isOpened()) {
                        fptr.open();
                        if (!fptr.isOpened()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                            }
                        }
                    }

                    fptr.setParam(IFptr.LIBFPTR_PARAM_JSON_DATA, task.toJSONString());
                    if (fptr.processJson() == IFptr.LIBFPTR_OK) {
                        return fptr.getParamString(IFptr.LIBFPTR_PARAM_JSON_DATA);
                    }

                    throw new RuntimeException(fptr.errorDescription());
                } finally {
                    if (fptr != null && fptr.isOpened()) {
                        fptr.close();
                    }
                }
            }).orTimeout(TIMEOUT, TimeUnit.SECONDS);

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(future.get());
        } catch (NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (DBException | RuntimeException | InterruptedException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TimeoutException) {
                resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT, e.getCause().getMessage());
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
    }

}
