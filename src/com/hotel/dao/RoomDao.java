package com.hotel.dao;

import com.hotel.model.Room;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for {@link Room} records.
 * All "list/find" reads exclude soft-deleted rows unless explicitly
 * stated otherwise (see findAllArchived).
 */
public interface RoomDao {

    Room create(Room room);

    void update(Room room);

    /**
     * Soft delete: marks record as inactive (is_deleted flag) instead of removing the row,
     * to preserve referential integrity for historical reservations/billing records.
     */
    void softDelete(int roomId);

    /** Restores a previously soft-deleted room. */
    void restore(int roomId);

    Optional<Room> findById(int roomId);

    List<Room> findAllActive();

    List<Room> findAllArchived();

    boolean existsByRoomNumber(String roomNumber, Integer excludingRoomId);

    long countActiveRooms();

    List<Room> findAllActiveWithOccupancy();
}
