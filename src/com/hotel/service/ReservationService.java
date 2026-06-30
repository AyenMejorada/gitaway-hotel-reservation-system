package com.hotel.service;

import com.hotel.dao.ReservationDao;
import com.hotel.dao.RoomDao;
import com.hotel.dao.impl.ReservationDaoImpl;
import com.hotel.dao.impl.RoomDaoImpl;
import com.hotel.exception.RecordNotFoundException;
import com.hotel.exception.ValidationException;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.model.Room;
import com.hotel.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Business logic and validation for {@link Reservation} entities.
 * Responsible for: date-range validation, double-booking prevention,
 * automatic total-amount calculation from room price x nights, and
 * keeping {@link Room} status reasonably in sync with reservation status.
 */
public class ReservationService {

    private final ReservationDao reservationDao;
    private final RoomDao roomDao;

    public ReservationService() {
        this.reservationDao = new ReservationDaoImpl();
        this.roomDao = new RoomDaoImpl();
    }

    public ReservationService(ReservationDao reservationDao, RoomDao roomDao) {
        this.reservationDao = reservationDao;
        this.roomDao = roomDao;
    }

    public Reservation addReservation(int guestId, int roomId, LocalDate checkIn, LocalDate checkOut,
                                       int numGuests, ReservationStatus status, String notes) {
        validateCoreFields(roomId, checkIn, checkOut, numGuests, status);

        Room room = roomDao.findById(roomId)
                .orElseThrow(() -> new RecordNotFoundException("Selected room was not found."));

        if (reservationDao.hasOverlappingReservation(roomId, checkIn, checkOut, null)) {
            throw new ValidationException(
                    "Room " + room.getRoomNumber() + " is already booked for an overlapping date range.");
        }

        Reservation reservation = new Reservation();
        reservation.setGuestId(guestId);
        reservation.setRoomId(roomId);
        reservation.setCheckInDate(checkIn);
        reservation.setCheckOutDate(checkOut);
        reservation.setNumGuests(numGuests);
        reservation.setStatus(status);
        reservation.setNotes(notes == null ? "" : notes.trim());
        reservation.setTotalAmount(calculateTotal(room.getPricePerNight(), checkIn, checkOut));

        return reservationDao.create(reservation);
    }

    public void updateReservation(int reservationId, int roomId, LocalDate checkIn, LocalDate checkOut,
                                   int numGuests, ReservationStatus status, String notes) {
        validateCoreFields(roomId, checkIn, checkOut, numGuests, status);

        Reservation existing = getReservationOrThrow(reservationId);
        Room room = roomDao.findById(roomId)
                .orElseThrow(() -> new RecordNotFoundException("Selected room was not found."));

        if (reservationDao.hasOverlappingReservation(roomId, checkIn, checkOut, reservationId)) {
            throw new ValidationException(
                    "Room " + room.getRoomNumber() + " is already booked for an overlapping date range.");
        }

        existing.setRoomId(roomId);
        existing.setCheckInDate(checkIn);
        existing.setCheckOutDate(checkOut);
        existing.setNumGuests(numGuests);
        existing.setStatus(status);
        existing.setNotes(notes == null ? "" : notes.trim());
        existing.setTotalAmount(calculateTotal(room.getPricePerNight(), checkIn, checkOut));

        reservationDao.update(existing);
    }

    /** Customer-facing cancellation: sets status to CANCELLED rather than physically deleting. */
    public void cancelReservation(int reservationId) {
        Reservation existing = getReservationOrThrow(reservationId);
        if (existing.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new ValidationException("A checked-out reservation cannot be cancelled.");
        }
        existing.setStatus(ReservationStatus.CANCELLED);
        reservationDao.update(existing);
    }

    public void softDeleteReservation(int reservationId) {
        getReservationOrThrow(reservationId);
        reservationDao.softDelete(reservationId);
    }

    public void restoreReservation(int reservationId) {
        reservationDao.restore(reservationId);
    }

    public Reservation getReservationOrThrow(int reservationId) {
        return reservationDao.findById(reservationId)
                .orElseThrow(() -> new RecordNotFoundException("Reservation with id " + reservationId + " was not found."));
    }

    public List<Reservation> getAllActiveReservations() {
        return reservationDao.findAllActive();
    }

    public List<Reservation> getAllArchivedReservations() {
        return reservationDao.findAllArchived();
    }

    public List<Reservation> getReservationsForGuest(int guestId) {
        return reservationDao.findActiveByGuestId(guestId);
    }

    public long countActiveReservations() {
        return reservationDao.countActiveReservations();
    }

    public BigDecimal getTotalRevenue() {
        return reservationDao.sumRevenue();
    }

    private BigDecimal calculateTotal(BigDecimal pricePerNight, LocalDate checkIn, LocalDate checkOut) {
        long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }

    private void validateCoreFields(int roomId, LocalDate checkIn, LocalDate checkOut,
                                     int numGuests, ReservationStatus status) {
        Validator.requirePositive(roomId, "Room");
        Validator.validateDateRange(checkIn, checkOut);
        Validator.requirePositive(numGuests, "Number of guests");
        Validator.requireNonNull(status, "Reservation status");
    }
}
