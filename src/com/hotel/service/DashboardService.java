package com.hotel.service;

import com.hotel.model.RoomStatus;

import java.math.BigDecimal;

/**
 * Aggregates the headline metrics shown on the Admin Dashboard:
 * total active rooms, total active guests, total active reservations,
 * total revenue from confirmed/checked-in/checked-out reservations,
 * and the current occupied/available room breakdown.
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
        long occupied = roomService.countByStatus(RoomStatus.OCCUPIED);
        long available = roomService.countByStatus(RoomStatus.AVAILABLE);
        return new DashboardStats(rooms, guests, reservations, revenue, occupied, available);
    }

    /** Simple immutable holder for the dashboard's headline metrics. */
    public static class DashboardStats {
        private final long totalRooms;
        private final long totalGuests;
        private final long totalReservations;
        private final BigDecimal totalRevenue;
        private final long occupiedRooms;
        private final long availableRooms;

        public DashboardStats(long totalRooms, long totalGuests, long totalReservations,
                               BigDecimal totalRevenue, long occupiedRooms, long availableRooms) {
            this.totalRooms = totalRooms;
            this.totalGuests = totalGuests;
            this.totalReservations = totalReservations;
            this.totalRevenue = totalRevenue;
            this.occupiedRooms = occupiedRooms;
            this.availableRooms = availableRooms;
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

        public long getOccupiedRooms() {
            return occupiedRooms;
        }

        public long getAvailableRooms() {
            return availableRooms;
        }
    }
}
