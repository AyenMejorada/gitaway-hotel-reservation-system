package com.hotel.test;

import com.hotel.model.*;
import com.hotel.service.*;
import com.hotel.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TestSystemEnhancements {

    private static final ReservationService reservationService = new ReservationService();
    private static final GuestService guestService = new GuestService();
    private static final RoomService roomService = new RoomService();
    private static final BillingService billingService = new BillingService();

    private static int guestId;
    private static int roomId; // Single Room (Capacity 1)

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   Starting System Enhancement Validation Tests  ");
        System.out.println("=================================================");

        setupTestData();

        try {
            testGuestCapacityValidation();
            testCheckInDateValidation();
            testDiscountValidation();
            testStatusTransitionValidation();
            testRoomStatusSyncing();
            testTransactionRollback();

            System.out.println("\nALL SYSTEM ENHANCEMENT TESTS PASSED SUCCESSFULLY!");
        } catch (Exception e) {
            System.err.println("\nTEST SUITE FAILED WITH ERROR:");
            e.printStackTrace();
        } finally {
            cleanupTestData();
        }

        System.out.println("=================================================");
    }

    private static void setupTestData() {
        // Find or create test guest
        List<Guest> guests = guestService.getAllActiveGuests();
        if (guests.isEmpty()) {
            Guest tempGuest = guestService.addGuest(null, "Test", "Enhance", "test@enhance.com", "09170000000", "Test Address", "ID-999");
            guestId = tempGuest.getGuestId();
        } else {
            guestId = guests.get(0).getGuestId();
        }

        // Find or create a room with capacity 1
        List<Room> rooms = roomService.getAllActiveRooms();
        Room singleRoom = rooms.stream()
                .filter(r -> r.getCapacity() == 1)
                .findFirst()
                .orElse(null);
        if (singleRoom == null) {
            singleRoom = roomService.addRoom("999", RoomType.SINGLE, new BigDecimal("1500.00"), RoomStatus.AVAILABLE, 1, "Test Single Room");
        }
        roomId = singleRoom.getRoomId();
        // Reset room status to AVAILABLE in case previous runs left it occupied
        singleRoom.setStatus(RoomStatus.AVAILABLE);
        roomService.updateRoom(roomId, singleRoom.getRoomNumber(), singleRoom.getRoomType(),
                singleRoom.getPricePerNight(), RoomStatus.AVAILABLE, singleRoom.getCapacity(), singleRoom.getDescription());
    }

    private static void cleanupTestData() {
        // Restore room status to AVAILABLE
        try {
            Room r = roomService.getRoomOrThrow(roomId);
            if (r.getStatus() != RoomStatus.AVAILABLE) {
                r.setStatus(RoomStatus.AVAILABLE);
                roomService.updateRoom(roomId, r.getRoomNumber(), r.getRoomType(), r.getPricePerNight(),
                        RoomStatus.AVAILABLE, r.getCapacity(), r.getDescription());
            }
        } catch (Exception ignored) {}
    }

    private static void testGuestCapacityValidation() {
        System.out.println("\n--- [Test 1] Guest Capacity Validation ---");
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now().plusDays(12);

        try {
            // Attempt to add a reservation with 2 guests in a room with capacity 1
            reservationService.addReservation(guestId, roomId, start, end, 2, ReservationStatus.PENDING, "Should fail");
            throw new RuntimeException("FAIL: Allowed reservation with 2 guests in room of capacity 1");
        } catch (ValidationException e) {
            System.out.println("    PASS: Blocked over-capacity booking. Message: " + e.getMessage());
        }
    }

    private static void testCheckInDateValidation() {
        System.out.println("\n--- [Test 2] Check-in Date in the Past Validation ---");
        LocalDate pastCheckIn = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);

        try {
            // Attempt to add reservation in the past
            reservationService.addReservation(guestId, roomId, pastCheckIn, end, 1, ReservationStatus.PENDING, "Should fail");
            throw new RuntimeException("FAIL: Allowed reservation with check-in date in the past");
        } catch (ValidationException e) {
            System.out.println("    PASS: Blocked check-in in the past. Message: " + e.getMessage());
        }

        // Test update reservation with past check-in
        LocalDate start = LocalDate.now().plusDays(20);
        LocalDate newEnd = LocalDate.now().plusDays(22);
        Reservation res = reservationService.addReservation(guestId, roomId, start, newEnd, 1, ReservationStatus.PENDING, "Valid");
        try {
            reservationService.updateReservation(res.getReservationId(), roomId, pastCheckIn, newEnd, 1, ReservationStatus.PENDING, "Should fail");
            throw new RuntimeException("FAIL: Allowed updating reservation check-in date to the past");
        } catch (ValidationException e) {
            System.out.println("    PASS: Blocked updating check-in to past. Message: " + e.getMessage());
        } finally {
            reservationService.softDeleteReservation(res.getReservationId());
        }
    }

    private static void testDiscountValidation() {
        System.out.println("\n--- [Test 3] Discount Exceeding Subtotal Validation ---");
        LocalDate start = LocalDate.now().plusDays(30);
        LocalDate end = LocalDate.now().plusDays(32); // 2 nights x 1500 = 3000

        Reservation res = reservationService.addReservation(guestId, roomId, start, end, 1, ReservationStatus.PENDING, "Valid");
        // To generate a bill, the reservation status must be CHECKED_OUT. Let's transition it first.
        reservationService.updateReservation(res.getReservationId(), roomId, start, end, 1, ReservationStatus.CONFIRMED, "Confirmed");
        reservationService.updateReservation(res.getReservationId(), roomId, start, end, 1, ReservationStatus.CHECKED_IN, "Checked In");
        reservationService.updateReservation(res.getReservationId(), roomId, start, end, 1, ReservationStatus.CHECKED_OUT, "Checked Out");

        Billing billing = billingService.findByReservationId(res.getReservationId())
                .orElseThrow(() -> new RuntimeException("FAIL: Bill was not automatically generated upon checkout."));

        try {
            // Room charge = 3000, Tax = 0, Add Charges = 200, Subtotal = 3200. Discount = 3500 (too high)
            billingService.updateBilling(billing.getBillId(), new BigDecimal("200.00"), new BigDecimal("3500.00"),
                    BigDecimal.ZERO, BillStatus.GENERATED);
            throw new RuntimeException("FAIL: Allowed discount to exceed subtotal charges");
        } catch (ValidationException e) {
            System.out.println("    PASS: Blocked excessive discount. Message: " + e.getMessage());
        } finally {
            billingService.softDeleteBilling(billing.getBillId());
            reservationService.softDeleteReservation(res.getReservationId());
        }
    }

    private static void testStatusTransitionValidation() {
        System.out.println("\n--- [Test 4] Status Transition Validation ---");
        LocalDate start = LocalDate.now().plusDays(40);
        LocalDate end = LocalDate.now().plusDays(42);

        Reservation res = reservationService.addReservation(guestId, roomId, start, end, 1, ReservationStatus.PENDING, "Valid");

        try {
            // Attempt to jump from PENDING directly to CHECKED_OUT
            reservationService.updateReservation(res.getReservationId(), roomId, start, end, 1, ReservationStatus.CHECKED_OUT, "Invalid transition");
            throw new RuntimeException("FAIL: Allowed invalid transition PENDING -> CHECKED_OUT");
        } catch (ValidationException e) {
            System.out.println("    PASS: Blocked PENDING -> CHECKED_OUT transition. Message: " + e.getMessage());
        }

        // Valid transition: PENDING -> CONFIRMED -> CHECKED_IN -> CHECKED_OUT
        reservationService.updateReservation(res.getReservationId(), roomId, start, end, 1, ReservationStatus.CONFIRMED, "Confirmed");
        reservationService.updateReservation(res.getReservationId(), roomId, start, end, 1, ReservationStatus.CHECKED_IN, "Checked In");

        try {
            // Invalid transition: CHECKED_IN -> PENDING
            reservationService.updateReservation(res.getReservationId(), roomId, start, end, 1, ReservationStatus.PENDING, "Invalid transition");
            throw new RuntimeException("FAIL: Allowed invalid transition CHECKED_IN -> PENDING");
        } catch (ValidationException e) {
            System.out.println("    PASS: Blocked CHECKED_IN -> PENDING transition. Message: " + e.getMessage());
        }

        // Finalize transition to CHECKED_OUT
        reservationService.updateReservation(res.getReservationId(), roomId, start, end, 1, ReservationStatus.CHECKED_OUT, "Checked Out");

        try {
            // Invalid transition: CHECKED_OUT -> CONFIRMED
            reservationService.updateReservation(res.getReservationId(), roomId, start, end, 1, ReservationStatus.CONFIRMED, "Invalid transition");
            throw new RuntimeException("FAIL: Allowed invalid transition CHECKED_OUT -> CONFIRMED");
        } catch (ValidationException e) {
            System.out.println("    PASS: Blocked transition from terminal state CHECKED_OUT. Message: " + e.getMessage());
        } finally {
            reservationService.softDeleteReservation(res.getReservationId());
        }
    }

    private static void testRoomStatusSyncing() {
        System.out.println("\n--- [Test 5] Automatic Room Status Syncing ---");
        LocalDate start = LocalDate.now().plusDays(50);
        LocalDate end = LocalDate.now().plusDays(52);

        // 1. Create a PENDING reservation
        Reservation res = reservationService.addReservation(guestId, roomId, start, end, 1, ReservationStatus.PENDING, "Valid");
        Room room = roomService.getRoomOrThrow(roomId);
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new RuntimeException("FAIL: Room is not AVAILABLE after creating PENDING reservation");
        }
        System.out.println("    PASS: Room remains AVAILABLE for PENDING reservation");

        // 2. Transition to CHECKED_IN
        reservationService.updateReservation(res.getReservationId(), roomId, start, end, 1, ReservationStatus.CONFIRMED, "Confirmed");
        reservationService.updateReservation(res.getReservationId(), roomId, start, end, 1, ReservationStatus.CHECKED_IN, "Checked In");
        room = roomService.getRoomOrThrow(roomId);
        if (room.getStatus() != RoomStatus.OCCUPIED) {
            throw new RuntimeException("FAIL: Room status is " + room.getStatus() + ", expected OCCUPIED on check-in");
        }
        System.out.println("    PASS: Room automatically became OCCUPIED on CHECKED_IN");

        // 3. Transition to CHECKED_OUT
        reservationService.updateReservation(res.getReservationId(), roomId, start, end, 1, ReservationStatus.CHECKED_OUT, "Checked Out");
        room = roomService.getRoomOrThrow(roomId);
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new RuntimeException("FAIL: Room status is " + room.getStatus() + ", expected AVAILABLE on check-out");
        }
        System.out.println("    PASS: Room automatically became AVAILABLE on CHECKED_OUT");

        reservationService.softDeleteReservation(res.getReservationId());
    }

    private static void testTransactionRollback() {
        System.out.println("\n--- [Test 6] Transaction Rollback and Atomicity ---");
        LocalDate start = LocalDate.now().plusDays(60);
        LocalDate end = LocalDate.now().plusDays(62);

        // Ensure room is AVAILABLE
        Room room = roomService.getRoomOrThrow(roomId);
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new RuntimeException("FAIL: Room setup failed. Expected AVAILABLE.");
        }

        try {
            // Attempt to add a reservation with CHECKED_IN status, but trigger a validation exception (e.g. invalid date range checkin after checkout)
            reservationService.addReservation(guestId, roomId, end, start, 1, ReservationStatus.CHECKED_IN, "Rollback test");
            throw new RuntimeException("FAIL: Allowed booking with invalid date range");
        } catch (ValidationException e) {
            System.out.println("    PASS: Correctly threw ValidationException for date range. Message: " + e.getMessage());
        }

        // Room should STILL be AVAILABLE and NOT OCCUPIED since transaction rolled back
        room = roomService.getRoomOrThrow(roomId);
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new RuntimeException("FAIL: Room status was changed to OCCUPIED despite transaction failure!");
        }
        System.out.println("    PASS: Database transaction correctly rolled back room status updates upon failure.");
    }
}
