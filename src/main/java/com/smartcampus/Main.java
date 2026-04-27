package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    // The base URI includes /api/v1 so Grizzly serves everything under that path.
    // @ApplicationPath on SmartCampusApplication is redundant when using ResourceConfig
    // directly, so we set the path explicitly here.
    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static HttpServer startServer() {
        final ResourceConfig rc = new ResourceConfig().packages("com.smartcampus");
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void main(String[] args) throws Exception {
        final HttpServer server = startServer();
        LOGGER.info("=======================================================");
        LOGGER.info("Smart Campus API started!");
        LOGGER.info("Discovery : http://localhost:8080/api/v1");
        LOGGER.info("Rooms     : http://localhost:8080/api/v1/rooms");
        LOGGER.info("Sensors   : http://localhost:8080/api/v1/sensors");
        LOGGER.info("Press CTRL+C to stop the server.");
        LOGGER.info("=======================================================");
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        Thread.currentThread().join();
    }
}
