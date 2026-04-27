package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

/**
 * Part 5.1 - Maps RoomNotEmptyException to HTTP 409 Conflict
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 409);
        body.put("error", "Conflict");
        body.put("code", "ROOM_NOT_EMPTY");
        body.put("message", "Cannot delete room '" + ex.getRoomId() + "'. It currently has "
                + ex.getSensorCount() + " sensor(s) assigned. "
                + "Please reassign or remove all sensors before deleting the room.");
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
