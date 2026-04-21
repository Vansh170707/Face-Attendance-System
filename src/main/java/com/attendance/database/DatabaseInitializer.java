package com.attendance.database;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Initializes database tables
 */
public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement()) {

            // Create users table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL,
                            email TEXT UNIQUE,
                            department TEXT,
                            face_encoding BLOB,
                            image_path TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Create attendance table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS attendance (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            user_id INTEGER NOT NULL,
                            check_in TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            check_out TIMESTAMP,
                            status TEXT DEFAULT 'PRESENT',
                            FOREIGN KEY (user_id) REFERENCES users(id)
                        )
                    """);

            System.out.println("✅ Database initialized successfully!");

        } catch (Exception e) {
            System.err.println("❌ Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
