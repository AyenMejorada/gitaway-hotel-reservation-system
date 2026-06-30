package com.hotel.util;

import com.hotel.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Centralized, reusable validation helpers. Every method throws
 * {@link ValidationException} with a user-friendly message on failure,
 * so callers in the service layer can simply invoke these in sequence
 * without writing repetitive if/else blocks.
 */
public final class Validator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9+()\\-\\s]{7,20}$");

    private Validator() {
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
    }

    public static void requireMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new ValidationException(fieldName + " must not exceed " + maxLength + " characters.");
        }
    }

    public static void validateEmail(String email) {
        requireNonBlank(email, "Email");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Please enter a valid email address.");
        }
    }

    public static void validatePhone(String phone) {
        requireNonBlank(phone, "Phone number");
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new ValidationException("Please enter a valid phone number.");
        }
    }

    public static void requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(fieldName + " must be greater than zero.");
        }
    }

    public static void requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(fieldName + " cannot be negative.");
        }
    }

    public static void requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be greater than zero.");
        }
    }

    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " must be selected.");
        }
    }

    public static void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        requireNonNull(checkIn, "Check-in date");
        requireNonNull(checkOut, "Check-out date");
        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationException("Check-out date must be after the check-in date.");
        }
    }

    public static void validateNotInPast(LocalDate date, String fieldName) {
        requireNonNull(date, fieldName);
        if (date.isBefore(LocalDate.now())) {
            throw new ValidationException(fieldName + " cannot be in the past.");
        }
    }

    /** Parses a BigDecimal from text, throwing a friendly ValidationException on bad input. */
    public static BigDecimal parseBigDecimal(String text, String fieldName) {
        requireNonBlank(text, fieldName);
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid number.");
        }
    }

    /** Parses an int from text, throwing a friendly ValidationException on bad input. */
    public static int parseInt(String text, String fieldName) {
        requireNonBlank(text, fieldName);
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid whole number.");
        }
    }
}
