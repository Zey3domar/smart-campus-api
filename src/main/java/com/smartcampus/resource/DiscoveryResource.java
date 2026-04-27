package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Part 1 - Discovery Endpoint
 * GET /api/v1 returns API metadata including versioning, contact, and resource links.
 * This implements HATEOAS principles by providing hypermedia links to all resources.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> discovery = new HashMap<>();
        discovery.put("name", "Smart Campus Sensor & Room Management API");
        discovery.put("version", "1.0.0");
        discovery.put("description", "RESTful API for managing campus rooms and IoT sensors");
        discovery.put("contact", Map.of(
                "name", "Smart Campus Admin",
                "email", "admin@smartcampus.ac.uk"
        ));

        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1");
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");

        discovery.put("links", links);

        return Response.ok(discovery).build();
    }
}
