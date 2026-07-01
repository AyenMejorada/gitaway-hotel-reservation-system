package com.hotel.dao;

import com.hotel.model.Billing;

import java.util.List;
import java.util.Optional;

public interface BillingDao {

    Billing create(Billing billing);

    void update(Billing billing);

    /**
     * Soft delete: marks record as inactive (is_deleted flag) instead of removing the row,
     * to preserve referential integrity for historical reservations/billing records.
     */
    void softDelete(int billId);

    void restore(int billId);

    void deletePermanently(int billId);

    void deleteByReservationId(int reservationId);

    Optional<Billing> findById(int billId);

    Optional<Billing> findByReservationId(int reservationId);

    List<Billing> findAllActive();

    List<Billing> findAllArchived();
}
