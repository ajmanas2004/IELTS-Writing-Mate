package com.ieltsmate.dao;

import com.ieltsmate.entity.Role;
import com.ieltsmate.entity.Student;
import com.ieltsmate.entity.Teacher;
import com.ieltsmate.entity.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO extends BaseDAO<User> {
    public UserDAO(java.sql.Connection connection) {
        super(connection);
    }

    @Override
    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name, role FROM users WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapUser(result)) : Optional.empty();
            }
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name, role FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapUser(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name, role FROM users ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (result.next()) {
                users.add(mapUser(result));
            }
            return users;
        }
    }

    @Override
    public void save(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindUser(statement, user);
            statement.executeUpdate();
            user.setId(generatedId(statement));
        }
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, password_hash = ?, full_name = ?, role = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindUser(statement, user);
            statement.setLong(5, user.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private static void bindUser(PreparedStatement statement, User user) throws SQLException {
        statement.setString(1, user.getUsername());
        statement.setString(2, user.getPasswordHash());
        statement.setString(3, user.getFullName());
        statement.setString(4, user.getRole().name());
    }

    private static User mapUser(ResultSet result) throws SQLException {
        Role role;
        try {
            role = Role.valueOf(result.getString("role"));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new SQLException("Database contains an unsupported user role", exception);
        }

        User user = switch (role) {
            case STUDENT -> new Student();
            case TEACHER -> new Teacher();
        };
        user.setId(result.getLong("id"));
        user.setUsername(result.getString("username"));
        user.setPasswordHash(result.getString("password_hash"));
        user.setFullName(result.getString("full_name"));
        return user;
    }
}
