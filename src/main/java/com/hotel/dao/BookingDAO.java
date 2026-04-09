package com.hotel.dao;

import com.hotel.db.DBConnection;
import com.hotel.model.Booking;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    public int createBooking(int roomId, int guestId, LocalDate checkIn) throws SQLException {
        String sql = "INSERT INTO bookings (room_id, guest_id, check_in, guests_count, breakfast_included, status) VALUES (?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"BOOKING_ID"})) {
            ps.setInt(1, roomId);
            ps.setInt(2, guestId);
            ps.setDate(3, Date.valueOf(checkIn));
            ps.setInt(4, 1);
            ps.setString(5, "N");
            ps.setString(6, "ACTIVE");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public int createBooking(int roomId,
                             int guestId,
                             LocalDate checkIn,
                             LocalDate checkOut,
                             int guestsCount,
                             boolean breakfastIncluded) throws SQLException {
        String sql = "INSERT INTO bookings (room_id, guest_id, check_in, check_out, guests_count, breakfast_included, status) " +
                     "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"BOOKING_ID"})) {
            ps.setInt(1, roomId);
            ps.setInt(2, guestId);
            ps.setDate(3, Date.valueOf(checkIn));
            ps.setDate(4, Date.valueOf(checkOut));
            ps.setInt(5, guestsCount);
            ps.setString(6, breakfastIncluded ? "Y" : "N");
            ps.setString(7, "ACTIVE");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public double checkoutBooking(int bookingId, LocalDate checkOut) throws SQLException {
        String getSQL = "SELECT b.check_in, r.price, r.room_type, b.guests_count, b.breakfast_included " +
                        "FROM bookings b JOIN rooms r ON b.room_id = r.room_id WHERE b.booking_id = ?";

        double total = 0;
        long nights = 1;
        int guestsCount = 1;
        String roomType = "";
        String breakfastIncluded = "N";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(getSQL)) {
            ps.setInt(1, bookingId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                LocalDate checkIn = rs.getDate("check_in").toLocalDate();
                double pricePerNight = rs.getDouble("price");
                roomType = rs.getString("room_type");
                guestsCount = rs.getInt("guests_count");
                breakfastIncluded = rs.getString("breakfast_included");

                nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
                if (nights < 1) nights = 1;

                total = nights * pricePerNight;
            }
        }

        // Breakfast rules:
        // - Free for DELUXE and SUITE
        // - Chargeable for other types @ ₹200 per person per night (only if breakfast_included='Y')
        if ("Y".equalsIgnoreCase(breakfastIncluded)) {
            String rt = roomType != null ? roomType.trim().toUpperCase() : "";
            boolean free = rt.contains("DELUXE") || rt.contains("SUITE");
            if (!free) {
                total += (200.0 * guestsCount * nights);
            }
        }

        // Add extra services total from booking_service_items
        total += new ServiceDAO().getServicesTotalForBooking(bookingId);

        // Update booking
        String updateSQL = "UPDATE bookings SET check_out=?, status='CHECKED_OUT', total_amount=? WHERE booking_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(updateSQL)) {
            ps.setDate(1, Date.valueOf(checkOut));
            ps.setDouble(2, total);
            ps.setInt(3, bookingId);
            ps.executeUpdate();
        }

        // Get room_id and free the room
        String roomSQL = "SELECT room_id FROM bookings WHERE booking_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(roomSQL)) {
            ps.setInt(1, bookingId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                new RoomDAO().updateRoomStatus(rs.getInt("room_id"), "AVAILABLE");
            }
        }
        return total;
    }

    public List<Booking> getActiveBookings() throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.booking_id, b.check_in, b.status, b.total_amount, " +
                     "r.room_number, r.room_type, r.price as price_per_night, " +
                     "g.name as guest_name " +
                     "FROM bookings b " +
                     "JOIN rooms r ON b.room_id = r.room_id " +
                     "JOIN guests g ON b.guest_id = g.guest_id " +
                     "WHERE b.status = 'ACTIVE' ORDER BY b.booking_id DESC";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Booking bk = new Booking();
                bk.setBookingId(rs.getInt("booking_id"));
                bk.setCheckIn(rs.getDate("check_in").toLocalDate());
                bk.setStatus(rs.getString("status"));
                bk.setTotalAmount(rs.getDouble("total_amount"));
                bk.setRoomNumber(rs.getString("room_number"));
                bk.setRoomType(rs.getString("room_type"));
                bk.setPricePerNight(rs.getDouble("price_per_night"));
                bk.setGuestName(rs.getString("guest_name"));
                list.add(bk);
            }
        }
        return list;
    }

    public List<Booking> getAllBookings() throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.booking_id, b.check_in, b.check_out, b.status, b.total_amount, " +
                     "r.room_number, r.room_type, r.price as price_per_night, " +
                     "g.name as guest_name " +
                     "FROM bookings b " +
                     "JOIN rooms r ON b.room_id = r.room_id " +
                     "JOIN guests g ON b.guest_id = g.guest_id " +
                     "ORDER BY b.booking_id DESC";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Booking bk = new Booking();
                bk.setBookingId(rs.getInt("booking_id"));
                bk.setCheckIn(rs.getDate("check_in").toLocalDate());
                Date co = rs.getDate("check_out");
                bk.setCheckOut(co != null ? co.toLocalDate() : null);
                bk.setStatus(rs.getString("status"));
                bk.setTotalAmount(rs.getDouble("total_amount"));
                bk.setRoomNumber(rs.getString("room_number"));
                bk.setRoomType(rs.getString("room_type"));
                bk.setPricePerNight(rs.getDouble("price_per_night"));
                bk.setGuestName(rs.getString("guest_name"));
                list.add(bk);
            }
        }
        return list;
    }
}