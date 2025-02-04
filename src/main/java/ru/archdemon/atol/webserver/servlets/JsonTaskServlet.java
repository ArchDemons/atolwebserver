package ru.archdemon.atol.webserver.servlets;

import java.io.IOException;
import java.util.List;
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
import ru.archdemon.atol.webserver.Consts;
import ru.archdemon.atol.webserver.Utils;
import ru.archdemon.atol.webserver.db.DBException;
import ru.archdemon.atol.webserver.db.DBInstance;
import ru.archdemon.atol.webserver.db.NotUniqueKeyException;
import ru.archdemon.atol.webserver.entities.SubtaskStatus;
import ru.archdemon.atol.webserver.entities.Task;

public class JsonTaskServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(JsonTaskServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        List<SubtaskStatus> subTasks;

        try {
            String uuid = req.getPathInfo().split("/")[1];
            subTasks = DBInstance.db.getTaskStatus(uuid);
        } catch (NotUniqueKeyException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(409, e.getMessage());
            return;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(500, e.getMessage());
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(404, "No UUID");

            return;
        }
        if (subTasks == null) {
            resp.sendError(404);

            return;
        }
        logger.info(String.format("%s %s", req.getMethod(), req.getRequestURI()));

        JSONArray results = new JSONArray();
        for (SubtaskStatus s : subTasks) {
            JSONObject o = new JSONObject();
            o.put("status", Utils.getStatusString(s.getStatus()));
            o.put("errorCode", s.getErrorCode());
            o.put("errorDescription", s.getErrorDescription());

            JSONParser parser = new JSONParser();
            JSONObject subTaskResult = null;
            try {
                subTaskResult = (JSONObject) parser.parse(s.getResultData());
            } catch (ParseException parseException) {
            }

            o.put("result", subTaskResult);
            results.add(o);
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

        resp.sendError(405);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uuid;
        Task task;
        List<SubtaskStatus> subTasks;

        try {
            uuid = req.getPathInfo().split("/")[1];
            subTasks = DBInstance.db.getTaskStatus(uuid);
            task = DBInstance.db.getTask(uuid);
        } catch (NotUniqueKeyException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(409, e.getMessage());
            return;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(500, e.getMessage());
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(404, "No UUID");
            return;
        }

        if (subTasks == null || task == null) {
            resp.sendError(404);

            return;
        }

        logger.info(String.format("%s %s", req.getMethod(), req.getRequestURI()));

        if (task.isReady()) {
            resp.sendError(405, "Task done or canceled");
            return;
        }

        for (int i = 0; i < subTasks.size(); i++) {
            if (((SubtaskStatus) subTasks.get(i)).getStatus() != 0) {
                resp.sendError(405, "Task in progress");
                return;
            }
        }

        try {
            DBInstance.db.cancelTask(task.getUuid());
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        SubtaskStatus status = new SubtaskStatus();
        status.setStatus(Consts.STATUS_CANCELED);

        for (int i = 0; i < subTasks.size(); i++) {
            try {
                DBInstance.db.updateSubTaskStatus(uuid, i, status);
            } catch (DBException e) {
                logger.error(e.getMessage(), e);
            }
        }
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
