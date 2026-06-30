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
        Validator.requireNonBlank(firstName, "First name");
        Validator.requireMaxLength(firstName, 50, "First name");
        Validator.requireNonBlank(lastName, "Last name");
        Validator.requireMaxLength(lastName, 50, "Last name");
        Validator.validateEmail(email);
        Validator.validatePhone(phone);
    }
}
