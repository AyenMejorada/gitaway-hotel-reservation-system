package com.hotel.ui.customer;

import com.hotel.model.Guest;
import com.hotel.model.User;
import com.hotel.service.GuestService;
import com.hotel.ui.common.Session;

/**
 * Resolves the {@link Guest} record linked to the currently logged-in
 * customer {@link User}. If the customer account has never made a
 * reservation before (no linked guest profile exists yet), a guest
 * profile is automatically created from the account's basic info the
 * first time it's needed.
 */
public final class CustomerContext {

    private static final GuestService guestService = new GuestService();

    private CustomerContext() {
    }

    public static Guest getOrCreateCurrentGuest() {
        User user = Session.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No user is currently logged in.");
        }

        return guestService.findByUserId(user.getUserId())
                .orElseGet(() -> createGuestForUser(user));
    }

    private static Guest createGuestForUser(User user) {
        String[] nameParts = splitName(user.getFullName());
        String email = user.getEmail() == null || user.getEmail().trim().isEmpty()
                ? user.getUsername() + "@guest.local"
                : user.getEmail();
        // Placeholder phone/address so the initial profile passes validation;
        // the customer can refine these later via Guest Management if needed.
        return guestService.addGuest(user.getUserId(), nameParts[0], nameParts[1], email,
                "0000000000", "Not specified", "");
    }

    private static String[] splitName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new String[]{"Guest", "User"};
        }
        String trimmed = fullName.trim();
        int spaceIdx = trimmed.indexOf(' ');
        if (spaceIdx < 0) {
            return new String[]{trimmed, "-"};
        }
        return new String[]{trimmed.substring(0, spaceIdx), trimmed.substring(spaceIdx + 1)};
    }
}
