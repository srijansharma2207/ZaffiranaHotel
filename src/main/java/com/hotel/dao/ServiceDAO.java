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
}
