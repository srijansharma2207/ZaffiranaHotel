package com.hotel.dao;

import com.hotel.db.DBConnection;
import com.hotel.model.AppUser;
import com.hotel.security.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class AppUserDAO {
    private static final int DEFAULT_ITERATIONS = 120_000;
    private static final int DEFAULT_KEYLEN_BITS = 256;
    private static final int SALT_BYTES = 16;

    public boolean hasAnyAdmin() throws SQLException {
        String sql = "SELECT COUNT(*) FROM app_users WHERE role = 'ADMIN'";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    public AppUser authenticate(String email, char[] password) throws SQLException {
        String sql = "SELECT user_id, email, full_name, phone, guest_id, role, password_hash, password_salt, iterations, created_at " +
            "FROM app_users WHERE LOWER(email) = LOWER(?)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String salt = rs.getString("password_salt");
                int iterations = rs.getInt("iterations");
                String expectedHash = rs.getString("password_hash");

                boolean ok = PasswordUtil.verify(password, salt, iterations, DEFAULT_KEYLEN_BITS, expectedHash);
                if (!ok) return null;

                AppUser u = new AppUser();
                u.setUserId(rs.getInt("user_id"));
                u.setEmail(rs.getString("email"));
                u.setFullName(rs.getString("full_name"));
                u.setPhone(rs.getString("phone"));
                int guestId = rs.getInt("guest_id");
                u.setGuestId(rs.wasNull() ? null : guestId);
                u.setRole(rs.getString("role"));

                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) {
                    u.setCreatedAt(LocalDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault()));
                }
                return u;
            }
        }
    }

    public AppUser createUser(String email, String fullName, String phone, Integer guestId, String role, char[] password) throws SQLException {
        String salt = PasswordUtil.generateSaltBase64(SALT_BYTES);
        String hash = PasswordUtil.pbkdf2HashBase64(password, salt, DEFAULT_ITERATIONS, DEFAULT_KEYLEN_BITS);

        String sql = "INSERT INTO app_users (email, full_name, phone, guest_id, role, password_hash, password_salt, iterations) " +
            "VALUES (?,?,?,?,?,?,?,?)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"USER_ID"})) {
            ps.setString(1, email);
            ps.setString(2, fullName);
            ps.setString(3, phone);
            if (guestId == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, guestId);
            ps.setString(5, role);
            ps.setString(6, hash);
            ps.setString(7, salt);
            ps.setInt(8, DEFAULT_ITERATIONS);
            ps.executeUpdate();

            int id = -1;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) id = rs.getInt(1);
            }

            AppUser u = new AppUser();
            u.setUserId(id);
            u.setEmail(email);
            u.setFullName(fullName);
            u.setPhone(phone);
            u.setGuestId(guestId);
            u.setRole(role);
            u.setCreatedAt(LocalDateTime.now());
            return u;
        }
    }

    public List<AppUser> listUsers() throws SQLException {
        List<AppUser> list = new ArrayList<>();
        String sql = "SELECT user_id, email, full_name, phone, guest_id, role, created_at FROM app_users ORDER BY user_id DESC";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                AppUser u = new AppUser();
                u.setUserId(rs.getInt("user_id"));
                u.setEmail(rs.getString("email"));
                u.setFullName(rs.getString("full_name"));
                u.setPhone(rs.getString("phone"));
                int guestId = rs.getInt("guest_id");
                u.setGuestId(rs.wasNull() ? null : guestId);
                u.setRole(rs.getString("role"));

                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) {
                    u.setCreatedAt(LocalDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault()));
                }
                list.add(u);
            }
        }
        return list;
    }
}
