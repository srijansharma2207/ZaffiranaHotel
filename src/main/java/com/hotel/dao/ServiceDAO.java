package com.hotel.dao;

import com.hotel.db.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceDAO {

    public Map<String, Double> getActiveServicePrices() throws SQLException {
        Map<String, Double> prices = new LinkedHashMap<>();
        String sql = "SELECT service_code, unit_price FROM service_catalog WHERE active = 'Y' ORDER BY service_code";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                prices.put(rs.getString("service_code"), rs.getDouble("unit_price"));
            }
        }
        return prices;
    }

    public double getUnitPrice(String serviceCode) throws SQLException {
        String sql = "SELECT unit_price FROM service_catalog WHERE service_code = ? AND active = 'Y'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, serviceCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        throw new SQLException("Service not found or inactive: " + serviceCode);
    }

    public void addServiceItem(int bookingId,
                               String serviceCode,
                               LocalDate serviceDate,
                               Integer pax,
                               int quantity,
                               double unitPrice) throws SQLException {
        double amount = unitPrice * quantity;
        String sql = "INSERT INTO booking_service_items (booking_id, service_code, service_date, pax, quantity, unit_price, amount) " +
                     "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setString(2, serviceCode);
            if (serviceDate != null) {
                ps.setDate(3, Date.valueOf(serviceDate));
            } else {
                ps.setDate(3, null);
            }
            if (pax != null) {
                ps.setInt(4, pax);
            } else {
                ps.setNull(4, java.sql.Types.NUMERIC);
            }
            ps.setInt(5, quantity);
            ps.setDouble(6, unitPrice);
            ps.setDouble(7, amount);
            ps.executeUpdate();
        }
    }

    public double getServicesTotalForBooking(int bookingId) throws SQLException {
        String sql = "SELECT NVL(SUM(amount), 0) FROM booking_service_items WHERE booking_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0;
    }

    public Map<String, Double> getBookingServiceBreakdown(int bookingId) throws SQLException {
        Map<String, Double> breakdown = new LinkedHashMap<>();

        String breakfastSql =
            "SELECT b.check_in, b.check_out, b.guests_count, b.breakfast_included, r.room_type " +
            "FROM bookings b " +
            "JOIN rooms r ON b.room_id = r.room_id " +
            "WHERE b.booking_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(breakfastSql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date checkOutDate   = rs.getDate("check_out");
                    LocalDate checkIn   = rs.getDate("check_in").toLocalDate();
                    LocalDate checkOut  = checkOutDate != null ? checkOutDate.toLocalDate() : null;
                    int guests          = rs.getInt("guests_count");
                    boolean breakfast   = "Y".equalsIgnoreCase(rs.getString("breakfast_included"));
                    String roomType     = rs.getString("room_type");

                    if (breakfast && checkOut != null) {
                        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
                        if (nights < 1) nights = 1;
                        String rt = roomType != null ? roomType.trim().toUpperCase() : "";
                        boolean free = rt.contains("DELUXE") || rt.contains("SUITE");
                        if (!free) {
                            double breakfastCost = 200.0 * guests * nights;
                            breakdown.put("Breakfast (" + guests + " guests × " + nights + " nights)", breakfastCost);
                        } else {
                            breakdown.put("Breakfast (included free)", 0.0);
                        }
                    } else if (breakfast && checkOut == null) {
                        breakdown.put("Breakfast (will be calculated at checkout)", 0.0);
                    }
                }
            }
        }

        // ✅ FIXED: use service_name instead of description
        String serviceSql =
            "SELECT bsi.service_code, bsi.quantity, bsi.amount, sc.service_name " +
            "FROM booking_service_items bsi " +
            "JOIN service_catalog sc ON bsi.service_code = sc.service_code " +
            "WHERE bsi.booking_id = ? " +
            "ORDER BY bsi.service_code";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(serviceSql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String serviceName = rs.getString("service_name");
                    int quantity       = rs.getInt("quantity");
                    double amount      = rs.getDouble("amount");
                    breakdown.put(serviceName + " (" + quantity + "×)", amount);
                }
            }
        }

        return breakdown;
    }
}