package ru.archdemon.atol.webserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import javax.servlet.ServletException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.json.simple.JSONObject;
import ru.archdemon.atol.webserver.db.DBInstance;
import ru.archdemon.atol.webserver.servlets.*;
import ru.archdemon.atol.webserver.workers.DriverWorker;

public class Main {

    private static final String WEBROOT_INDEX = "/webapp/";
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static DriverWorker driverWorker;
    private static Server server;

    public static class JspStarter extends AbstractLifeCycle
            implements ServletContextHandler.ServletContainerInitializerCaller {

        JettyJasperInitializer sci;
        ServletContextHandler context;

        public JspStarter(ServletContextHandler context) throws IOException {
            this.sci = new JettyJasperInitializer();
            this.context = context;
            this.context.setAttribute(JarScanner.class.getName(), new StandardJarScanner());
        }

        @Override
        protected void doStart() throws ServletException, Exception {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.context.getClassLoader());
            try {
                this.sci.onStartup(null, this.context.getServletContext());
                super.doStart();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            start(args);
            stop(args);
        } else if ("start".equals(args[0])) {
            start(args);
        } else if ("stop".equals(args[0])) {
            stop(args);
        }
    }

    public static void stop(String[] args) throws Exception {
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

    public static void start(String[] args) throws Exception {
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

        Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(server);
        classlist.addBefore(JettyWebXmlConfiguration.class.getName(), AnnotationConfiguration.class.getName());

        URI baseUri = getWebRootResourceUri();

        FilterHolder filterHolder = new FilterHolder(CrossOriginFilter.class);
        filterHolder.setInitParameter("allowedOrigins", "*");
        filterHolder.setInitParameter("allowedMethods", "GET,POST,PUT,DELETE,HEAD,OPTIONS");

        ServletContextHandler servletContextHandler = new ServletContextHandler(1);
        servletContextHandler.setContextPath("/");
        servletContextHandler.setResourceBase(baseUri.toASCIIString());

        enableEmbeddedJspSupport(servletContextHandler);

        servletContextHandler.addServlet(UtilServlet.class, "/api/v2/utils/*");
        servletContextHandler.addServlet(UtilServlet.class, "/api/v2/operations/*");
        servletContextHandler.addServlet(ServerInfoServlet.class, "/api/v2/serverInfo");
        servletContextHandler.addServlet(DeviceServlet.class, "/api/v2/devices");
        servletContextHandler.addServlet(DeviceServlet.class, "/api/v2/devices/*");
        servletContextHandler.addServlet(JsonTaskServlet.class, "/api/v2/requests");
        servletContextHandler.addServlet(JsonTaskServlet.class, "/api/v2/requests/*");
        servletContextHandler.addServlet(RequestsQueueStatusServlet.class, "/api/v2/getRequestsQueueStatus");
        servletContextHandler.addServlet(SettingServlet.class, "/api/v2/settings");
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

    private static void enableEmbeddedJspSupport(ServletContextHandler servletContextHandler) throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(), "atol-web-server-tmp");

        if (!scratchDir.exists() && !scratchDir.mkdirs()) {
            throw new IOException("Unable to create scratch directory: " + scratchDir);
        }

        servletContextHandler.setAttribute("javax.servlet.context.tempdir", scratchDir);

        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], Main.class.getClassLoader());
        servletContextHandler.setClassLoader(jspClassLoader);

        servletContextHandler.addBean(new JspStarter(servletContextHandler));
    }

    private static URI getWebRootResourceUri() throws FileNotFoundException, URISyntaxException {
        URL indexUri = Main.class.getResource(WEBROOT_INDEX);
        if (indexUri == null) {
            throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
        }
        return indexUri.toURI();
    }
}
