package com.hotel.dao;

import com.hotel.model.Guest;

import java.util.List;
import java.util.Optional;

public interface GuestDao {

    Guest create(Guest guest);

    void update(Guest guest);

    /**
     * Soft delete: marks record as inactive (is_deleted flag) instead of removing the row,
     * to preserve referential integrity for historical reservations/billing records.
     */
    void softDelete(int guestId);

    void restore(int guestId);

    void deletePermanently(int guestId);

    Optional<Guest> findById(int guestId);

    Optional<Guest> findByUserId(int userId);

    List<Guest> findAllActive();

    List<Guest> findAllArchived();

    long countActiveGuests();
}
