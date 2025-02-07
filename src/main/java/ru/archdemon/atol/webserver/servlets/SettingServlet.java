package ru.archdemon.atol.webserver.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import ru.archdemon.atol.webserver.Utils;
import ru.archdemon.atol.webserver.db.DBException;
import ru.archdemon.atol.webserver.db.DBInstance;
import ru.archdemon.atol.webserver.entities.Setting;

public class SettingServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(SettingServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            Setting setting = DBInstance.db.getSetting();
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(setting.toJson().toJSONString());
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

            Setting setting = new Setting();
            setting.setScriptsPath(json.get("scriptsPath").toString());
            setting.setInvertCdStatus(Boolean.parseBoolean(json.get("invertCashDrawerStatus").toString()));
            setting.setHeaderLines(json.get("additionalHeaderLines").toString());
            setting.setFooterLines(json.get("additionalFooterLines").toString());
            setting.setBlockOnPrintErrors(Boolean.parseBoolean(json.get("blockQueueOnPrintErrors").toString()));
            setting.setDeleteRequestsAfter(Integer.parseInt(json.get("deleteRequestsAfter").toString()));
            setting.setValidateRequestsOnAdd(Boolean.parseBoolean(json.get("validateRequestsOnAdd").toString()));

            logger.info(String.format("%s %s [%s]", req.getMethod(), req.getRequestURI(), body));

            DBInstance.db.updateSetting(setting);
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (ParseException e) {
            logger.error(e.getMessage(), body);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
