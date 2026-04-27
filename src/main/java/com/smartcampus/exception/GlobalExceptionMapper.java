package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Part 5.4 - Global Safety Net: catches any unhandled Throwable and returns HTTP 500.
 *
 * Security rationale: Exposing Java stack traces to clients leaks:
 * - Internal class/package names (useful for targeted exploits)
 * - Library versions (known CVEs can be targeted)
 * - Business logic flow (attack surface mapping)
 * - Server configuration details
 * Instead we log the full trace server-side and return a generic error.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        // Full stack trace is logged server-side ONLY — never sent to the client
        LOGGER.severe("Unhandled exception [" + ex.getClass().getName() + "]: " + ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact the system administrator.");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
