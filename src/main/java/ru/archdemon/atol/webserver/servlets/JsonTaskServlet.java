package ru.archdemon.atol.webserver.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
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
import ru.archdemon.atol.webserver.Consts;
import ru.archdemon.atol.webserver.Utils;
import ru.archdemon.atol.webserver.db.DBException;
import ru.archdemon.atol.webserver.db.DBInstance;
import ru.archdemon.atol.webserver.db.NotUniqueKeyException;
import ru.archdemon.atol.webserver.entities.BlockRecord;
import ru.archdemon.atol.webserver.entities.Result;
import ru.archdemon.atol.webserver.entities.Request;

public class JsonTaskServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(JsonTaskServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        List<Result> subTasks;

        try {
            String uuid = req.getPathInfo().split("/")[1];
            Request task = DBInstance.db.getTask(uuid);
            subTasks = DBInstance.db.getTaskStatus(task.getId());
        } catch (NotUniqueKeyException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_CONFLICT, e.getMessage());
            return;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No UUID");
            return;
        }

        if (subTasks == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        logger.info(String.format("%s %s", req.getMethod(), req.getRequestURI()));

        JSONArray results = new JSONArray();
        for (Result s : subTasks) {
            results.add(s.toJson());
        }

        JSONObject response = new JSONObject();
        response.put("results", results);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(response.toJSONString());

        logger.info(String.format("%d %s", resp.getStatus(), response.toJSONString()));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (req.getPathInfo() != null) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String deviceId = req.getParameter("deviceID");

        JSONObject json;
        String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        try {
            json = (JSONObject) JSONValue.parseWithException(body);
            if (!json.containsKey("uuid") || !json.containsKey("request")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'uuid' and / or 'request' not found");
                return;
            }
        } catch (ParseException e) {
            logger.error(e.getMessage(), body);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        JSONArray subTasks;
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
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, error);
            return;
        }

        Request task = new Request();
        task.setDeviceId(deviceId);
        task.setUuid(json.get("uuid").toString());
        task.setData(json.get("request").toString());
        task.setCreatedTime(new Date());

        logger.info(String.format("%s %s [%s]", req.getMethod(), req.getRequestURI(), body));

        try {
            DBInstance.db.addTask(task);
            BlockRecord block = DBInstance.db.getBlockState(deviceId);

            JSONObject response = new JSONObject();
            response.put("number", 0);
            response.put("uuid", task.getUuid());
            response.put("isBlocked", block != null);
            response.put("blockedUUID", block != null ? block.getUuid() : "");

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(response.toJSONString());
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

        String uuid;
        Request task;
        List<Result> subTasks;

        try {
            uuid = req.getPathInfo().split("/")[1];
            task = DBInstance.db.getTask(uuid);
            if (task == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            subTasks = DBInstance.db.getTaskStatus(task.getId());
        } catch (NotUniqueKeyException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_CONFLICT, e.getMessage());
            return;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No UUID");
            return;
        }

        logger.info(String.format("%s %s", req.getMethod(), req.getRequestURI()));

        if (task.isReady()) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Task done or canceled");
            return;
        }

        for (int i = 0; i < subTasks.size(); i++) {
            if (((Result) subTasks.get(i)).getStatus() != Consts.STATUS_WAIT) {
                resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Task in progress");
                return;
            }
        }

        try {
            DBInstance.db.cancelTask(task.getUuid());
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        for (Result status : subTasks) {
            try {
                status.setStatus(Consts.STATUS_CANCELED);
                DBInstance.db.updateSubTaskStatus(status);
            } catch (DBException e) {
                logger.error(e.getMessage(), e);
            }
        }
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
