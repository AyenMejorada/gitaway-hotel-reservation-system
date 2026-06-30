package com.hotel.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents the bill associated with a single reservation.
 * Display-only fields (guestName, roomNumber) are populated by joined
 * DAO queries for convenience in UI tables.
 */
public class Billing {

    private int billId;
    private int reservationId;
    private BigDecimal roomCharges;
    private BigDecimal additionalCharges;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Display-only fields populated by joined queries
    private String guestName;
    private String roomNumber;

    public Billing() {
        this.roomCharges = BigDecimal.ZERO;
        this.additionalCharges = BigDecimal.ZERO;
        this.discount = BigDecimal.ZERO;
        this.tax = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.paymentStatus = PaymentStatus.UNPAID;
        this.paymentMethod = PaymentMethod.NONE;
    }

    public int getBillId() {
        return billId;
    }

    public void setBillId(int billId) {
        this.billId = billId;
    }

    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public BigDecimal getRoomCharges() {
        return roomCharges;
    }

    public void setRoomCharges(BigDecimal roomCharges) {
        this.roomCharges = roomCharges;
    }

    public BigDecimal getAdditionalCharges() {
        return additionalCharges;
    }

    public void setAdditionalCharges(BigDecimal additionalCharges) {
        this.additionalCharges = additionalCharges;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    /** Recomputes totalAmount = roomCharges + additionalCharges + tax - discount. */
    public void recalculateTotal() {
        BigDecimal rc = roomCharges == null ? BigDecimal.ZERO : roomCharges;
        BigDecimal ac = additionalCharges == null ? BigDecimal.ZERO : additionalCharges;
        BigDecimal tx = tax == null ? BigDecimal.ZERO : tax;
        BigDecimal dc = discount == null ? BigDecimal.ZERO : discount;
        this.totalAmount = rc.add(ac).add(tx).subtract(dc);
    }

    @Override
    public String toString() {
        return "Bill #" + billId + " - " + totalAmount;
    }
}
