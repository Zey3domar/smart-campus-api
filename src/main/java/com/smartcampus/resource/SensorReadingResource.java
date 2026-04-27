package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Part 4 - SensorReading Sub-Resource
 * Handles /api/v1/sensors/{sensorId}/readings
 * Instantiated by SensorResource's sub-resource locator.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final Sensor sensor;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(Sensor sensor) {
        this.sensor = sensor;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns all historical readings for this sensor.
     */
    @GET
    public Response getReadings() {
        List<SensorReading> readings = store.getReadingsForSensor(sensor.getId());
        return Response.ok(readings).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Appends a new reading for this sensor.
     * Side effect: updates sensor's currentValue to keep data consistent.
     * Throws SensorUnavailableException if sensor status is "MAINTENANCE".
     */
    @POST
    public Response addReading(SensorReading reading) {
        // State constraint: MAINTENANCE sensors cannot accept new readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensor.getId(), sensor.getStatus());
        }

        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Request body with a 'value' field is required."))
                    .build();
        }

        // Initialise id and timestamp if missing
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(java.util.UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        store.addReading(sensor.getId(), reading);

        // Side effect: update the parent sensor's currentValue for consistency
        sensor.setCurrentValue(reading.getValue());
        store.saveSensor(sensor);

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> err = new HashMap<>();
        err.put("error", message);
        return err;
    }
}
