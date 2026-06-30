package com.hotel.service;

import com.hotel.dao.RoomDao;
import com.hotel.dao.impl.RoomDaoImpl;
import com.hotel.exception.RecordNotFoundException;
import com.hotel.exception.ValidationException;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.RoomType;
import com.hotel.util.Validator;

import java.math.BigDecimal;
import java.util.List;

/**
 * Business logic and validation for {@link Room} entities, sitting between
 * the UI layer and {@link RoomDao}.
 */
public class RoomService {

    private final RoomDao roomDao;

    public RoomService() {
        this.roomDao = new RoomDaoImpl();
    }

    public RoomService(RoomDao roomDao) {
        this.roomDao = roomDao;
    }

    public Room addRoom(String roomNumber, RoomType type, BigDecimal pricePerNight,
                         RoomStatus status, int capacity, String description) {
        validateRoomFields(roomNumber, type, pricePerNight, status, capacity);
        if (roomDao.existsByRoomNumber(roomNumber.trim(), null)) {
            throw new ValidationException("Room number '" + roomNumber + "' already exists.");
        }
        Room room = new Room(roomNumber.trim(), type, pricePerNight, status, capacity,
                description == null ? "" : description.trim());
        return roomDao.create(room);
    }

    public void updateRoom(int roomId, String roomNumber, RoomType type, BigDecimal pricePerNight,
                            RoomStatus status, int capacity, String description) {
        validateRoomFields(roomNumber, type, pricePerNight, status, capacity);
        Room existing = getRoomOrThrow(roomId);
        if (roomDao.existsByRoomNumber(roomNumber.trim(), roomId)) {
            throw new ValidationException("Room number '" + roomNumber + "' already exists.");
        }
        existing.setRoomNumber(roomNumber.trim());
        existing.setRoomType(type);
        existing.setPricePerNight(pricePerNight);
        existing.setStatus(status);
        existing.setCapacity(capacity);
        existing.setDescription(description == null ? "" : description.trim());
        roomDao.update(existing);
    }

    public void softDeleteRoom(int roomId) {
        getRoomOrThrow(roomId);
        roomDao.softDelete(roomId);
    }

    public void restoreRoom(int roomId) {
        roomDao.restore(roomId);
    }

    public Room getRoomOrThrow(int roomId) {
        return roomDao.findById(roomId)
                .orElseThrow(() -> new RecordNotFoundException("Room with id " + roomId + " was not found."));
    }

    public List<Room> getAllActiveRooms() {
        return roomDao.findAllActive();
    }

    public List<Room> getAllArchivedRooms() {
        return roomDao.findAllArchived();
    }

    public long countActiveRooms() {
        return roomDao.countActiveRooms();
    }

    private void validateRoomFields(String roomNumber, RoomType type, BigDecimal pricePerNight,
                                     RoomStatus status, int capacity) {
        Validator.requireNonBlank(roomNumber, "Room number");
        Validator.requireMaxLength(roomNumber, 10, "Room number");
        Validator.requireNonNull(type, "Room type");
        Validator.requireNonNull(status, "Room status");
        Validator.requirePositive(pricePerNight, "Price per night");
        Validator.requirePositive(capacity, "Capacity");
    }
}
