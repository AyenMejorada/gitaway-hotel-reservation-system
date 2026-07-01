package com.hotel.service;

import com.hotel.dao.GuestDao;
import com.hotel.dao.impl.GuestDaoImpl;
import com.hotel.exception.RecordNotFoundException;
import com.hotel.model.Guest;
import com.hotel.util.Validator;

import java.util.List;
import java.util.Optional;

public class GuestService {

    private final GuestDao guestDao;

    public GuestService() {
        this.guestDao = new GuestDaoImpl();
    }

    public GuestService(GuestDao guestDao) {
        this.guestDao = guestDao;
    }

    public Guest addGuest(Integer userId, String firstName, String lastName, String email,
                           String phone, String address, String idNumber) {
        validateGuestFields(firstName, lastName, email, phone);
        Guest guest = new Guest(firstName.trim(), lastName.trim(), email.trim(), phone.trim(),
                address == null ? "" : address.trim(), idNumber == null ? "" : idNumber.trim());
        guest.setUserId(userId);
        return guestDao.create(guest);
    }

    public void updateGuest(int guestId, String firstName, String lastName, String email,
                             String phone, String address, String idNumber) {
        validateGuestFields(firstName, lastName, email, phone);
        Guest existing = getGuestOrThrow(guestId);
        existing.setFirstName(firstName.trim());
        existing.setLastName(lastName.trim());
        existing.setEmail(email.trim());
        existing.setPhone(phone.trim());
        existing.setAddress(address == null ? "" : address.trim());
        existing.setIdNumber(idNumber == null ? "" : idNumber.trim());
        guestDao.update(existing);
    }

    public void softDeleteGuest(int guestId) {
        getGuestOrThrow(guestId);
        guestDao.softDelete(guestId);
    }

    public void restoreGuest(int guestId) {
        guestDao.restore(guestId);
    }

    public void deleteGuestPermanently(int guestId) {
        getGuestOrThrow(guestId);
        try {
            guestDao.deletePermanently(guestId);
        } catch (com.hotel.exception.DatabaseException e) {
            if (e.getCause() instanceof java.sql.SQLException) {
                java.sql.SQLException se = (java.sql.SQLException) e.getCause();
                if (se.getSQLState() != null && se.getSQLState().startsWith("23")) {
                    throw new com.hotel.exception.ValidationException(
                            "Cannot permanently delete this guest because they have associated reservations.");
                }
            }
            throw e;
        }
    }

    public Guest getGuestOrThrow(int guestId) {
        return guestDao.findById(guestId)
                .orElseThrow(() -> new RecordNotFoundException("Guest with id " + guestId + " was not found."));
    }

    public Optional<Guest> findByUserId(int userId) {
        return guestDao.findByUserId(userId);
    }

    public List<Guest> getAllActiveGuests() {
        return guestDao.findAllActive();
    }

    public List<Guest> getAllArchivedGuests() {
        return guestDao.findAllArchived();
    }

    public long countActiveGuests() {
        return guestDao.countActiveGuests();
    }

    private void validateGuestFields(String firstName, String lastName, String email, String phone) {
        Validator.requireNonBlank(firstName, "First name", 50);
        Validator.requireNonBlank(lastName, "Last name", 50);
        Validator.validateEmail(email);
        Validator.validatePhone(phone);
    }
}
