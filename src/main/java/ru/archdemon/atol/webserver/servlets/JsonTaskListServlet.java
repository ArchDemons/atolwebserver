package ru.archdemon.atol.webserver.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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
import ru.archdemon.atol.webserver.db.NotUniqueKeyException;
import ru.archdemon.atol.webserver.entities.Task;

public class JsonTaskListServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(JsonTaskListServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        JSONArray subTasks = null;
        resp.setStatus(HttpServletResponse.SC_CREATED);

        JSONObject json;
        String requestJson;
        String body = Utils.readFromReader(new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8)));
        try {
            json = (JSONObject) JSONValue.parseWithException(body);
            if (!json.containsKey("uuid") || !json.containsKey("request")) {
                resp.sendError(400, "\"uuid\" and / or \"request\" not found");
                return;
            }
            requestJson = json.get("request").toString();
        } catch (ParseException e) {
            logger.error(e.getMessage(), subTasks);
            resp.sendError(400, e.getMessage());

            return;
        }

        if (json.get("request") instanceof JSONArray) {
            subTasks = (JSONArray) json.get("request");
        } else {
            subTasks = new JSONArray();
            subTasks.add(json.get("request"));
        }

        int fiscalTasksCount = 0;
        for (int i = 0; i < subTasks.size(); i++) {
            JSONObject subtask = (JSONObject) subTasks.get(i);
            if (subtask.containsKey("type") && Utils.isFiscalOperation((String) subtask.get("type"))) {
                fiscalTasksCount++;
            }
        }
        if (fiscalTasksCount > 1) {
            String error = String.format("Too many fiscal sub-tasks - %d", fiscalTasksCount);
            logger.error(error);
            resp.sendError(400, error);
            return;
        }

        Task task = new Task();
        task.setUuid((String) json.get("uuid"));
        task.setData(requestJson);
        task.setTimestamp(new Date());

        logger.info(String.format("%s %s [%s]", req.getMethod(), req.getRequestURI(), body));

        try {
            DBInstance.db.addTask(task);
        } catch (NotUniqueKeyException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(409, e.getMessage());
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(500, e.getMessage());
        }
        resp.setStatus(201);
    }
}
