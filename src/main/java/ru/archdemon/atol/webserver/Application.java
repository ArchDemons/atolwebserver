package ru.archdemon.atol.webserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import ru.archdemon.atol.webserver.db.DBInstance;
import ru.archdemon.atol.webserver.servlets.DeviceActivateServlet;
import ru.archdemon.atol.webserver.servlets.DeviceDeactivateServlet;
import ru.archdemon.atol.webserver.servlets.DeviceDefaultServlet;
import ru.archdemon.atol.webserver.servlets.DeviceServlet;
import ru.archdemon.atol.webserver.servlets.JsonTaskServlet;
import ru.archdemon.atol.webserver.servlets.OperationServlet;
import ru.archdemon.atol.webserver.servlets.RequestsQueueStatusServlet;
import ru.archdemon.atol.webserver.servlets.ServerInfoServlet;
import ru.archdemon.atol.webserver.servlets.SettingServlet;
import ru.archdemon.atol.webserver.servlets.UtilServlet;
import ru.archdemon.atol.webserver.workers.DriverWorker;

/**
 *
 * @author dalarin
 */
public class Application {

    private static final String WEBROOT_INDEX = "/webapp/";
    private static final Logger logger = LogManager.getLogger(Application.class);

    private DriverWorker driverWorker;
    private Server server;

    private static URI getWebRootResourceUri() throws FileNotFoundException, URISyntaxException {
        URL indexUri = Main.class.getResource(WEBROOT_INDEX);
        if (indexUri == null) {
            throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
        }
        return indexUri.toURI();
    }

    public void stop(String[] args) throws Exception {
        logger.info("Завершение работы...");

        if (server != null && server.isRunning()) {
            logger.info("Завершение сервера...");
            for (Handler handler : server.getHandlers()) {
                handler.stop();
            }
            server.stop();
            server.getThreadPool().join();
            logger.info("OK");
        }

        if (driverWorker != null && driverWorker.isAlive()) {
            logger.info("Завершение потока работы с ККТ...");
            driverWorker.interrupt();
            driverWorker.join();
            logger.info("OK");
        }
    }

    public void start(String[] args) throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop(null);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }));
        logger.info(String.format("Запуск сервера ККТ v. %s...", (new Version()).getVersion()));

        logger.info("Инициализация БД...");
        DBInstance.db.init();
        logger.info("OK");

        logger.info("Запуск сервера");
        server = new Server();

        String appConfigPath = Thread.currentThread().getContextClassLoader().getResource("application.properties").getPath();
        Properties properties = new Properties();
        properties.load(new FileInputStream(appConfigPath));

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(Integer.parseInt(properties.getProperty("web.port")));
        server.addConnector(connector);

        Configuration.ClassList.setServerDefault(server)
                .addBefore(JettyWebXmlConfiguration.class.getName(), AnnotationConfiguration.class.getName());

        URI baseUri = getWebRootResourceUri();

        FilterHolder filterHolder = new FilterHolder(CrossOriginFilter.class);
        filterHolder.setInitParameter("allowedOrigins", "*");
        filterHolder.setInitParameter("allowedMethods", "GET,POST,PUT,DELETE,HEAD,OPTIONS");

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setContextPath("/");
        servletContextHandler.setResourceBase(baseUri.toASCIIString());

        servletContextHandler.addServlet(UtilServlet.class, "/api/v2/utils/*");
        servletContextHandler.addServlet(OperationServlet.class, "/api/v2/operations/*");
        servletContextHandler.addServlet(ServerInfoServlet.class, "/api/v2/serverInfo");
        servletContextHandler.addServlet(DeviceServlet.class, "/api/v2/devices");
        servletContextHandler.addServlet(DeviceServlet.class, "/api/v2/devices/*");
        servletContextHandler.addServlet(JsonTaskServlet.class, "/api/v2/requests");
        servletContextHandler.addServlet(JsonTaskServlet.class, "/api/v2/requests/*");
        servletContextHandler.addServlet(RequestsQueueStatusServlet.class, "/api/v2/getRequestsQueueStatus");
        servletContextHandler.addServlet(SettingServlet.class, "/api/v2/settings");
        servletContextHandler.addServlet(DeviceActivateServlet.class, "/api/v2/activateDevice");
        servletContextHandler.addServlet(DeviceDeactivateServlet.class, "/api/v2/deactivateDevice");
        servletContextHandler.addServlet(DeviceDefaultServlet.class, "/api/v2/setDefaultDevice");
        servletContextHandler.addFilter(filterHolder, "/*", null);

        ServletHolder holderDefault = new ServletHolder("default", DefaultServlet.class);
        holderDefault.setInitParameter("resourceBase", baseUri.toASCIIString());
        holderDefault.setInitParameter("dirAllowed", "false");
        servletContextHandler.addServlet(holderDefault, "/");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{servletContextHandler});
        server.setHandler(handlers);

        server.start();
        logger.info("OK");

        logger.info("Запуск потока работы с ККТ...");
        driverWorker = new DriverWorker();
        driverWorker.start();
        logger.info("OK");

        server.join();
    }

}
