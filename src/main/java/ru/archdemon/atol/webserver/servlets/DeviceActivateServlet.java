package ru.archdemon.atol.webserver.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.archdemon.atol.webserver.db.DBException;
import ru.archdemon.atol.webserver.db.DBInstance;
import ru.archdemon.atol.webserver.db.NotFoundException;
import ru.archdemon.atol.webserver.entities.Device;

public class DeviceActivateServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(DeviceActivateServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String deviceId = req.getParameterValues("deviceID")[0];
        try {
            Device device = DBInstance.db.getDevice(deviceId);
            device.setActive(true);
            DBInstance.db.updateDevice(deviceId, device);
        } catch (NotFoundException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No device found");
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
