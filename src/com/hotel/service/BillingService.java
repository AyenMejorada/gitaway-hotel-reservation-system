package com.hotel.service;

import com.hotel.dao.BillingDao;
import com.hotel.dao.ReservationDao;
import com.hotel.dao.impl.BillingDaoImpl;
import com.hotel.dao.impl.ReservationDaoImpl;
import com.hotel.exception.RecordNotFoundException;
import com.hotel.exception.ValidationException;
import com.hotel.model.Billing;
import com.hotel.model.BillStatus;
import com.hotel.model.Reservation;
import com.hotel.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BillingService {

    private final BillingDao billingDao;
    private final ReservationDao reservationDao;

    public BillingService() {
        this.billingDao = new BillingDaoImpl();
        this.reservationDao = new ReservationDaoImpl();
    }

    public BillingService(BillingDao billingDao, ReservationDao reservationDao) {
        this.billingDao = billingDao;
        this.reservationDao = reservationDao;
    }

    /**
     * Automatically generates a billing record when a reservation reaches the CHECKED OUT status.
     * Prevents duplicate bill generation.
     */
    public Billing generateBillForReservation(int reservationId) {
        Optional<Billing> existing = billingDao.findByReservationId(reservationId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Reservation reservation = reservationDao.findById(reservationId)
                .orElseThrow(() -> new RecordNotFoundException("Reservation with id " + reservationId + " was not found."));

        BigDecimal roomCharges = reservation.getTotalAmount();

        Billing billing = new Billing();
        billing.setReservationId(reservationId);
        billing.setBillingDate(LocalDate.now());
        billing.setRoomCharges(roomCharges);
        billing.setAdditionalCharges(BigDecimal.ZERO);
        billing.setDiscount(BigDecimal.ZERO);
        billing.setTax(BigDecimal.ZERO);
        billing.setBillStatus(BillStatus.GENERATED);
        billing.recalculateTotal();

        return billingDao.create(billing);
    }

    public void updateBilling(int billId, BigDecimal additionalCharges, BigDecimal discount, BigDecimal tax,
            BillStatus billStatus) {
        validateAmounts(additionalCharges, discount, tax);
        Validator.requireNonNull(billStatus, "Bill status");

        Billing existing = getBillingOrThrow(billId);
        BigDecimal roomCharges = existing.getRoomCharges();
        BigDecimal subtotal = roomCharges.add(additionalCharges).add(tax);
        if (discount.compareTo(subtotal) > 0) {
            throw new ValidationException("Discount cannot exceed the total charges of ₱" + subtotal + ".");
        }

        existing.setAdditionalCharges(additionalCharges);
        existing.setDiscount(discount);
        existing.setTax(tax);
        existing.setBillStatus(billStatus);
        existing.recalculateTotal();

        billingDao.update(existing);
    }

    public void softDeleteBilling(int billId) {
        getBillingOrThrow(billId);
        billingDao.softDelete(billId);
    }

    public void restoreBilling(int billId) {
        billingDao.restore(billId);
    }

    public Billing getBillingOrThrow(int billId) {
        return billingDao.findById(billId)
                .orElseThrow(() -> new RecordNotFoundException("Bill with id " + billId + " was not found."));
    }

    public Optional<Billing> findByReservationId(int reservationId) {
        return billingDao.findByReservationId(reservationId);
    }

    public List<Billing> getAllActiveBillings() {
        return billingDao.findAllActive();
    }

    public List<Billing> getAllArchivedBillings() {
        return billingDao.findAllArchived();
    }

    private void validateAmounts(BigDecimal additionalCharges, BigDecimal discount, BigDecimal tax) {
        Validator.requireNonNegative(additionalCharges, "Additional charges");
        Validator.requireNonNegative(discount, "Discount");
        Validator.requireNonNegative(tax, "Tax");
    }
}
