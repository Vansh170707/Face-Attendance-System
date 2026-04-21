package com.attendance.services;

import com.attendance.database.DatabaseManager;
import com.attendance.models.Attendance;
import com.attendance.models.User;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Attendance management service
 */
public class AttendanceService {

    public boolean markAttendance(int userId, String userName) {
        // Check if already marked today
        if (hasMarkedToday(userId)) {
            System.out.println("ℹ️ " + userName + " already marked attendance today.");
            return false;
        }

        String sql = "INSERT INTO attendance (user_id, check_in, status) VALUES (?, ?, 'PRESENT')";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            // Store as proper datetime string format that SQLite understands
            String timestamp = LocalDateTime.now().toString().replace("T", " ");
            pstmt.setString(2, timestamp);
            pstmt.executeUpdate();

            System.out.println("✅ Attendance marked for: " + userName);
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Failed to mark attendance: " + e.getMessage());
            return false;
        }
    }

    /**
     * Manually mark a user as ABSENT for a specific date.
     */
    public boolean markAbsent(int userId, String userName, LocalDate date) {
        // Check if already has a record for that date
        String checkSql = "SELECT COUNT(*) FROM attendance WHERE user_id = ? AND DATE(check_in) = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, userId);
            checkStmt.setString(2, date.toString());
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("ℹ️ " + userName + " already has a record for " + date);
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        // Insert ABSENT record
        String sql = "INSERT INTO attendance (user_id, check_in, status) VALUES (?, ?, 'ABSENT')";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            String timestamp = date.atTime(0, 0).toString().replace("T", " ");
            pstmt.setString(2, timestamp);
            pstmt.executeUpdate();

            System.out.println("❌ Marked ABSENT for: " + userName + " on " + date);
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Failed to mark absent: " + e.getMessage());
            return false;
        }
    }

    public boolean markCheckOut(int userId) {
        String today = LocalDate.now().toString();
        String sql = """
                    UPDATE attendance
                    SET check_out = ?
                    WHERE user_id = ?
                    AND DATE(check_in) = ?
                    AND check_out IS NULL
                """;

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String timestamp = LocalDateTime.now().toString().replace("T", " ");
            pstmt.setString(1, timestamp);
            pstmt.setInt(2, userId);
            pstmt.setString(3, today);
            int updated = pstmt.executeUpdate();

            return updated > 0;

        } catch (SQLException e) {
            System.err.println("❌ Failed to mark check-out: " + e.getMessage());
            return false;
        }
    }

    public boolean hasMarkedToday(int userId) {
        String today = LocalDate.now().toString();
        String sql = "SELECT COUNT(*) FROM attendance WHERE user_id = ? AND DATE(check_in) = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, today);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Attendance> getTodayAttendance() {
        return getAttendanceByDate(LocalDate.now());
    }

    public List<Attendance> getAttendanceByDate(LocalDate date) {
        List<Attendance> records = new ArrayList<>();
        String sql = """
                    SELECT a.*, u.name, u.department
                    FROM attendance a
                    JOIN users u ON a.user_id = u.id
                    WHERE DATE(a.check_in) = ?
                    ORDER BY a.check_in DESC
                """;

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, date.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Attendance record = new Attendance();
                record.setId(rs.getInt("id"));
                record.setUserId(rs.getInt("user_id"));
                record.setUserName(rs.getString("name"));
                record.setDepartment(rs.getString("department"));

                String checkInStr = rs.getString("check_in");
                if (checkInStr != null) {
                    record.setCheckIn(LocalDateTime.parse(checkInStr.replace(" ", "T")));
                }

                String checkOutStr = rs.getString("check_out");
                if (checkOutStr != null) {
                    record.setCheckOut(LocalDateTime.parse(checkOutStr.replace(" ", "T")));
                }

                record.setStatus(rs.getString("status"));
                records.add(record);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public List<Attendance> getAttendanceByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Attendance> records = new ArrayList<>();
        String sql = """
                    SELECT a.*, u.name, u.department
                    FROM attendance a
                    JOIN users u ON a.user_id = u.id
                    WHERE DATE(a.check_in) BETWEEN ? AND ?
                    ORDER BY a.check_in DESC
                """;

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate.toString());
            pstmt.setString(2, endDate.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Attendance record = new Attendance();
                record.setId(rs.getInt("id"));
                record.setUserId(rs.getInt("user_id"));
                record.setUserName(rs.getString("name"));
                record.setDepartment(rs.getString("department"));

                String checkInStr = rs.getString("check_in");
                if (checkInStr != null) {
                    record.setCheckIn(LocalDateTime.parse(checkInStr.replace(" ", "T")));
                }

                String checkOutStr = rs.getString("check_out");
                if (checkOutStr != null) {
                    record.setCheckOut(LocalDateTime.parse(checkOutStr.replace(" ", "T")));
                }

                record.setStatus(rs.getString("status"));
                records.add(record);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public int getTotalUsersCount() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getTodayPresentCount() {
        String today = LocalDate.now().toString();
        String sql = "SELECT COUNT(DISTINCT user_id) FROM attendance WHERE DATE(check_in) = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, today);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setDepartment(rs.getString("department"));
                user.setImagePath(rs.getString("image_path"));
                users.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}
