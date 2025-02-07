package ru.archdemon.atol.webserver.servlets;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;
import ru.atol.drivers10.fptr.Fptr;
import ru.archdemon.atol.webserver.Version;

public class ServerInfoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        JSONObject response = new JSONObject();
        response.put("currentDateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        response.put("driverVersion", (new Fptr()).version());
        response.put("os", System.getProperty("os.name"));
        response.put("serverVersion", (new Version()).getVersion());

        resp.getWriter().write(response.toJSONString());
    }
}
