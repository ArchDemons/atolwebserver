package ru.archdemon.atol.webserver.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ru.atol.drivers10.fptr.Fptr;
import ru.archdemon.atol.webserver.Version;

public class DevicesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("serverVersion", (new Version()).getVersion());
        req.setAttribute("driverVersion", (new Fptr()).version());

        req.getRequestDispatcher("about.jsp").forward(req, resp);
    }
}
