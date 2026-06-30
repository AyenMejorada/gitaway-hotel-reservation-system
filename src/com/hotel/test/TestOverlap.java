package com.hotel.test;

import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.service.ReservationService;
import com.hotel.exception.ValidationException;

import java.time.LocalDate;
import java.util.List;

public class TestOverlap {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" Starting Automated Availability & Overlap Tests ");
        System.out.println("=================================================");

        ReservationService service = new ReservationService();
        com.hotel.service.GuestService guestService = new com.hotel.service.GuestService();
        int testGuestId;
        java.util.List<com.hotel.model.Guest> guests = guestService.getAllActiveGuests();
        if (guests.isEmpty()) {
            com.hotel.model.Guest tempGuest = guestService.addGuest(null, "Test", "User", "test@user.com", "09171234567", "Test Address", "12345");
            testGuestId = tempGuest.getGuestId();
            System.out.println("    Created temporary guest ID: " + testGuestId);
        } else {
            testGuestId = guests.get(0).getGuestId();
            System.out.println("    Using existing guest ID: " + testGuestId);
        }
        int testRoomId = 1;  // Room 101 from seed

        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 5);

        Reservation res1 = null;
        Reservation res2 = null;

        try {
            System.out.println("\n[1] Creating first PENDING reservation...");
            res1 = service.addReservation(testGuestId, testRoomId, start, end, 1, ReservationStatus.PENDING, "Test 1");
            System.out.println("    Successfully created reservation ID: " + res1.getReservationId());

            System.out.println("\n[2] Attempting to create second PENDING reservation for overlapping dates...");
            res2 = service.addReservation(testGuestId, testRoomId, start, end, 1, ReservationStatus.PENDING, "Test 2");
            System.out.println("    Successfully created second reservation ID: " + res2.getReservationId());
            System.out.println("    PASS: Multiple PENDING reservations are allowed on overlapping dates!");

            System.out.println("\n[3] Confirming the first reservation...");
            service.updateReservation(res1.getReservationId(), testRoomId, start, end, 1, ReservationStatus.CONFIRMED, "Test 1 Confirmed");
            System.out.println("    Successfully confirmed reservation ID: " + res1.getReservationId());

            System.out.println("\n[4] Querying confirmed reservations for the room...");
            List<Reservation> confirmed = service.getConfirmedReservationsForRoom(testRoomId);
            final int res1Id = res1.getReservationId();
            boolean found = confirmed.stream().anyMatch(r -> r.getReservationId() == res1Id);
            if (found) {
                System.out.println("    PASS: Confirmed reservation list correctly contains reservation ID: " + res1.getReservationId());
            } else {
                throw new RuntimeException("FAIL: Confirmed reservation not returned in list.");
            }

            System.out.println("\n[5] Attempting to confirm the second reservation (should fail)...");
            try {
                service.updateReservation(res2.getReservationId(), testRoomId, start, end, 1, ReservationStatus.CONFIRMED, "Test 2 Confirmed");
                throw new RuntimeException("FAIL: Second reservation was confirmed despite overlap!");
            } catch (ValidationException e) {
                System.out.println("    PASS: Correctly threw ValidationException: " + e.getMessage());
            }

            System.out.println("\n[6] Attempting to add a new reservation on overlapping dates (should fail)...");
            try {
                service.addReservation(testGuestId, testRoomId, start, end, 1, ReservationStatus.PENDING, "Test 3");
                throw new RuntimeException("FAIL: Added a new reservation despite overlap with a CONFIRMED one!");
            } catch (ValidationException e) {
                System.out.println("    PASS: Correctly threw ValidationException: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("ERROR: Test failed with exception!");
            e.printStackTrace();
        } finally {
            // Cleanup
            System.out.println("\n[7] Cleaning up test reservations...");
            if (res1 != null) {
                try {
                    service.softDeleteReservation(res1.getReservationId());
                    System.out.println("    Archived reservation " + res1.getReservationId());
                } catch (Exception ignored) {}
            }
            if (res2 != null) {
                try {
                    service.softDeleteReservation(res2.getReservationId());
                    System.out.println("    Archived reservation " + res2.getReservationId());
                } catch (Exception ignored) {}
            }
        }
        System.out.println("\n=================================================");
        System.out.println(" Test Run Finished ");
        System.out.println("=================================================");
    }
}
