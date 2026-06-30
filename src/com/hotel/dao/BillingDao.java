package com.hotel.dao;

import com.hotel.model.Billing;

import java.util.List;
import java.util.Optional;

public interface BillingDao {

    Billing create(Billing billing);

    void update(Billing billing);

    void softDelete(int billId);

    void restore(int billId);

    Optional<Billing> findById(int billId);

    Optional<Billing> findByReservationId(int reservationId);

    List<Billing> findAllActive();

    List<Billing> findAllArchived();
}
