package com.hotel.model;

public enum BillStatus {
    GENERATED("Generated"),
    PENDING_SETTLEMENT("Pending Settlement"),
    SETTLED("Settled"),
    CANCELLED("Cancelled");

    private final String displayName;

    BillStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
