package com.hotel.dao.impl;

import com.hotel.dao.ReservationDao;
import com.hotel.db.ConnectionFactory;
import com.hotel.exception.DatabaseException;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.model.RoomType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationDaoImpl implements ReservationDao {

    // Joined select used for every read so the UI always has guest/room display data available.
    private static final String BASE_SELECT =
            "SELECT r.*, g.first_name, g.last_name, rm.room_number, rm.price_per_night " +
            "FROM reservations r " +
            "JOIN guests g ON r.guest_id = g.guest_id " +
            "LEFT JOIN rooms rm ON r.room_id = rm.room_id ";

    @Override
    public Reservation create(Reservation reservation) {
        String sql = "INSERT INTO reservations (guest_id, room_type, room_id, check_in_date, check_out_date, "
                + "num_guests, status, total_amount, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, reservation.getGuestId());
            ps.setString(2, reservation.getRoomType().name());
            if (reservation.getRoomId() > 0) {
                ps.setInt(3, reservation.getRoomId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.setDate(4, Date.valueOf(reservation.getCheckInDate()));
            ps.setDate(5, Date.valueOf(reservation.getCheckOutDate()));
            ps.setInt(6, reservation.getNumGuests());
            ps.setString(7, reservation.getStatus().name());
            ps.setBigDecimal(8, reservation.getTotalAmount());
            ps.setString(9, reservation.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    reservation.setReservationId(keys.getInt(1));
                }
            }
            return reservation;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create reservation.", e);
        }
    }

    @Override
    public void update(Reservation reservation) {
        String sql = "UPDATE reservations SET guest_id = ?, room_type = ?, room_id = ?, check_in_date = ?, "
                + "check_out_date = ?, num_guests = ?, status = ?, total_amount = ?, notes = ? "
                + "WHERE reservation_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservation.getGuestId());
            ps.setString(2, reservation.getRoomType().name());
            if (reservation.getRoomId() > 0) {
                ps.setInt(3, reservation.getRoomId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.setDate(4, Date.valueOf(reservation.getCheckInDate()));
            ps.setDate(5, Date.valueOf(reservation.getCheckOutDate()));
            ps.setInt(6, reservation.getNumGuests());
            ps.setString(7, reservation.getStatus().name());
            ps.setBigDecimal(8, reservation.getTotalAmount());
            ps.setString(9, reservation.getNotes());
            ps.setInt(10, reservation.getReservationId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Reservation not found for update (id=" + reservation.getReservationId() + ").");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update reservation.", e);
        }
    }

    @Override
    public void softDelete(int reservationId) {
        setDeletedFlag(reservationId, true);
    }

    @Override
    public void restore(int reservationId) {
        setDeletedFlag(reservationId, false);
    }

    private void setDeletedFlag(int reservationId, boolean deleted) {
        String sql = "UPDATE reservations SET is_deleted = ? WHERE reservation_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, deleted);
            ps.setInt(2, reservationId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Reservation not found (id=" + reservationId + ").");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update reservation deletion status.", e);
        }
    }

    @Override
    public Optional<Reservation> findById(int reservationId) {
        String sql = BASE_SELECT + " WHERE r.reservation_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up reservation.", e);
        }
    }

    @Override
    public List<Reservation> findAllActive() {
        return findAllByDeletedFlag(false);
    }

    @Override
    public List<Reservation> findAllArchived() {
        return findAllByDeletedFlag(true);
    }

    private List<Reservation> findAllByDeletedFlag(boolean deleted) {
        String sql = BASE_SELECT + " WHERE r.is_deleted = ? ORDER BY r.check_in_date DESC";
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, deleted);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load reservations.", e);
        }
    }

    @Override
    public List<Reservation> findActiveByGuestId(int guestId) {
        String sql = BASE_SELECT + " WHERE r.is_deleted = 0 AND r.guest_id = ? ORDER BY r.check_in_date DESC";
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load your reservations.", e);
        }
    }

    @Override
    public boolean hasOverlappingReservation(int roomId, java.time.LocalDate checkIn, java.time.LocalDate checkOut,
                                              Integer excludingReservationId) {
        // Two date ranges [a,b) and [c,d) overlap when a < d AND c < b.
        String sql = "SELECT COUNT(*) FROM reservations WHERE room_id = ? AND is_deleted = 0 "
                + "AND status IN ('CONFIRMED', 'CHECKED_IN') "
                + "AND check_in_date < ? AND ? < check_out_date "
                + "AND reservation_id <> ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            ps.setInt(4, excludingReservationId == null ? -1 : excludingReservationId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check room availability.", e);
        }
    }

    @Override
    public List<Reservation> findConfirmedReservationsByRoomId(int roomId) {
        String sql = BASE_SELECT + " WHERE r.is_deleted = 0 AND r.room_id = ? AND r.status IN ('CONFIRMED', 'CHECKED_IN') ORDER BY r.check_in_date ASC";
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load confirmed reservations for room ID: " + roomId, e);
        }
    }

    @Override
    public long countActiveReservations() {
        String sql = "SELECT COUNT(*) FROM reservations WHERE is_deleted = 0";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
              ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count reservations.", e);
        }
    }

    @Override
    public java.math.BigDecimal sumRevenue() {
        // Revenue = total of all active reservations that are not cancelled/pending.
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM reservations "
                + "WHERE is_deleted = 0 AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            BigDecimal total = rs.getBigDecimal(1);
            return total == null ? BigDecimal.ZERO : total;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to calculate revenue.", e);
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setReservationId(rs.getInt("reservation_id"));
        r.setGuestId(rs.getInt("guest_id"));
        r.setRoomId(rs.getInt("room_id"));
        r.setCheckInDate(rs.getDate("check_in_date").toLocalDate());
        r.setCheckOutDate(rs.getDate("check_out_date").toLocalDate());
        r.setNumGuests(rs.getInt("num_guests"));
        r.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        r.setTotalAmount(rs.getBigDecimal("total_amount"));
        r.setNotes(rs.getString("notes"));
        r.setDeleted(rs.getBoolean("is_deleted"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) r.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) r.setUpdatedAt(updatedAt.toLocalDateTime());

        // Joined display fields
        r.setGuestName(rs.getString("first_name") + " " + rs.getString("last_name"));
        r.setRoomNumber(rs.getString("room_number"));
        r.setRoomType(RoomType.valueOf(rs.getString("room_type")));
        r.setPricePerNight(rs.getBigDecimal("price_per_night"));
        return r;
    }
}
