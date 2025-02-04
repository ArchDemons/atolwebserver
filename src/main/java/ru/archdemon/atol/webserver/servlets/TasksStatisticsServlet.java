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
import ru.archdemon.atol.webserver.entities.BlockRecord;
import ru.archdemon.atol.webserver.entities.TasksStat;

public class TasksStatisticsServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(JsonTaskServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        logger.info(String.format("%s %s", req.getMethod(), req.getRequestURI()));

        try {
            TasksStat stat = DBInstance.db.getTasksStat();
            BlockRecord block = DBInstance.db.getBlockState();

            JSONObject response = new JSONObject();
            response.put("ready_count", stat.getTasksReadyCount());
            response.put("not_ready_count", stat.getTasksNotReadyCount());
            response.put("canceled_count", stat.getTasksCanceledCount());
            response.put("is_blocked", !block.getUuid().isEmpty());
            response.put("block_request_uuid", block.getUuid());

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(response.toJSONString());

            logger.info(String.format("%d %s", resp.getStatus(), response.toJSONString()));
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(500, e.getMessage());
        }
    }
}
