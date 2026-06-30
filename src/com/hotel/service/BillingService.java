package com.hotel.service;

import com.hotel.dao.BillingDao;
import com.hotel.dao.ReservationDao;
import com.hotel.dao.impl.BillingDaoImpl;
import com.hotel.dao.impl.ReservationDaoImpl;
import com.hotel.exception.RecordNotFoundException;
import com.hotel.model.Billing;
import com.hotel.model.PaymentMethod;
import com.hotel.model.PaymentStatus;
import com.hotel.model.Reservation;
import com.hotel.util.Validator;

import java.math.BigDecimal;
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

    public Billing addBilling(int reservationId, BigDecimal additionalCharges, BigDecimal discount,
            BigDecimal tax, PaymentStatus paymentStatus, PaymentMethod paymentMethod) {
        Reservation reservation = reservationDao.findById(reservationId)
                .orElseThrow(
                        () -> new RecordNotFoundException("Reservation with id " + reservationId + " was not found."));

        validateAmounts(additionalCharges, discount, tax);
        Validator.requireNonNull(paymentStatus, "Payment status");
        Validator.requireNonNull(paymentMethod, "Payment method");

        Billing billing = new Billing();
        billing.setReservationId(reservationId);
        billing.setRoomCharges(reservation.getTotalAmount());
        billing.setAdditionalCharges(additionalCharges);
        billing.setDiscount(discount);
        billing.setTax(tax);
        billing.setPaymentStatus(paymentStatus);
        billing.setPaymentMethod(paymentMethod);
        billing.recalculateTotal();

        return billingDao.create(billing);
    }

    public void updateBilling(int billId, BigDecimal additionalCharges, BigDecimal discount, BigDecimal tax,
            PaymentStatus paymentStatus, PaymentMethod paymentMethod) {
        validateAmounts(additionalCharges, discount, tax);
        Validator.requireNonNull(paymentStatus, "Payment status");
        Validator.requireNonNull(paymentMethod, "Payment method");

        Billing existing = getBillingOrThrow(billId);
        existing.setAdditionalCharges(additionalCharges);
        existing.setDiscount(discount);
        existing.setTax(tax);
        existing.setPaymentStatus(paymentStatus);
        existing.setPaymentMethod(paymentMethod);
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
