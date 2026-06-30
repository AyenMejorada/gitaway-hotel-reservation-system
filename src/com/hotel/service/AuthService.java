package com.hotel.service;

import com.hotel.dao.UserDao;
import com.hotel.dao.impl.UserDaoImpl;
import com.hotel.exception.AuthenticationException;
import com.hotel.exception.ValidationException;
import com.hotel.model.User;
import com.hotel.util.Validator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles login authentication and enforces a maximum of {@value #MAX_ATTEMPTS}
 * failed login attempts per username before the account is temporarily locked
 * for the remainder of the application session.
 * <p>
 * Attempt counters are kept in memory (per app run) keyed by username, which
 * is sufficient for a single desktop-application instance. The counter resets
 * automatically on a successful login.
 */
public class AuthService {

    public static final int MAX_ATTEMPTS = 3;

    private final UserDao userDao;
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private final Map<String, Boolean> lockedAccounts = new HashMap<>();

    public AuthService() {
        this.userDao = new UserDaoImpl();
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Attempts to authenticate a user.
     *
     * @throws ValidationException     if the username/password fields are blank
     * @throws AuthenticationException if credentials are wrong, account is locked,
     *                                  inactive, or does not exist
     */
    public User login(String username, String password) {
        Validator.requireNonBlank(username, "Username");
        Validator.requireNonBlank(password, "Password");

        String key = username.trim().toLowerCase();

        if (Boolean.TRUE.equals(lockedAccounts.get(key))) {
            throw new AuthenticationException(
                    "This account has been locked after " + MAX_ATTEMPTS
                            + " failed login attempts. Please restart the application or contact an administrator.");
        }

        Optional<User> userOpt = userDao.findByUsername(username.trim());

        if (!userOpt.isPresent() || !userOpt.get().getPassword().equals(password)) {
            registerFailedAttempt(key);
            int remaining = MAX_ATTEMPTS - failedAttempts.getOrDefault(key, 0);
            if (remaining <= 0) {
                lockedAccounts.put(key, true);
                throw new AuthenticationException(
                        "Invalid credentials. Maximum login attempts (" + MAX_ATTEMPTS + ") exceeded. Account locked.");
            }
            throw new AuthenticationException(
                    "Invalid username or password. " + remaining + " attempt(s) remaining.");
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            throw new AuthenticationException("This account has been deactivated. Please contact an administrator.");
        }

        // Successful login resets the counter.
        failedAttempts.remove(key);
        lockedAccounts.remove(key);
        return user;
    }

    private void registerFailedAttempt(String key) {
        failedAttempts.merge(key, 1, Integer::sum);
    }

    public int getRemainingAttempts(String username) {
        if (username == null) return MAX_ATTEMPTS;
        String key = username.trim().toLowerCase();
        return MAX_ATTEMPTS - failedAttempts.getOrDefault(key, 0);
    }

    public boolean isLocked(String username) {
        if (username == null) return false;
        return Boolean.TRUE.equals(lockedAccounts.get(username.trim().toLowerCase()));
    }
}
