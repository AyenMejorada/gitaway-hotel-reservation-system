package com.hotel.exception;

/**
 * Base unchecked exception for all application-level errors.
 * Using an unchecked exception keeps service/DAO method signatures clean
 * while still allowing the UI layer to catch and display meaningful messages.
 */
public class HotelException extends RuntimeException {

    public HotelException(String message) {
        super(message);
    }

    public HotelException(String message, Throwable cause) {
        super(message, cause);
    }
}
