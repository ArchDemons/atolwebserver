package ru.archdemon.atol.webserver.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ru.atol.drivers10.fptr.Fptr;
import ru.atol.drivers10.fptr.IFptr;

public class UtilServlet extends HttpServlet {

    private JSONArray getDeviceMapping(IFptr fptr, String selectKey)
            throws ParseException {

        fptr.setParam(IFptr.LIBFPTR_PARAM_MAPPING_KEY, selectKey);
        fptr.utilMapping();

        JSONParser parser = new JSONParser();
        return (JSONArray) parser.parse(fptr.getParamString(IFptr.LIBFPTR_PARAM_MAPPING_VALUE));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Fptr fptr = new Fptr();

        try {
            String command = req.getPathInfo().split("/")[1];

            switch (command) {
                case "deviceParametersStruct":
                    JSONObject connection = new JSONObject();
                    connection.put("accessPassword", "");
                    connection.put("baudRate", 1200);
                    connection.put("com", "");
                    connection.put("ipAddress", "");
                    connection.put("ipPort", 0);
                    connection.put("mac", "");
                    connection.put("model", 500);
                    connection.put("ofdChannel", "auto");
                    connection.put("port", "usb");
                    connection.put("usbDevice", "auto");
                    connection.put("userPassword", "");

                    JSONObject other = new JSONObject();
                    other.put("additionalFooterLines", "");
                    other.put("additionalHeaderLines", "");
                    other.put("invertCashDrawerStatus", false);
                    other.put("scriptsPath", "");
                    other.put("useGlobalAdditionalFooterLines", true);
                    other.put("useGlobalAdditionalHeaderLines", true);
                    other.put("useGlobalInvertCashDrawerStatusFlag", true);
                    other.put("useGlobalScriptsSettings", true);

                    JSONObject result = new JSONObject();
                    result.put("connectionSettings", connection);
                    result.put("id", "KKT_1");
                    result.put("isActive", true);
                    result.put("isDefault", false);
                    result.put("name", "АТОЛ 22Ф №1");
                    result.put("otherSettings", other);

                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                    resp.getWriter().write(result.toJSONString());
                    break;
                case "mapping":
                    JSONObject response = new JSONObject();
                    response.put("model", getDeviceMapping(fptr, "Model"));
                    response.put("baudRate", getDeviceMapping(fptr, "BaudRate"));
                    response.put("com", getDeviceMapping(fptr, "ComFile"));
                    response.put("ofdChannel", getDeviceMapping(fptr, "OfdChannel"));
                    response.put("port", getDeviceMapping(fptr, "Port"));
                    response.put("usbDevice", getDeviceMapping(fptr, "UsbDevicePath"));

                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                    resp.getWriter().write(response.toJSONString());
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (ParseException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (ArrayIndexOutOfBoundsException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}
