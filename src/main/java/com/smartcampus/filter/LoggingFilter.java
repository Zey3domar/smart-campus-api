package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5 - API Logging Filter
 * Implements both ContainerRequestFilter and ContainerResponseFilter
 * to log every incoming request and outgoing response without polluting resource classes.
 *
 * Cross-cutting concerns (logging, auth, CORS) belong in filters — not in resource methods —
 * because they apply universally. JAX-RS filters provide a single, clean interception point.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();
        LOGGER.info(String.format("[REQUEST]  %s %s", method, uri));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        int status = responseContext.getStatus();
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getPath();
        LOGGER.info(String.format("[RESPONSE] %s %s -> HTTP %d", method, uri, status));
    }
}
