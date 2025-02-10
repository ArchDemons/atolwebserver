package ru.archdemon.atol.webserver.servlets;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import ru.archdemon.atol.webserver.db.DBException;
import ru.archdemon.atol.webserver.db.DBInstance;
import ru.archdemon.atol.webserver.db.NotFoundException;

public class DeviceDefaultServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(DeviceDefaultServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        JSONObject json;
        String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        try {
            json = (JSONObject) JSONValue.parseWithException(body);
            if (!json.containsKey("id")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Attribute 'id' not found");
                return;
            }
            DBInstance.db.defaultDevice(json.get("id").toString());
        } catch (NotFoundException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No device found");
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (ParseException e) {
            logger.error(e.getMessage());
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

}
