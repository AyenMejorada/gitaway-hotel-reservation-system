package com.hotel.dao;

import com.hotel.model.User;

import java.util.Optional;

/**
 * Data access contract for {@link User} records (login accounts).
 */
public interface UserDao {

    Optional<User> findByUsername(String username);

    Optional<User> findById(int userId);

    User create(User user);

    void updateActiveStatus(int userId, boolean active);
}
