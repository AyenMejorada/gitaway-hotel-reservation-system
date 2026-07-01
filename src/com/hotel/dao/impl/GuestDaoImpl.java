package com.hotel.dao.impl;

import com.hotel.dao.GuestDao;
import com.hotel.db.ConnectionFactory;
import com.hotel.exception.DatabaseException;
import com.hotel.model.Guest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GuestDaoImpl implements GuestDao {

    @Override
    public Guest create(Guest guest) {
        String sql = "INSERT INTO guests (user_id, first_name, last_name, email, phone, address, id_number) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (guest.getUserId() != null) {
                ps.setInt(1, guest.getUserId());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, guest.getFirstName());
            ps.setString(3, guest.getLastName());
            ps.setString(4, guest.getEmail());
            ps.setString(5, guest.getPhone());
            ps.setString(6, guest.getAddress());
            ps.setString(7, guest.getIdNumber());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    guest.setGuestId(keys.getInt(1));
                }
            }
            return guest;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create guest record.", e);
        }
    }

    @Override
    public void update(Guest guest) {
        String sql = "UPDATE guests SET first_name = ?, last_name = ?, email = ?, phone = ?, "
                + "address = ?, id_number = ? WHERE guest_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, guest.getFirstName());
            ps.setString(2, guest.getLastName());
            ps.setString(3, guest.getEmail());
            ps.setString(4, guest.getPhone());
            ps.setString(5, guest.getAddress());
            ps.setString(6, guest.getIdNumber());
            ps.setInt(7, guest.getGuestId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Guest not found for update (id=" + guest.getGuestId() + ").");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update guest record.", e);
        }
    }

    @Override
    public void softDelete(int guestId) {
        setDeletedFlag(guestId, true);
    }

    @Override
    public void restore(int guestId) {
        setDeletedFlag(guestId, false);
    }

    @Override
    public void deletePermanently(int guestId) {
        String sql = "DELETE FROM guests WHERE guest_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guestId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Guest not found (id=" + guestId + ").");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to permanently delete guest.", e);
        }
    }

    private void setDeletedFlag(int guestId, boolean deleted) {
        String sql = "UPDATE guests SET is_deleted = ? WHERE guest_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, deleted);
            ps.setInt(2, guestId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Guest not found (id=" + guestId + ").");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update guest deletion status.", e);
        }
    }

    @Override
    public Optional<Guest> findById(int guestId) {
        String sql = "SELECT * FROM guests WHERE guest_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up guest.", e);
        }
    }

    @Override
    public Optional<Guest> findByUserId(int userId) {
        String sql = "SELECT * FROM guests WHERE user_id = ? AND is_deleted = 0";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up guest by user id.", e);
        }
    }

    @Override
    public List<Guest> findAllActive() {
        return findAllByDeletedFlag(false);
    }

    @Override
    public List<Guest> findAllArchived() {
        return findAllByDeletedFlag(true);
    }

    private List<Guest> findAllByDeletedFlag(boolean deleted) {
        String sql = "SELECT * FROM guests WHERE is_deleted = ? ORDER BY last_name, first_name";
        List<Guest> guests = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, deleted);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    guests.add(mapRow(rs));
                }
            }
            return guests;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load guests.", e);
        }
    }

    @Override
    public long countActiveGuests() {
        String sql = "SELECT COUNT(*) FROM guests WHERE is_deleted = 0";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count guests.", e);
        }
    }

    private Guest mapRow(ResultSet rs) throws SQLException {
        Guest g = new Guest();
        g.setGuestId(rs.getInt("guest_id"));
        int userId = rs.getInt("user_id");
        g.setUserId(rs.wasNull() ? null : userId);
        g.setFirstName(rs.getString("first_name"));
        g.setLastName(rs.getString("last_name"));
        g.setEmail(rs.getString("email"));
        g.setPhone(rs.getString("phone"));
        g.setAddress(rs.getString("address"));
        g.setIdNumber(rs.getString("id_number"));
        g.setDeleted(rs.getBoolean("is_deleted"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) g.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) g.setUpdatedAt(updatedAt.toLocalDateTime());
        return g;
    }
}
