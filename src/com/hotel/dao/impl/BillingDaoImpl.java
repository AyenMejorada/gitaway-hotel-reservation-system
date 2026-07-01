package com.hotel.dao.impl;

import com.hotel.dao.BillingDao;
import com.hotel.db.ConnectionFactory;
import com.hotel.exception.DatabaseException;
import com.hotel.model.Billing;
import com.hotel.model.BillStatus;
import com.hotel.model.RoomType;

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

public class BillingDaoImpl implements BillingDao {

    private static final String BASE_SELECT =
            "SELECT b.*, g.first_name, g.last_name, rm.room_number, r.room_type, r.check_in_date, r.check_out_date, " +
            "DATEDIFF(r.check_out_date, r.check_in_date) AS number_of_nights " +
            "FROM billing b " +
            "JOIN reservations r ON b.reservation_id = r.reservation_id " +
            "JOIN guests g ON r.guest_id = g.guest_id " +
            "LEFT JOIN rooms rm ON r.room_id = rm.room_id ";

    @Override
    public Billing create(Billing billing) {
        String sql = "INSERT INTO billing (reservation_id, billing_date, room_charges, additional_charges, discount, "
                + "tax, total_amount, bill_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, billing.getReservationId());
            ps.setDate(2, Date.valueOf(billing.getBillingDate()));
            ps.setBigDecimal(3, billing.getRoomCharges());
            ps.setBigDecimal(4, billing.getAdditionalCharges());
            ps.setBigDecimal(5, billing.getDiscount());
            ps.setBigDecimal(6, billing.getTax());
            ps.setBigDecimal(7, billing.getTotalAmount());
            ps.setString(8, billing.getBillStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    billing.setBillId(keys.getInt(1));
                }
            }
            return billing;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create billing record.", e);
        }
    }

    @Override
    public void update(Billing billing) {
        String sql = "UPDATE billing SET room_charges = ?, additional_charges = ?, discount = ?, "
                + "tax = ?, total_amount = ?, bill_status = ?, billing_date = ? WHERE bill_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, billing.getRoomCharges());
            ps.setBigDecimal(2, billing.getAdditionalCharges());
            ps.setBigDecimal(3, billing.getDiscount());
            ps.setBigDecimal(4, billing.getTax());
            ps.setBigDecimal(5, billing.getTotalAmount());
            ps.setString(6, billing.getBillStatus().name());
            ps.setDate(7, Date.valueOf(billing.getBillingDate()));
            ps.setInt(8, billing.getBillId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Bill not found for update (id=" + billing.getBillId() + ").");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update billing record.", e);
        }
    }

    @Override
    public void softDelete(int billId) {
        setDeletedFlag(billId, true);
    }

    @Override
    public void restore(int billId) {
        setDeletedFlag(billId, false);
    }

    private void setDeletedFlag(int billId, boolean deleted) {
        String sql = "UPDATE billing SET is_deleted = ? WHERE bill_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, deleted);
            ps.setInt(2, billId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Bill not found (id=" + billId + ").");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update billing deletion status.", e);
        }
    }

    @Override
    public Optional<Billing> findById(int billId) {
        String sql = BASE_SELECT + " WHERE b.bill_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up bill.", e);
        }
    }

    @Override
    public Optional<Billing> findByReservationId(int reservationId) {
        String sql = BASE_SELECT + " WHERE b.reservation_id = ? AND b.is_deleted = 0";
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
            throw new DatabaseException("Failed to look up bill by reservation.", e);
        }
    }

    @Override
    public List<Billing> findAllActive() {
        return findAllByDeletedFlag(false);
    }

    @Override
    public List<Billing> findAllArchived() {
        return findAllByDeletedFlag(true);
    }

    private List<Billing> findAllByDeletedFlag(boolean deleted) {
        String sql = BASE_SELECT + " WHERE b.is_deleted = ? ORDER BY b.created_at DESC";
        List<Billing> list = new ArrayList<>();
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
            throw new DatabaseException("Failed to load billing records.", e);
        }
    }

    private Billing mapRow(ResultSet rs) throws SQLException {
        Billing b = new Billing();
        b.setBillId(rs.getInt("bill_id"));
        b.setReservationId(rs.getInt("reservation_id"));
        Date bDate = rs.getDate("billing_date");
        if (bDate != null) b.setBillingDate(bDate.toLocalDate());
        b.setRoomCharges(rs.getBigDecimal("room_charges"));
        b.setAdditionalCharges(rs.getBigDecimal("additional_charges"));
        b.setDiscount(rs.getBigDecimal("discount"));
        b.setTax(rs.getBigDecimal("tax"));
        b.setTotalAmount(rs.getBigDecimal("total_amount"));
        b.setBillStatus(BillStatus.valueOf(rs.getString("bill_status").toUpperCase().replace(" ", "_")));
        b.setDeleted(rs.getBoolean("is_deleted"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) b.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) b.setUpdatedAt(updatedAt.toLocalDateTime());

        b.setGuestName(rs.getString("first_name") + " " + rs.getString("last_name"));
        b.setRoomNumber(rs.getString("room_number") == null ? "N/A" : rs.getString("room_number"));
        b.setRoomType(RoomType.valueOf(rs.getString("room_type")));
        Date checkIn = rs.getDate("check_in_date");
        if (checkIn != null) b.setCheckInDate(checkIn.toLocalDate());
        Date checkOut = rs.getDate("check_out_date");
        if (checkOut != null) b.setCheckOutDate(checkOut.toLocalDate());
        b.setNumberOfNights(rs.getInt("number_of_nights"));
        return b;
    }
}
