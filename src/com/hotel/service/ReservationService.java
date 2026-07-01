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
import com.hotel.service.BillingService;
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
    private final BillingService billingService;

    public ReservationService() {
        this.reservationDao = new ReservationDaoImpl();
        this.roomDao = new RoomDaoImpl();
        this.billingService = new BillingService();
    }

    public ReservationService(ReservationDao reservationDao, RoomDao roomDao) {
        this.reservationDao = reservationDao;
        this.roomDao = roomDao;
        this.billingService = new BillingService();
    }

    public BigDecimal getPriceForType(com.hotel.model.RoomType type) {
        return roomDao.findAllActive().stream()
                .filter(r -> r.getRoomType() == type)
                .map(Room::getPricePerNight)
                .findFirst()
                .orElseGet(() -> {
                    switch (type) {
                        case SINGLE: return BigDecimal.valueOf(1500.00);
                        case DOUBLE: return BigDecimal.valueOf(2500.00);
                        case DELUXE: return BigDecimal.valueOf(4000.00);
                        case SUITE: return BigDecimal.valueOf(7000.00);
                        default: return BigDecimal.ZERO;
                    }
                });
    }

    public int getCapacityForType(com.hotel.model.RoomType type) {
        return roomDao.findAllActive().stream()
                .filter(r -> r.getRoomType() == type)
                .map(Room::getCapacity)
                .findFirst()
                .orElseGet(() -> {
                    switch (type) {
                        case SINGLE: return 1;
                        case DOUBLE: return 2;
                        case DELUXE: return 3;
                        case SUITE: return 4;
                        default: return 0;
                    }
                });
    }

    public Reservation addReservation(int guestId, int roomId, LocalDate checkIn, LocalDate checkOut,
            int numGuests, ReservationStatus status, String notes) {
        Room room = roomDao.findById(roomId)
                .orElseThrow(() -> new RecordNotFoundException("Selected room was not found."));
        return addReservation(guestId, room.getRoomType(), roomId, checkIn, checkOut, numGuests, status, notes);
    }

    public Reservation addReservation(int guestId, com.hotel.model.RoomType roomType, int roomId, LocalDate checkIn, LocalDate checkOut,
            int numGuests, ReservationStatus status, String notes) {
        if (roomId > 0) {
            validateCoreFields(roomId, checkIn, checkOut, numGuests, status);
            Validator.validateNotInPast(checkIn, "Check-in date");

            Room room = roomDao.findById(roomId)
                    .orElseThrow(() -> new RecordNotFoundException("Selected room was not found."));

            if (room.getStatus() == RoomStatus.MAINTENANCE) {
                throw new ValidationException(
                        "Room " + room.getRoomNumber() + " is currently in maintenance and cannot be booked.");
            }

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
            reservation.setRoomType(roomType);
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
                } else if (status == ReservationStatus.CONFIRMED) {
                    updateRoomStatus(roomId, RoomStatus.RESERVED);
                }
                TransactionManager.commit();
                return created;
            } catch (Exception e) {
                TransactionManager.rollback();
                throw e;
            }
        } else {
            Validator.validateDateRange(checkIn, checkOut);
            Validator.requirePositive(numGuests, "Number of guests");
            Validator.requireNonNull(status, "Reservation status");
            Validator.validateNotInPast(checkIn, "Check-in date");

            int maxCapacity = getCapacityForType(roomType);
            if (numGuests > maxCapacity) {
                throw new ValidationException(
                        "Number of guests exceeds the room type's maximum capacity of " + maxCapacity + ".");
            }

            BigDecimal pricePerNight = getPriceForType(roomType);

            Reservation reservation = new Reservation();
            reservation.setGuestId(guestId);
            reservation.setRoomType(roomType);
            reservation.setRoomId(0);
            reservation.setCheckInDate(checkIn);
            reservation.setCheckOutDate(checkOut);
            reservation.setNumGuests(numGuests);
            reservation.setStatus(status);
            reservation.setNotes(notes == null ? "" : notes.trim());
            reservation.setTotalAmount(calculateTotal(pricePerNight, checkIn, checkOut));

            TransactionManager.begin();
            try {
                Reservation created = reservationDao.create(reservation);
                TransactionManager.commit();
                return created;
            } catch (Exception e) {
                TransactionManager.rollback();
                throw e;
            }
        }
    }

    public void updateReservation(int reservationId, int roomId, LocalDate checkIn, LocalDate checkOut,
            int numGuests, ReservationStatus status, String notes) {
        Reservation existing = getReservationOrThrow(reservationId);
        com.hotel.model.RoomType roomType = existing.getRoomType();
        updateReservation(reservationId, roomType, roomId, checkIn, checkOut, numGuests, status, notes);
    }

    public void updateReservation(int reservationId, com.hotel.model.RoomType roomType, int roomId, LocalDate checkIn, LocalDate checkOut,
            int numGuests, ReservationStatus status, String notes) {
        Validator.validateDateRange(checkIn, checkOut);
        Validator.requirePositive(numGuests, "Number of guests");
        Validator.requireNonNull(status, "Reservation status");

        Reservation existing = getReservationOrThrow(reservationId);
        int oldRoomId = existing.getRoomId();

        if (!existing.getCheckInDate().equals(checkIn)) {
            Validator.validateNotInPast(checkIn, "Check-in date");
        }

        validateStatusTransition(existing.getStatus(), status);

        BigDecimal pricePerNight;
        if (roomId > 0) {
            Room room = roomDao.findById(roomId)
                    .orElseThrow(() -> new RecordNotFoundException("Selected room was not found."));

            if (oldRoomId != roomId && room.getStatus() == RoomStatus.MAINTENANCE) {
                throw new ValidationException(
                        "Room " + room.getRoomNumber() + " is currently in maintenance and cannot be booked.");
            }

            if (numGuests > room.getCapacity()) {
                throw new ValidationException(
                        "Number of guests exceeds the room's maximum capacity of " + room.getCapacity() + ".");
            }

            if (reservationDao.hasOverlappingReservation(roomId, checkIn, checkOut, reservationId)) {
                throw new ValidationException(
                        "Room " + room.getRoomNumber() + " is already booked for an overlapping date range.");
            }
            pricePerNight = room.getPricePerNight();
        } else {
            int maxCapacity = getCapacityForType(roomType);
            if (numGuests > maxCapacity) {
                throw new ValidationException(
                        "Number of guests exceeds the room type's maximum capacity of " + maxCapacity + ".");
            }
            pricePerNight = getPriceForType(roomType);
        }

        TransactionManager.begin();
        try {
            ReservationStatus oldStatus = existing.getStatus();

            existing.setRoomType(roomType);
            existing.setRoomId(roomId);
            existing.setCheckInDate(checkIn);
            existing.setCheckOutDate(checkOut);
            existing.setNumGuests(numGuests);
            existing.setStatus(status);
            existing.setNotes(notes == null ? "" : notes.trim());
            existing.setTotalAmount(calculateTotal(pricePerNight, checkIn, checkOut));

            reservationDao.update(existing);

            // Sync room status
            if (roomId > 0) {
                if (oldRoomId != roomId) {
                    if (oldRoomId > 0 && (oldStatus == ReservationStatus.CHECKED_IN || oldStatus == ReservationStatus.CONFIRMED)) {
                        updateRoomStatus(oldRoomId, RoomStatus.AVAILABLE);
                    }
                    if (status == ReservationStatus.CHECKED_IN) {
                        updateRoomStatus(roomId, RoomStatus.OCCUPIED);
                    } else if (status == ReservationStatus.CONFIRMED) {
                        updateRoomStatus(roomId, RoomStatus.RESERVED);
                    }
                } else {
                    if (oldStatus != status) {
                        if (status == ReservationStatus.CHECKED_IN) {
                            updateRoomStatus(roomId, RoomStatus.OCCUPIED);
                        } else if (status == ReservationStatus.CONFIRMED) {
                            updateRoomStatus(roomId, RoomStatus.RESERVED);
                        } else if (status == ReservationStatus.CHECKED_OUT || status == ReservationStatus.CANCELLED) {
                            updateRoomStatus(roomId, RoomStatus.AVAILABLE);
                        }
                    }
                }
            } else {
                if (oldRoomId > 0 && (oldStatus == ReservationStatus.CHECKED_IN || oldStatus == ReservationStatus.CONFIRMED)) {
                    updateRoomStatus(oldRoomId, RoomStatus.AVAILABLE);
                }
            }

            if (status == ReservationStatus.CHECKED_OUT) {
                billingService.generateBillForReservation(reservationId);
            }

            TransactionManager.commit();
        } catch (Exception e) {
            TransactionManager.rollback();
            throw e;
        }
    }

    public void assignRoomAndConfirm(int reservationId, int roomId) {
        Reservation res = getReservationOrThrow(reservationId);
        Room room = roomDao.findById(roomId)
                .orElseThrow(() -> new RecordNotFoundException("Room not found."));

        if (room.getRoomType() != res.getRoomType()) {
            throw new ValidationException("Selected room type (" + room.getRoomType() + ") does not match reservation room type (" + res.getRoomType() + ").");
        }

        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new ValidationException("Room " + room.getRoomNumber() + " is currently in maintenance.");
        }

        if (res.getNumGuests() > room.getCapacity()) {
            throw new ValidationException("Number of guests exceeds room capacity of " + room.getCapacity() + ".");
        }

        if (reservationDao.hasOverlappingReservation(roomId, res.getCheckInDate(), res.getCheckOutDate(), reservationId)) {
            throw new ValidationException("Room " + room.getRoomNumber() + " is already booked or occupied for the requested dates.");
        }

        TransactionManager.begin();
        try {
            res.setRoomId(roomId);
            res.setStatus(ReservationStatus.CONFIRMED);
            reservationDao.update(res);
            updateRoomStatus(roomId, RoomStatus.RESERVED);
            TransactionManager.commit();
        } catch (Exception e) {
            TransactionManager.rollback();
            throw e;
        }
    }

    /**
     * Customer-facing cancellation: sets status to CANCELLED rather than physically
     * deleting.
     */
    public void cancelReservation(int reservationId) {
        TransactionManager.begin();
        try {
            Reservation existing = getReservationOrThrow(reservationId);
            validateStatusTransition(existing.getStatus(), ReservationStatus.CANCELLED);

            int roomId = existing.getRoomId();
            ReservationStatus oldStatus = existing.getStatus();

            existing.setStatus(ReservationStatus.CANCELLED);
            reservationDao.update(existing);

            if (oldStatus == ReservationStatus.CHECKED_IN || oldStatus == ReservationStatus.CONFIRMED) {
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

    public void deleteReservationPermanently(int reservationId) {
        getReservationOrThrow(reservationId);
        TransactionManager.begin();
        try {
            billingService.deleteBillingByReservationId(reservationId);
            reservationDao.deletePermanently(reservationId);
            TransactionManager.commit();
        } catch (Exception e) {
            TransactionManager.rollback();
            throw e;
        }
    }

    public Reservation getReservationOrThrow(int reservationId) {
        return reservationDao.findById(reservationId)
                .orElseThrow(
                        () -> new RecordNotFoundException("Reservation with id " + reservationId + " was not found."));
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

    public boolean isRoomTypeAvailable(com.hotel.model.RoomType type, LocalDate checkIn, LocalDate checkOut) {
        List<Room> typeRooms = roomDao.findAllActive().stream()
                .filter(r -> r.getRoomType() == type && r.getStatus() != RoomStatus.MAINTENANCE)
                .collect(java.util.stream.Collectors.toList());
        if (typeRooms.isEmpty()) {
            return false;
        }
        for (Room r : typeRooms) {
            if (!reservationDao.hasOverlappingReservation(r.getRoomId(), checkIn, checkOut, null)) {
                return true;
            }
        }
        return false;
    }

    public List<Room> getAvailableRoomsOfType(com.hotel.model.RoomType type, LocalDate checkIn, LocalDate checkOut, Integer excludingReservationId) {
        return roomDao.findAllActive().stream()
                .filter(r -> r.getRoomType() == type && r.getStatus() != RoomStatus.MAINTENANCE)
                .filter(r -> !reservationDao.hasOverlappingReservation(r.getRoomId(), checkIn, checkOut, excludingReservationId))
                .collect(java.util.stream.Collectors.toList());
    }
}
