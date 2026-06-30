package com.hotel.dao;

import com.hotel.model.Guest;

import java.util.List;
import java.util.Optional;

public interface GuestDao {

    Guest create(Guest guest);

    void update(Guest guest);

    void softDelete(int guestId);

    void restore(int guestId);

    Optional<Guest> findById(int guestId);

    Optional<Guest> findByUserId(int userId);

    List<Guest> findAllActive();

    List<Guest> findAllArchived();

    long countActiveGuests();
}
