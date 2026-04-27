package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Part 2 - Room Management Resource
 * Manages /api/v1/rooms path.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    @Context
    private UriInfo uriInfo;

    /**
     * GET /api/v1/rooms
     * Returns a list of all rooms with full objects.
     */
    @GET
    public Response getAllRooms() {
        Collection<Room> rooms = store.getRooms().values();
        return Response.ok(rooms).build();
    }

    /**
     * POST /api/v1/rooms
     * Creates a new room. Generates an ID if not provided.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Request body is required."))
                    .build();
        }
        if (room.getName() == null || room.getName().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Room name is required."))
                    .build();
        }
        if (room.getId() == null || room.getId().isBlank()) {
            room.setId("ROOM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (store.getRoomById(room.getId()).isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("A room with ID '" + room.getId() + "' already exists."))
                    .build();
        }
        Room saved = store.saveRoom(room);
        URI location = uriInfo.getAbsolutePathBuilder().path(saved.getId()).build();
        return Response.status(Response.Status.CREATED)
                .location(location)
                .entity(saved)
                .build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Returns detailed metadata for a specific room.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        return store.getRoomById(roomId)
                .map(r -> Response.ok(r).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(errorBody("Room with ID '" + roomId + "' not found."))
                        .build());
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     * Deletes a room only if it has no sensors assigned.
     * Business Logic: throws RoomNotEmptyException -> mapped to 409 Conflict.
     *
     * Idempotency: First DELETE removes the room (204). Subsequent DELETEs return 404.
     * This is technically NOT fully idempotent in terms of response code,
     * but the server state change is idempotent (room remains gone).
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoomById(roomId)
                .orElseThrow(() -> new NotFoundException("Room with ID '" + roomId + "' not found."));

        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }

        store.deleteRoom(roomId);
        return Response.noContent().build();
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> err = new HashMap<>();
        err.put("error", message);
        return err;
    }
}
