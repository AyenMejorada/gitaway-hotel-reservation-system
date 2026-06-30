package com.hotel.service;

import com.hotel.dao.ReservationDao;
import com.hotel.dao.RoomDao;
import com.hotel.dao.impl.ReservationDaoImpl;
import com.hotel.dao.impl.RoomDaoImpl;
import com.hotel.exception.RecordNotFoundException;
import com.hotel.exception.ValidationException;
import com.hotel.db.TransactionManager;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
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
        Validator.validateNotInPast(checkIn, "Check-in date");

        Room room = roomDao.findById(roomId)
                .orElseThrow(() -> new RecordNotFoundException("Selected room was not found."));

        if (numGuests > room.getCapacity()) {
            throw new ValidationException(
                    "Number of guests exceeds the room's maximum capacity of " + room.getCapacity() + ".");
        }

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

        TransactionManager.begin();
        try {
            Reservation created = reservationDao.create(reservation);
            if (status == ReservationStatus.CHECKED_IN) {
                updateRoomStatus(roomId, RoomStatus.OCCUPIED);
            }
            TransactionManager.commit();
            return created;
        } catch (Exception e) {
            TransactionManager.rollback();
            throw e;
        }
    }

    public void updateReservation(int reservationId, int roomId, LocalDate checkIn, LocalDate checkOut,
                                   int numGuests, ReservationStatus status, String notes) {
        validateCoreFields(roomId, checkIn, checkOut, numGuests, status);

        Reservation existing = getReservationOrThrow(reservationId);
        if (!existing.getCheckInDate().equals(checkIn)) {
            Validator.validateNotInPast(checkIn, "Check-in date");
        }

        validateStatusTransition(existing.getStatus(), status);

        Room room = roomDao.findById(roomId)
                .orElseThrow(() -> new RecordNotFoundException("Selected room was not found."));

        if (numGuests > room.getCapacity()) {
            throw new ValidationException(
                    "Number of guests exceeds the room's maximum capacity of " + room.getCapacity() + ".");
        }

        if (reservationDao.hasOverlappingReservation(roomId, checkIn, checkOut, reservationId)) {
            throw new ValidationException(
                    "Room " + room.getRoomNumber() + " is already booked for an overlapping date range.");
        }

        TransactionManager.begin();
        try {
            int oldRoomId = existing.getRoomId();
            ReservationStatus oldStatus = existing.getStatus();

            existing.setRoomId(roomId);
            existing.setCheckInDate(checkIn);
            existing.setCheckOutDate(checkOut);
            existing.setNumGuests(numGuests);
            existing.setStatus(status);
            existing.setNotes(notes == null ? "" : notes.trim());
            existing.setTotalAmount(calculateTotal(room.getPricePerNight(), checkIn, checkOut));

            reservationDao.update(existing);

            // Sync room status
            if (oldRoomId != roomId) {
                if (oldStatus == ReservationStatus.CHECKED_IN) {
                    updateRoomStatus(oldRoomId, RoomStatus.AVAILABLE);
                }
                if (status == ReservationStatus.CHECKED_IN) {
                    updateRoomStatus(roomId, RoomStatus.OCCUPIED);
                }
            } else {
                if (oldStatus != status) {
                    if (status == ReservationStatus.CHECKED_IN) {
                        updateRoomStatus(roomId, RoomStatus.OCCUPIED);
                    } else if (status == ReservationStatus.CHECKED_OUT) {
                        updateRoomStatus(roomId, RoomStatus.AVAILABLE);
                    }
                }
            }

            TransactionManager.commit();
        } catch (Exception e) {
            TransactionManager.rollback();
            throw e;
        }
    }

    /** Customer-facing cancellation: sets status to CANCELLED rather than physically deleting. */
    public void cancelReservation(int reservationId) {
        TransactionManager.begin();
        try {
            Reservation existing = getReservationOrThrow(reservationId);
            validateStatusTransition(existing.getStatus(), ReservationStatus.CANCELLED);

            int roomId = existing.getRoomId();
            ReservationStatus oldStatus = existing.getStatus();

            existing.setStatus(ReservationStatus.CANCELLED);
            reservationDao.update(existing);

            if (oldStatus == ReservationStatus.CHECKED_IN) {
                updateRoomStatus(roomId, RoomStatus.AVAILABLE);
            }

            TransactionManager.commit();
        } catch (Exception e) {
            TransactionManager.rollback();
            throw e;
        }
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

    public List<Reservation> getConfirmedReservationsForRoom(int roomId) {
        return reservationDao.findConfirmedReservationsByRoomId(roomId);
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

    private void updateRoomStatus(int roomId, RoomStatus targetStatus) {
        roomDao.findById(roomId).ifPresent(room -> {
            if (room.getStatus() != targetStatus) {
                room.setStatus(targetStatus);
                roomDao.update(room);
            }
        });
    }

    private void validateStatusTransition(ReservationStatus current, ReservationStatus target) {
        if (current == target) {
            return;
        }
        if (current == ReservationStatus.CHECKED_OUT) {
            throw new ValidationException("A checked-out reservation cannot be changed to " + target + ".");
        }
        if (current == ReservationStatus.CANCELLED) {
            throw new ValidationException("A cancelled reservation cannot be changed to " + target + ".");
        }
        if (current == ReservationStatus.CHECKED_IN) {
            if (target != ReservationStatus.CHECKED_OUT) {
                throw new ValidationException("A checked-in reservation can only transition to CHECKED_OUT.");
            }
        }
        if (current == ReservationStatus.CONFIRMED) {
            if (target != ReservationStatus.CHECKED_IN && target != ReservationStatus.CANCELLED) {
                throw new ValidationException("A confirmed reservation can only transition to CHECKED_IN or CANCELLED.");
            }
        }
        if (current == ReservationStatus.PENDING) {
            if (target == ReservationStatus.CHECKED_OUT) {
                throw new ValidationException("A pending reservation cannot transition directly to CHECKED_OUT.");
            }
        }
    }

    private void validateCoreFields(int roomId, LocalDate checkIn, LocalDate checkOut,
                                     int numGuests, ReservationStatus status) {
        Validator.requirePositive(roomId, "Room");
        Validator.validateDateRange(checkIn, checkOut);
        Validator.requirePositive(numGuests, "Number of guests");
        Validator.requireNonNull(status, "Reservation status");
    }
}
