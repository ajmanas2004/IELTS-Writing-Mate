package com.ieltsmate.service;

import com.ieltsmate.dao.UserDAO;
import com.ieltsmate.entity.Role;
import com.ieltsmate.entity.User;
import com.ieltsmate.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class AuthService {
    private final UserDAO userDAO;
    private volatile User currentUser;

    public AuthService(UserDAO userDAO) {
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO must not be null");
    }

    public synchronized Optional<User> login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password must not be blank");
        }

        Optional<User> user = userDAO.findByUsername(username.trim());
        if (user.isEmpty()
                || !isSupportedRole(user.get().getRole())
                || !PasswordUtil.checkPassword(password, user.get().getPasswordHash())) {
            currentUser = null;
            return Optional.empty();
        }

        currentUser = user.get();
        return Optional.of(copyUser(currentUser));
    }

    public synchronized void logout() {
        currentUser = null;
    }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser).map(AuthService::copyUser);
    }

    public User requireCurrentUser() {
        return getCurrentUser().orElseThrow(() -> new IllegalStateException("No user is logged in"));
    }

    public User requireRole(Role role) {
        User user = requireCurrentUser();
        if (user.getRole() != role) {
            throw new SecurityException("This operation is not allowed for the current user");
        }
        return user;
    }

    private static User copyUser(User user) {
        User copy = switch (user.getRole()) {
            case STUDENT -> new com.ieltsmate.entity.Student();
            case TEACHER -> new com.ieltsmate.entity.Teacher();
        };
        copy.setId(user.getId());
        copy.setUsername(user.getUsername());
        copy.setPasswordHash(user.getPasswordHash());
        copy.setFullName(user.getFullName());
        return copy;
    }

    private static boolean isSupportedRole(Role role) {
        return role == Role.STUDENT || role == Role.TEACHER;
    }
}
