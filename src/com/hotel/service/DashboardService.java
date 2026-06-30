package com.hotel.service;

import java.math.BigDecimal;

/**
 * Aggregates the four headline metrics shown on the Admin Dashboard:
 * total active rooms, total active guests, total active reservations,
 * and total revenue from confirmed/checked-in/checked-out reservations.
 */
public class DashboardService {

    private final RoomService roomService;
    private final GuestService guestService;
    private final ReservationService reservationService;

    public DashboardService() {
        this.roomService = new RoomService();
        this.guestService = new GuestService();
        this.reservationService = new ReservationService();
    }

    public DashboardService(RoomService roomService, GuestService guestService,
                             ReservationService reservationService) {
        this.roomService = roomService;
        this.guestService = guestService;
        this.reservationService = reservationService;
    }

    public DashboardStats getStats() {
        long rooms = roomService.countActiveRooms();
        long guests = guestService.countActiveGuests();
        long reservations = reservationService.countActiveReservations();
        BigDecimal revenue = reservationService.getTotalRevenue();
        return new DashboardStats(rooms, guests, reservations, revenue);
    }

    /** Simple immutable holder for the dashboard's four headline metrics. */
    public static class DashboardStats {
        private final long totalRooms;
        private final long totalGuests;
        private final long totalReservations;
        private final BigDecimal totalRevenue;

        public DashboardStats(long totalRooms, long totalGuests, long totalReservations, BigDecimal totalRevenue) {
            this.totalRooms = totalRooms;
            this.totalGuests = totalGuests;
            this.totalReservations = totalReservations;
            this.totalRevenue = totalRevenue;
        }

        public long getTotalRooms() {
            return totalRooms;
        }

        public long getTotalGuests() {
            return totalGuests;
        }

        public long getTotalReservations() {
            return totalReservations;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }
    }
}
