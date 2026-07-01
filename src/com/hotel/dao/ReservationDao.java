package com.hotel.dao;

import com.hotel.model.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationDao {

    Reservation create(Reservation reservation);

    void update(Reservation reservation);

    /**
     * Soft delete: marks record as inactive (is_deleted flag) instead of removing the row,
     * to preserve referential integrity for historical reservations/billing records.
     */
    void softDelete(int reservationId);

    void restore(int reservationId);

    Optional<Reservation> findById(int reservationId);

    /** Returns active (non-deleted) reservations joined with guest/room display info. */
    List<Reservation> findAllActive();

    List<Reservation> findAllArchived();

    /** Returns active reservations belonging to a specific guest (for the customer portal). */
    List<Reservation> findActiveByGuestId(int guestId);

    /** Checks whether a room is already booked (non-cancelled) for an overlapping date range. */
    boolean hasOverlappingReservation(int roomId, LocalDate checkIn, LocalDate checkOut, Integer excludingReservationId);

    List<Reservation> findConfirmedReservationsByRoomId(int roomId);

    long countActiveReservations();

    BigDecimal sumRevenue();
}
