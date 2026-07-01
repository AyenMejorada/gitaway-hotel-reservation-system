package com.hotel.service;

import com.hotel.model.RoomStatus;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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

    public String generateReport() {
        long totalRooms = roomService.countActiveRooms();
        long occupiedRooms = roomService.countByStatus(RoomStatus.OCCUPIED);
        long availableRooms = roomService.countByStatus(RoomStatus.AVAILABLE);
        long reservedRooms = roomService.countByStatus(RoomStatus.RESERVED);
        long maintenanceRooms = roomService.countByStatus(RoomStatus.MAINTENANCE);
        long totalGuests = guestService.countActiveGuests();
        BigDecimal totalRevenue = reservationService.getTotalRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        List<Reservation> activeReservations = reservationService.getAllActiveReservations();
        long pending = 0;
        long confirmed = 0;
        long checkedIn = 0;
        long checkedOut = 0;
        long cancelled = 0;
        for (Reservation r : activeReservations) {
            if (r.getStatus() != null) {
                switch (r.getStatus()) {
                    case PENDING: pending++; break;
                    case CONFIRMED: confirmed++; break;
                    case CHECKED_IN: checkedIn++; break;
                    case CHECKED_OUT: checkedOut++; break;
                    case CANCELLED: cancelled++; break;
                }
            }
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        java.text.NumberFormat revenueFormatter = java.text.NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("            GITAWAY HOTEL SYSTEM REPORT             \n");
        sb.append("====================================================\n");
        sb.append("Generated On : ").append(timestamp).append("\n");
        sb.append("Hotel Name   : Gitaway Hotel\n");
        sb.append("----------------------------------------------------\n\n");
        
        sb.append("[ ROOM INVENTORY STATISTICS ]\n");
        sb.append("  Total Active Rooms      : ").append(totalRooms).append("\n");
        sb.append("  Occupied Rooms          : ").append(occupiedRooms).append("\n");
        sb.append("  Available Rooms         : ").append(availableRooms).append("\n");
        sb.append("  Reserved Rooms          : ").append(reservedRooms).append("\n");
        sb.append("  Under Maintenance       : ").append(maintenanceRooms).append("\n\n");

        sb.append("[ GUEST STATISTICS ]\n");
        sb.append("  Total Active Guests     : ").append(totalGuests).append("\n\n");

        sb.append("[ RESERVATION STATISTICS ]\n");
        sb.append("  Total Active (Non-deleted) Reservations: ").append(activeReservations.size()).append("\n");
        sb.append("    - PENDING             : ").append(pending).append("\n");
        sb.append("    - CONFIRMED           : ").append(confirmed).append("\n");
        sb.append("    - CHECKED_IN          : ").append(checkedIn).append("\n");
        sb.append("    - CHECKED_OUT         : ").append(checkedOut).append("\n");
        sb.append("    - CANCELLED           : ").append(cancelled).append("\n\n");

        sb.append("[ FINANCIAL SUMMARY ]\n");
        sb.append("  Total Estimated Revenue : ").append(revenueFormatter.format(totalRevenue)).append("\n");
        sb.append("====================================================\n");

        return sb.toString();
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