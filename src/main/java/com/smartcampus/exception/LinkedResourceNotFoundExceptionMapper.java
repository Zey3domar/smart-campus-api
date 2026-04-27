package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

/**
 * Part 5.2 - Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity
 *
 * 422 is more semantically accurate than 404 here because the request URL is valid,
 * the JSON payload is well-formed, but the *content* references a resource that doesn't exist.
 * A 404 implies the endpoint itself was not found, which is misleading.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 422);
        body.put("error", "Unprocessable Entity");
        body.put("code", "LINKED_RESOURCE_NOT_FOUND");
        body.put("message", "The referenced " + ex.getResourceType() + " with ID '"
                + ex.getResourceId() + "' does not exist. "
                + "Ensure the resource exists before creating a dependency on it.");
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
