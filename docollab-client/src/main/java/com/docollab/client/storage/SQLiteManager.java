package com.docollab.client.storage;

import com.docollab.shared.algorithm.CRDTNode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteManager {

    private static final String DB_URL = "jdbc:sqlite:docollab_local.db";

    public SQLiteManager() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        // EXACTLY 12 COLUMNS DEFINED HERE
        String createTableSQL = "CREATE TABLE IF NOT EXISTS DocumentNodes ("
                + "id TEXT PRIMARY KEY,"
                + "char_value TEXT NOT NULL,"
                + "left_id TEXT,"
                + "is_deleted BOOLEAN NOT NULL,"
                + "is_synced BOOLEAN NOT NULL,"
                + "is_bold BOOLEAN NOT NULL,"
                + "is_italic BOOLEAN NOT NULL,"
                + "is_underline BOOLEAN NOT NULL,"
                + "font_family TEXT,"
                + "font_size INTEGER,"
                + "color_hex TEXT,"
                + "alignment TEXT" // Column 12
                + ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);
            System.out.println("Local Database upgraded successfully.");

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    public void saveNodeLocally(CRDTNode node, boolean isSynced) {
        // EXACTLY 12 COLUMNS AND 12 QUESTION MARKS
        String insertSQL = "INSERT OR REPLACE INTO DocumentNodes "
                + "(id, char_value, left_id, is_deleted, is_synced, is_bold, is_italic, is_underline, font_family, font_size, color_hex, alignment) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, node.getId());
            pstmt.setString(2, String.valueOf(node.getValue()));
            pstmt.setString(3, node.getLeftId());
            pstmt.setBoolean(4, node.isDeleted());
            pstmt.setBoolean(5, isSynced);

            pstmt.setBoolean(6, node.isBold());
            pstmt.setBoolean(7, node.isItalic());
            pstmt.setBoolean(8, node.isUnderline());
            pstmt.setString(9, node.getFontFamily());
            pstmt.setInt(10, node.getFontSize());
            pstmt.setString(11, node.getColorHex());
            pstmt.setString(12, node.getAlignment()); // Parameter 12

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to save rich text node locally: " + e.getMessage());
        }
    }
}