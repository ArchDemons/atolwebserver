package ru.archdemon.atol.webserver.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import ru.archdemon.atol.webserver.db.DBException;
import ru.archdemon.atol.webserver.db.DBInstance;
import ru.archdemon.atol.webserver.entities.Device;
import ru.archdemon.atol.webserver.workers.DriverWorker;
import ru.atol.drivers10.fptr.Fptr;
import ru.atol.drivers10.fptr.IFptr;

public class OperationServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(OperationServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String deviceId = req.getParameterValues("deviceID")[0];

        try {
            String command = req.getPathInfo().split("/")[1];

            Device device = DBInstance.db.getDevice(deviceId);
            IFptr fptr;
            boolean needClose = false;
            if (DriverWorker.FPTRS.containsKey(device)) {
                fptr = DriverWorker.FPTRS.get(device);
            } else {
                fptr = new Fptr();
                needClose = true;
                fptr.setSettings(device.getSettings());
            }

            if (!fptr.isOpened()) {
                fptr.open();
                if (!fptr.isOpened()) {
                    throw new RuntimeException("Cannot open fptr");
                }
            }

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
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

            fptr.setParam(IFptr.LIBFPTR_PARAM_JSON_DATA, task.toJSONString());
            if (fptr.processJson() == IFptr.LIBFPTR_OK) {
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(fptr.getParamString(IFptr.LIBFPTR_PARAM_JSON_DATA));
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, fptr.errorDescription());
            }
            if (needClose) {
                fptr.close();
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
