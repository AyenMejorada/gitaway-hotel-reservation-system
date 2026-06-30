package com.hotel.ui.common;

import com.hotel.model.User;

/**
 * Holds the currently logged-in {@link User} for the duration of the
 * application session. A simple static holder is sufficient here since
 * this is a single-user desktop application (one login session at a time
 * per running instance).
 */
public final class Session {

    private static User currentUser;

    private Session() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}
