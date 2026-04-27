package com.smartcampus;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton in-memory data store.
 * Uses ConcurrentHashMap and CopyOnWriteArrayList to handle concurrent access safely,
 * since JAX-RS resource classes are instantiated per-request but share this static state.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    // Thread-safe maps for all entities
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    private DataStore() {
        seedData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    private void seedData() {
        // Seed some initial rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Science Lab", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        // Seed some sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 420.0, "LIB-301");
        Sensor s3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 0.0, "LAB-101");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        r1.getSensorIds().add(s1.getId());
        r1.getSensorIds().add(s2.getId());
        r2.getSensorIds().add(s3.getId());

        // Seed some readings
        sensorReadings.put("TEMP-001", new CopyOnWriteArrayList<>(List.of(new SensorReading(21.5))));
        sensorReadings.put("CO2-001", new CopyOnWriteArrayList<>(List.of(new SensorReading(420.0))));
    }

    // ---- Room operations ----

    public Map<String, Room> getRooms() { return rooms; }

    public Optional<Room> getRoomById(String id) {
        return Optional.ofNullable(rooms.get(id));
    }

    public Room saveRoom(Room room) {
        rooms.put(room.getId(), room);
        return room;
    }

    public boolean deleteRoom(String id) {
        return rooms.remove(id) != null;
    }

    // ---- Sensor operations ----

    public Map<String, Sensor> getSensors() { return sensors; }

    public Optional<Sensor> getSensorById(String id) {
        return Optional.ofNullable(sensors.get(id));
    }

    public Sensor saveSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        return sensor;
    }

    public boolean deleteSensor(String id) {
        return sensors.remove(id) != null;
    }

    // ---- SensorReading operations ----

    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return sensorReadings.getOrDefault(sensorId, new CopyOnWriteArrayList<>());
    }

    public SensorReading addReading(String sensorId, SensorReading reading) {
        sensorReadings.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>()).add(reading);
        return reading;
    }
}
