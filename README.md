# Smart Campus Sensor & Room Management API

A JAX-RS RESTful API for managing university campus rooms and IoT sensors, built with Jersey 2 on an embedded Grizzly HTTP server.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Build & Run](#build--run)
- [API Endpoints](#api-endpoints)
- [Sample curl Commands](#sample-curl-commands)
- [Report: Answers to Coursework Questions](#report-answers-to-coursework-questions)

---

## Overview

The Smart Campus API provides a clean, versioned RESTful interface for facilities managers and automated building systems to interact with campus data. It manages three core resources:

- **Rooms** – physical locations on campus (libraries, labs, etc.)
- **Sensors** – IoT devices deployed within rooms (temperature, CO₂, occupancy)
- **Sensor Readings** – historical measurement logs for each sensor

All data is stored in-memory using `ConcurrentHashMap` and `CopyOnWriteArrayList` for thread safety.

---

## Tech Stack

- **Java 11**
- **JAX-RS** (Jakarta RESTful Web Services) via **Jersey 2.39.1**
- **Grizzly HTTP Server** (embedded — no external app server needed)
- **Jackson** (JSON serialisation)
- **Maven** (build tool)

---

## Build & Run

### Prerequisites

- Java 11+
- Maven 3.6+

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/smart-campus-api.git
cd smart-campus-api

# 2. Build the fat JAR
mvn clean package

# 3. Run the server
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```

The server starts at **http://localhost:8080/api/v1**

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1 | Discovery — API metadata & links |
| GET | /api/v1/rooms | List all rooms |
| POST | /api/v1/rooms | Create a new room |
| GET | /api/v1/rooms/{roomId} | Get a specific room |
| DELETE | /api/v1/rooms/{roomId} | Delete a room (only if sensor-free) |
| GET | /api/v1/sensors | List sensors (optional `?type=` filter) |
| POST | /api/v1/sensors | Register a new sensor |
| GET | /api/v1/sensors/{sensorId} | Get a specific sensor |
| GET | /api/v1/sensors/{sensorId}/readings | List all readings for a sensor |
| POST | /api/v1/sensors/{sensorId}/readings | Record a new sensor reading |

---

## Sample curl Commands

### 1. Discovery Endpoint
```bash
curl -X GET http://localhost:8080/api/v1 \
  -H "Accept: application/json"
```

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LAB-202","name":"Physics Lab","capacity":25}'
```

### 3. List all CO2 Sensors (with query filter)
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2" \
  -H "Accept: application/json"
```

### 4. Register a Sensor linked to a Room
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":19.5,"roomId":"LAB-101"}'
```

### 5. Post a Sensor Reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":22.3}'
```

### 6. Attempt to delete a Room with sensors (triggers 409)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 7. Attempt to post a reading to a MAINTENANCE sensor (triggers 403)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":5}'
```

### 8. Register sensor with non-existent room (triggers 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0,"roomId":"FAKE-999"}'
```

---

## Report: Answers to Coursework Questions

---

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request**. This is the per-request (prototype) scope — the JAX-RS specification mandates it as the default. A new object is instantiated, the method is invoked, and the object is discarded.

This has a critical implication for in-memory data storage: **you cannot store shared data as instance fields** of a resource class, because each request gets a fresh instance with no memory of previous requests. If you stored your `HashMap<String, Room>` as a plain instance field on `RoomResource`, each request would see an empty map.

To address this, I implemented a **singleton `DataStore` class** using the Singleton pattern (`DataStore.getInstance()`). All resource instances access the same static data store. To prevent race conditions caused by concurrent requests, the store uses `ConcurrentHashMap` (for rooms and sensors) and `CopyOnWriteArrayList` (for sensor reading lists), which are both thread-safe. This avoids `ConcurrentModificationException` and data corruption when multiple requests read/write simultaneously.

You can also annotate a resource with `@Singleton` to make JAX-RS treat it as a single shared instance, but then you must manage thread safety of its fields manually.

---

### Part 1.2 — HATEOAS and Hypermedia

HATEOAS (Hypermedia As The Engine Of Application State) is the principle that API responses should include **links to related resources and possible next actions** — not just raw data. For example, a GET response for a room should include a link to its sensors, and a POST response should include a link to the newly created resource.

This is considered advanced RESTful design because it makes the API **self-documenting and self-navigating**. A client that receives the discovery response at `GET /api/v1` immediately knows where all resources live, without needing to consult external documentation. If endpoints change, the client can follow links dynamically rather than hardcoding paths.

Compared to static documentation, HATEOAS means: the client only needs to know the entry point URL; it discovers everything else at runtime. This reduces tight coupling between client and server, making it easier to evolve the API (e.g., moving a resource to a new path) without breaking clients.

---

### Part 2.1 — Full Objects vs IDs in Collection Responses

When returning a list of rooms, there are two options:

- **IDs only** (`["LIB-301", "LAB-101"]`): Very small payload, low bandwidth. However, the client must make N additional requests to fetch each room's details — this is the "N+1 problem". For a campus with thousands of rooms, this creates significant latency and server load.

- **Full objects** (this implementation): The entire room data is returned in one response. The client gets everything needed in a single round trip. The trade-off is larger payload size, but for typical room objects this is negligible compared to the latency of multiple round trips.

The best practice is to return full objects for collections, as modern APIs prioritise reducing round trips. If bandwidth becomes a concern, pagination or sparse fieldsets (`?fields=id,name`) can be added later.

---

### Part 2.2 — DELETE Idempotency

In this implementation, **DELETE is partially idempotent** but with an important nuance:

- **First DELETE** on `/api/v1/rooms/LAB-202`: The room exists, has no sensors → it is deleted → `204 No Content`
- **Second DELETE** on the same URL: The room no longer exists → `404 Not Found`

Strictly speaking, idempotency means the **server state** is unchanged by repeated calls. After the first DELETE, the room is gone. After the second DELETE, the room is still gone — the state is identical. In this sense, DELETE **is** idempotent at the state level.

However, the **HTTP response code changes** (204 → 404). Some API designers argue true idempotency means the response should also be consistent (e.g., always returning 204 even when the resource doesn't exist). My implementation follows the more common convention: return 404 on the second call to inform the client the resource was not found, while the underlying state remains unchanged. This is RFC-compliant and considered acceptable practice.

---

### Part 3.1 — `@Consumes` and Format Mismatches

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that the POST endpoint **only accepts `application/json`** request bodies.

If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`:

1. The JAX-RS runtime checks the incoming `Content-Type` header against the declared `@Consumes` value.
2. Finding no matching resource method, it returns **`415 Unsupported Media Type`** automatically, before even invoking the resource method.
3. No custom code is required — this is handled entirely by the framework.

This provides a clean contract: clients know exactly what format is expected, and receive a clear error if they send the wrong one. It also prevents unexpected input from reaching the application layer.

---

### Part 3.2 — Query Parameters vs Path Segments for Filtering

**Query parameters** (`GET /api/v1/sensors?type=CO2`) vs **path segments** (`GET /api/v1/sensors/type/CO2`):

Query parameters are generally superior for filtering because:

1. **Semantic clarity**: A URL path identifies a *resource*. `/sensors/type/CO2` implies that `type/CO2` is a specific, fixed sub-resource — but it's actually a filter over the collection. Query parameters semantically represent *search criteria*, not resource identity.

2. **Optional nature**: Query parameters are naturally optional. `GET /sensors` returns all sensors. `GET /sensors?type=CO2` narrows the result. With path segments you'd need separate routes for filtered and unfiltered access.

3. **Composability**: Multiple filters combine naturally: `?type=CO2&status=ACTIVE`. With path segments this becomes messy and combinatorially explosive.

4. **Caching & REST conventions**: Path-based resources are typically cacheable by URL. Filtering is dynamic and context-dependent — query strings signal to caches and clients that results may vary.

---

### Part 4.1 — Sub-Resource Locator Pattern

The Sub-Resource Locator pattern works by having a **method in a parent resource return a new resource class instance** (not a response). JAX-RS then delegates further path matching and method dispatch to that returned object.

Benefits compared to defining all paths in one class:

1. **Separation of concerns**: `SensorResource` handles sensor CRUD. `SensorReadingResource` handles reading history. Each class has a single, focused responsibility.

2. **Reduced complexity**: A monolithic resource class with paths like `/sensors`, `/sensors/{id}`, `/sensors/{id}/readings`, `/sensors/{id}/readings/{rid}` grows unmanageable. Sub-resources keep each class small and testable.

3. **Contextual state injection**: When `SensorResource` creates the `SensorReadingResource`, it passes the already-validated `Sensor` object to the constructor. The sub-resource starts execution knowing which sensor it's operating on, without re-fetching it.

4. **Reusability**: A sub-resource class could potentially be reused from multiple parent resources if the same nested structure appears in different contexts.

---

### Part 5.2 — HTTP 422 vs 404 for Missing Referenced Resources

When a client sends a POST to `/api/v1/sensors` with `"roomId": "FAKE-999"`:

- The **URL** (`/api/v1/sensors`) is perfectly valid — 404 would be wrong here.
- The **JSON payload** is syntactically valid — 400 Bad Request would also be slightly inaccurate.
- The problem is that the **semantic content** of the payload references a resource that doesn't exist.

**HTTP 422 Unprocessable Entity** is more accurate because:
- It signals that the server understands the content type and the request is well-formed syntactically.
- The failure is at the *semantic* or *business logic* level — the referenced entity (`roomId`) cannot be resolved.
- A 404 says "I couldn't find the endpoint you requested", which is misleading when the endpoint exists and worked fine; only the referenced data is missing.

422 clearly communicates to the client: "Your request arrived, was understood, but cannot be fulfilled because a linked resource is missing."

---

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers is a significant security vulnerability for several reasons:

1. **Technology fingerprinting**: The trace reveals the exact class hierarchy, framework names, and library versions (e.g., `org.glassfish.jersey.server`, `com.fasterxml.jackson`). Attackers use this to look up **known CVEs** for those specific versions.

2. **Internal architecture disclosure**: Package and class names reveal the internal design — e.g., `com.smartcampus.DataStore` tells an attacker that data is stored in-memory, which might suggest other attack vectors (e.g., race conditions, memory exhaustion).

3. **Business logic exposure**: Line numbers and method names in a trace can reveal control flow, making it easier to identify and probe edge cases or input validation gaps.

4. **Attack surface mapping**: Exception types (e.g., `NullPointerException` at `SensorResource.java:47`) tell an attacker exactly which input triggered the error and where in the code it occurred — enabling targeted fuzzing.

The correct approach (implemented in `GlobalExceptionMapper`) is to log the full trace **server-side only** and return a generic `500 Internal Server Error` with no internal details exposed to the client.

---

### Part 5.5 — JAX-RS Filters vs Manual Logging

Using JAX-RS filters for cross-cutting concerns like logging is superior to manually inserting `Logger.info()` calls in every resource method for several reasons:

1. **DRY (Don't Repeat Yourself)**: With 10 endpoints, manual logging means 10+ places to maintain. A filter logs everything in one class, automatically.

2. **Consistency**: A filter guarantees every request is logged with the same format. Manual logging is prone to developers forgetting or using different formats.

3. **Separation of concerns**: Resource methods should contain business logic only. Logging is infrastructure — mixing them violates the Single Responsibility Principle and makes code harder to read and test.

4. **Completeness**: A `ContainerResponseFilter` can log the final HTTP status code after the response is assembled — something that's difficult to replicate by hand inside each method, especially when exceptions produce the response.

5. **Easy toggling**: Filters can be registered/unregistered at application startup or controlled with `@NameBinding` annotations, making it trivial to enable verbose logging in development and disable it in production.
