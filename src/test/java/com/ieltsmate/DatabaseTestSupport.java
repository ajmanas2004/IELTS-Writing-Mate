package com.ieltsmate;

import com.ieltsmate.util.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

abstract class DatabaseTestSupport {
    protected Connection connection;
    protected String passwordHash;

    @BeforeEach
    void setUpDatabase() throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        applySchema();

        passwordHash = PasswordUtil.hashPassword("correct-password");
        insertUser(1, "student-a", "Student A", "STUDENT");
        insertUser(2, "student-b", "Student B", "STUDENT");
        insertUser(3, "teacher", "Teacher", "TEACHER");
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO writing_tasks (id, task_type, prompt, time_limit)
                    VALUES (1, 'TASK_1', 'Test prompt', 20)
                    """);
        }
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    protected void insertUser(int id, String username, String fullName, String role)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (id, username, password_hash, full_name, role) VALUES (?, ?, ?, ?, ?)")) {
            statement.setInt(1, id);
            statement.setString(2, username);
            statement.setString(3, passwordHash);
            statement.setString(4, fullName);
            statement.setString(5, role);
            statement.executeUpdate();
        }
    }

    private void applySchema() throws IOException, SQLException {
        try (InputStream input = getClass().getResourceAsStream("/database/schema.sql")) {
            if (input == null) {
                throw new IOException("Database schema resource is missing");
            }
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement statement = connection.createStatement()) {
                for (String sql : schema.split(";")) {
                    String command = sql.replaceAll("(?m)^\\s*--.*$", "").trim();
                    if (!command.isEmpty()) {
                        statement.execute(command);
                    }
                }
            }
        }
    }
}
