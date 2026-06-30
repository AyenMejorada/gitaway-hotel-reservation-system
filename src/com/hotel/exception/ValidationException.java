package com.hotel.exception;

/**
 * Thrown when user-supplied data fails business or format validation
 * (e.g. blank required field, invalid email, check-out before check-in).
 */
public class ValidationException extends HotelException {

    public ValidationException(String message) {
        super(message);
    }
}
