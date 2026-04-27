package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application configuration class.
 * Sets the versioned base path for all API endpoints.
 *
 * Lifecycle Note: By default, JAX-RS creates a new instance of each resource class
 * per request (request-scoped). This means resource classes are NOT singletons.
 * To share state (in-memory data stores), we use static fields or a separate
 * singleton DataStore class accessed by all resource instances.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // Packages are scanned automatically via ResourceConfig in Main.java
}
