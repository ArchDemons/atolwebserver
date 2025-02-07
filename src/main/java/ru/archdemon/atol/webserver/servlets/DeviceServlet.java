package ru.archdemon.atol.webserver.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import ru.archdemon.atol.webserver.Utils;
import ru.archdemon.atol.webserver.db.DBException;
import ru.archdemon.atol.webserver.db.DBInstance;
import ru.archdemon.atol.webserver.db.NotFoundException;
import ru.archdemon.atol.webserver.db.NotUniqueKeyException;
import ru.archdemon.atol.webserver.entities.Device;

public class DeviceServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(DeviceServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            if (req.getPathInfo() != null) {
                Device device = DBInstance.db.getDevice(req.getPathInfo().split("/")[1]);
                resp.getWriter().write(device.toJson().toJSONString());
            } else {
                JSONArray response = new JSONArray();
                List<Device> devices = DBInstance.db.getDevices();
                for (Device device : devices) {
                    response.add(device.toJson());
                }

                resp.getWriter().write(response.toJSONString());
            }
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        JSONObject json;
        String body = Utils.readFromReader(new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8)));
        try {
            json = (JSONObject) JSONValue.parseWithException(body);
            if (!json.containsKey("id")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Attribute 'id' not found");
                return;
            }
        } catch (ParseException e) {
            logger.error(e.getMessage());
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        Device device = new Device(json.get("id").toString());
        device.setName(json.get("name").toString());
        try {
            DBInstance.db.addDevice(device);
            device = DBInstance.db.getDevice(device.getId());

            resp.getWriter().write(device.toJson().toJSONString());
            resp.setStatus(HttpServletResponse.SC_CREATED);
        } catch (NotUniqueKeyException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            Device device = DBInstance.db.getDevice(req.getPathInfo().split("/")[1]);
            DBInstance.db.deleteDevice(device.getId());
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "No deviceID");
        } catch (NotFoundException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No device found");
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String body = Utils.readFromReader(new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8)));

        try {
            JSONObject json = (JSONObject) JSONValue.parseWithException(body);

            Device device = DBInstance.db.getDevice(req.getPathInfo().split("/")[1]);

            device.setId(json.get("id").toString());
            device.setName(json.get("name").toString());

            device.setActive(Boolean.parseBoolean(json.get("isActive").toString()));
            device.setDefault(Boolean.parseBoolean(json.get("isDefault").toString()));

            JSONObject connectionSettings = (JSONObject) json.get("connectionSettings");
            device.setModel(Integer.parseInt(connectionSettings.get("model").toString()));
            device.setUserPassword(connectionSettings.get("userPassword").toString());
            device.setAccessPassword(connectionSettings.get("accessPassword").toString());
            device.setPort(connectionSettings.get("port").toString());
            device.setCom(connectionSettings.get("com").toString());
            device.setUsb(connectionSettings.get("usbDevice").toString());
            device.setBaudRate(Integer.parseInt(connectionSettings.get("baudRate").toString()));
            device.setIpAddr(connectionSettings.get("ipAddress").toString());
            device.setIpPort(Integer.parseInt(connectionSettings.get("ipPort").toString()));
            device.setMac(connectionSettings.get("mac").toString());
            device.setOfdChannel(connectionSettings.get("ofdChannel").toString());

            JSONObject otherSettings = (JSONObject) json.get("otherSettings");
            device.setUseGlobalSp(Boolean.parseBoolean(otherSettings.get("useGlobalScriptsSettings").toString()));
            device.setScriptsPath(otherSettings.get("scriptsPath").toString());
            device.setUseGlobalIcds(Boolean.parseBoolean(otherSettings.get("useGlobalInvertCashDrawerStatusFlag").toString()));
            device.setInvertCdStatus(Boolean.parseBoolean(otherSettings.get("invertCashDrawerStatus").toString()));
            device.setUseGlobalHl(Boolean.parseBoolean(otherSettings.get("useGlobalAdditionalHeaderLines").toString()));
            device.setHeaderLines(otherSettings.get("additionalHeaderLines").toString());
            device.setUseGlobalFl(Boolean.parseBoolean(otherSettings.get("useGlobalAdditionalFooterLines").toString()));
            device.setFooterLines(otherSettings.get("additionalFooterLines").toString());

            logger.info(String.format("%s %s [%s]", req.getMethod(), req.getRequestURI(), body));

            DBInstance.db.updateDevice(req.getPathInfo().split("/")[1], device);
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (ParseException e) {
            logger.error(e.getMessage(), body);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "No deviceID");
        } catch (NotFoundException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No device found");
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
