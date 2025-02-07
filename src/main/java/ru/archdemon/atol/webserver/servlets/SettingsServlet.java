package ru.archdemon.atol.webserver.servlets;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ru.atol.drivers10.fptr.Fptr;
import ru.atol.drivers10.fptr.IFptr;
import ru.archdemon.atol.webserver.settings.SelectItem;
import ru.archdemon.atol.webserver.settings.Settings;

public class SettingsServlet extends HttpServlet {

    private List<SelectItem> getDeviceSelect(IFptr fptr, String selectKey, JSONObject currentDeviceSettings)
            throws ParseException {

        JSONParser parser = new JSONParser();
        List<SelectItem> select = new LinkedList<>();
        Object selectedJson = currentDeviceSettings.get(selectKey);
        String selected = (selectedJson == null) ? fptr.getSingleSetting(selectKey) : selectedJson.toString();

        fptr.setParam(IFptr.LIBFPTR_PARAM_MAPPING_KEY, selectKey);
        fptr.utilMapping();
        JSONArray mapping = (JSONArray) parser.parse(fptr.getParamString(IFptr.LIBFPTR_PARAM_MAPPING_VALUE));

        for (Object o : mapping) {
            select.add(new SelectItem(
                    ((JSONObject) o).get("key").toString(),
                    ((JSONObject) o).get("description").toString(),
                    ((JSONObject) o).get("key").equals(selected)));
        }

        return select;
    }

    private String getDeviceString(IFptr fptr, String key, JSONObject currentDeviceSettings) {
        Object currentValueJson = currentDeviceSettings.get(key);
        if (currentValueJson == null) {
            return fptr.getSingleSetting(key);
        }
        return currentValueJson.toString();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        JSONObject common, device;
        try {
            JSONObject settings = Settings.load();
            device = (JSONObject) ((JSONObject) settings.get("devices")).get("main");
            common = (JSONObject) settings.get("common");
        } catch (ParseException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        Fptr fptr = new Fptr();

        try {
            req.setAttribute("channels", getDeviceSelect(fptr, "Port", device));
            req.setAttribute("usbDevicePaths", getDeviceSelect(fptr, "UsbDevicePath", device));
            req.setAttribute("coms", getDeviceSelect(fptr, "ComFile", device));
            req.setAttribute("baudrates", getDeviceSelect(fptr, "BaudRate", device));
            req.setAttribute("tcpAddress", getDeviceString(fptr, "IPAddress", device));
            req.setAttribute("tcpPort", getDeviceString(fptr, "IPPort", device));
            req.setAttribute("ofdChannels", getDeviceSelect(fptr, "OfdChannel", device));

            req.setAttribute("isActive", ((Boolean) common.get("is_active")));
        } catch (ParseException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        req.getRequestDispatcher("settings.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        JSONObject settings;
        try {
            settings = Settings.load();
        } catch (ParseException e) {
            settings = new JSONObject();
        }

        JSONObject commonSettings = (JSONObject) settings.get("common");
        JSONObject deviceSettings = (JSONObject) settings.get("devices");
        JSONObject mainDeviceSettings = (JSONObject) deviceSettings.get("main");

        String channel = req.getParameter("channels");
        String usbDevicePath = req.getParameter("usbDevicePaths");
        String com = req.getParameter("coms");
        String baudrate = req.getParameter("baudrates");
        String tcpAddress = req.getParameter("tcpAddress");
        String tcpPort = req.getParameter("tcpPort");
        String ofdChannel = req.getParameter("ofdChannels");

        commonSettings.put("is_active", (req.getParameter("isActive") != null));

        mainDeviceSettings.put("Port", channel);
        mainDeviceSettings.put("UsbDevicePath", usbDevicePath);
        mainDeviceSettings.put("ComFile", com);
        mainDeviceSettings.put("BaudRate", baudrate);
        mainDeviceSettings.put("OfdChannel", ofdChannel);
        mainDeviceSettings.put("IPAddress", tcpAddress);
        mainDeviceSettings.put("IPPort", tcpPort);

        Settings.save(settings);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("<div>Настройки успешно записаны. Для применения перезапустите WEB-сервер или ПК.</div>");
        resp.getWriter().write("<a href=/settings>Вернуться к настройкам</a>");
    }
}
