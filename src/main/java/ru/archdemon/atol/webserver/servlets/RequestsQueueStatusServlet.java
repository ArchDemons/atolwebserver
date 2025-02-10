package ru.archdemon.atol.webserver.servlets;

import java.io.IOException;
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
import ru.archdemon.atol.webserver.entities.BlockRecord;
import ru.archdemon.atol.webserver.entities.RequestsQueueStatus;

public class RequestsQueueStatusServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(JsonTaskServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        logger.info(String.format("%s %s", req.getMethod(), req.getRequestURI()));

        String deviceId = req.getParameter("deviceID");

        try {
            RequestsQueueStatus stat = DBInstance.db.getTasksStat(deviceId);
            BlockRecord block = DBInstance.db.getBlockState(deviceId);

            JSONObject response = new JSONObject();
            response.put("number", stat.getNumber());
            response.put("canceled", stat.getCanceled());
            response.put("ready", stat.getReady());
            response.put("isBlocked", block != null);
            response.put("blockReason", block != null ? getBlockReason(block) : "");
            response.put("blockedUUID", block != null ? block.getUuid() : "");

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(response.toJSONString());

            logger.info(String.format("%d %s", resp.getStatus(), response.toJSONString()));
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private String getBlockReason(BlockRecord block) {
        if (block.isConnectionError()) {
            return "connectionError";
        } else if (block.isPaperError()) {
            return "paperError";
        } else if (block.isFnError()) {
            return "fnError";
        }

        return "";
    }
}
