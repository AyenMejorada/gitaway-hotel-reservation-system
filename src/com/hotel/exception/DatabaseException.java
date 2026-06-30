package com.hotel.exception;

/**
 * Thrown when a database operation fails (connection issues, SQL errors,
 * constraint violations that aren't business-validated beforehand, etc).
 */
public class DatabaseException extends HotelException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
