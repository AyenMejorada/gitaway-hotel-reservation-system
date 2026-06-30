package com.hotel.dao.impl;

import com.hotel.dao.RoomDao;
import com.hotel.db.ConnectionFactory;
import com.hotel.exception.DatabaseException;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.RoomType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomDaoImpl implements RoomDao {

    @Override
    public Room create(Room room) {
        String sql = "INSERT INTO rooms (room_number, room_type, price_per_night, status, capacity, description) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType().name());
            ps.setBigDecimal(3, room.getPricePerNight());
            ps.setString(4, room.getStatus().name());
            ps.setInt(5, room.getCapacity());
            ps.setString(6, room.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    room.setRoomId(keys.getInt(1));
                }
            }
            return room;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create room. Room number may already exist.", e);
        }
    }

    @Override
    public void update(Room room) {
        String sql = "UPDATE rooms SET room_number = ?, room_type = ?, price_per_night = ?, "
                + "status = ?, capacity = ?, description = ? WHERE room_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType().name());
            ps.setBigDecimal(3, room.getPricePerNight());
            ps.setString(4, room.getStatus().name());
            ps.setInt(5, room.getCapacity());
            ps.setString(6, room.getDescription());
            ps.setInt(7, room.getRoomId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Room not found for update (id=" + room.getRoomId() + ").");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update room.", e);
        }
    }

    @Override
    public void softDelete(int roomId) {
        setDeletedFlag(roomId, true);
    }

    @Override
    public void restore(int roomId) {
        setDeletedFlag(roomId, false);
    }

    private void setDeletedFlag(int roomId, boolean deleted) {
        String sql = "UPDATE rooms SET is_deleted = ? WHERE room_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, deleted);
            ps.setInt(2, roomId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Room not found (id=" + roomId + ").");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update room deletion status.", e);
        }
    }

    @Override
    public Optional<Room> findById(int roomId) {
        String sql = "SELECT * FROM rooms WHERE room_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up room.", e);
        }
    }

    @Override
    public List<Room> findAllActive() {
        return findAllByDeletedFlag(false);
    }

    @Override
    public List<Room> findAllArchived() {
        return findAllByDeletedFlag(true);
    }

    private List<Room> findAllByDeletedFlag(boolean deleted) {
        String sql = "SELECT * FROM rooms WHERE is_deleted = ? ORDER BY room_number";
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, deleted);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(mapRow(rs));
                }
            }
            return rooms;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load rooms.", e);
        }
    }

    @Override
    public boolean existsByRoomNumber(String roomNumber, Integer excludingRoomId) {
        String sql = "SELECT COUNT(*) FROM rooms WHERE room_number = ? AND room_id <> ?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            ps.setInt(2, excludingRoomId == null ? -1 : excludingRoomId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check room number uniqueness.", e);
        }
    }

    @Override
    public long countActiveRooms() {
        String sql = "SELECT COUNT(*) FROM rooms WHERE is_deleted = 0";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count rooms.", e);
        }
    }

    @Override
    public List<Room> findAllActiveWithOccupancy() {
        String sql = "SELECT r.*, "
                + "CONCAT(g.first_name, ' ', g.last_name) AS current_guest, "
                + "res.check_in_date, "
                + "res.check_out_date "
                + "FROM rooms r "
                + "LEFT JOIN reservations res ON r.room_id = res.room_id "
                + "    AND res.is_deleted = 0 "
                + "    AND res.status IN ('CONFIRMED', 'CHECKED_IN') "
                + "    AND (CURRENT_DATE >= res.check_in_date AND CURRENT_DATE <= res.check_out_date) "
                + "LEFT JOIN guests g ON res.guest_id = g.guest_id "
                + "WHERE r.is_deleted = 0 "
                + "ORDER BY r.room_number";
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Room r = mapRow(rs);
                r.setCurrentGuest(rs.getString("current_guest"));
                java.sql.Date inDate = rs.getDate("check_in_date");
                if (inDate != null) {
                    r.setCheckInDate(inDate.toLocalDate());
                }
                java.sql.Date outDate = rs.getDate("check_out_date");
                if (outDate != null) {
                    r.setCheckOutDate(outDate.toLocalDate());
                }
                rooms.add(r);
            }
            return rooms;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load rooms with occupancy details.", e);
        }
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setRoomId(rs.getInt("room_id"));
        r.setRoomNumber(rs.getString("room_number"));
        r.setRoomType(RoomType.valueOf(rs.getString("room_type")));
        r.setPricePerNight(rs.getBigDecimal("price_per_night"));
        r.setStatus(RoomStatus.valueOf(rs.getString("status")));
        r.setCapacity(rs.getInt("capacity"));
        r.setDescription(rs.getString("description"));
        r.setDeleted(rs.getBoolean("is_deleted"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null)
            r.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null)
            r.setUpdatedAt(updatedAt.toLocalDateTime());
        return r;
    }
}
