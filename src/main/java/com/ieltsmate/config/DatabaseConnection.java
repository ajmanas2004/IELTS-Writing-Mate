package com.ieltsmate.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class DatabaseConnection {
    private static final Path DATABASE_PATH = Path.of("ielts_writing_mate.db");

    private DatabaseConnection() {
    }

    public static Connection open() throws SQLException, IOException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            initializeSchema(connection);
            seedDemoData(connection);
            return connection;
        } catch (SQLException | IOException | RuntimeException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    private static void initializeSchema(Connection connection) throws IOException, SQLException {
        for (String command : readResource("/database/schema.sql").split(";")) {
            executeIfPresent(connection, command);
        }
    }

    private static void seedDemoData(Connection connection) throws IOException, SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            if (!result.next()) {
                throw new SQLException("Could not check whether demo data has been initialized");
            }
            if (result.getLong(1) != 0) {
                return;
            }
        }

        for (String command : readResource("/database/seed.sql").split(";")) {
            executeIfPresent(connection, command);
        }
    }

    private static String readResource(String resource) throws IOException {
        try (InputStream input = DatabaseConnection.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Required database resource is missing: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void executeIfPresent(Connection connection, String sql) throws SQLException {
        String command = Objects.requireNonNull(sql).replaceAll("(?m)^\\s*--.*$", "").trim();
        if (!command.isEmpty()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(command);
            }
        }
    }
}
