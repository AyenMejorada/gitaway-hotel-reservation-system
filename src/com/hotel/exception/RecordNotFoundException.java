package com.hotel.exception;

/**
 * Thrown when a requested record (room, guest, reservation, bill, user)
 * cannot be found in the database, typically by its primary key.
 */
public class RecordNotFoundException extends HotelException {

    public RecordNotFoundException(String message) {
        super(message);
    }
}
