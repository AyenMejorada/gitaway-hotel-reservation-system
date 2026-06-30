package com.hotel.exception;

/**
 * Thrown when login credentials are invalid, the account is inactive,
 * or the account has been locked due to exceeding the maximum allowed
 * login attempts.
 */
public class AuthenticationException extends HotelException {

    public AuthenticationException(String message) {
        super(message);
    }
}
