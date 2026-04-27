package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Part 3 - Sensor Operations Resource
 * Manages /api/v1/sensors path.
 * Also acts as a sub-resource locator for /api/v1/sensors/{sensorId}/readings
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    @Context
    private UriInfo uriInfo;

    /**
     * GET /api/v1/sensors
     * Returns all sensors, with optional ?type= filter.
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = store.getSensors().values();
        if (type != null && !type.isBlank()) {
            List<Sensor> filtered = all.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
            return Response.ok(filtered).build();
        }
        return Response.ok(all).build();
    }

    /**
     * POST /api/v1/sensors
     * Registers a new sensor. Validates that the given roomId exists.
     * If roomId is missing or doesn't exist, throws LinkedResourceNotFoundException -> 422.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Request body is required."))
                    .build();
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            throw new LinkedResourceNotFoundException("roomId", "null");
        }

        // Validate that the referenced room actually exists
        Room room = store.getRoomById(sensor.getRoomId())
                .orElseThrow(() -> new LinkedResourceNotFoundException("Room", sensor.getRoomId()));

        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId(sensor.getType().toUpperCase().substring(0, Math.min(4, sensor.getType().length()))
                    + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.saveSensor(sensor);
        room.getSensorIds().add(sensor.getId());

        URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.status(Response.Status.CREATED)
                .location(location)
                .entity(sensor)
                .build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     * Returns a specific sensor by ID.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        return store.getSensorById(sensorId)
                .map(s -> Response.ok(s).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(errorBody("Sensor with ID '" + sensorId + "' not found."))
                        .build());
    }

    /**
     * Sub-resource locator for /api/v1/sensors/{sensorId}/readings
     * Delegates to SensorReadingResource with the validated sensor context.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        // Validate sensor exists before delegating
        Sensor sensor = store.getSensorById(sensorId)
                .orElseThrow(() -> new NotFoundException("Sensor with ID '" + sensorId + "' not found."));
        return new SensorReadingResource(sensor);
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> err = new HashMap<>();
        err.put("error", message);
        return err;
    }
}
