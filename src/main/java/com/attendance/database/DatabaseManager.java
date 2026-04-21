package com.attendance.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection manager using SQLite.
 * Returns a fresh connection per call — callers must close via try-with-resources.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:attendance.db";

    private DatabaseManager() {
    }

    /**
     * Creates and returns a new database connection.
     * Callers are responsible for closing it (use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
