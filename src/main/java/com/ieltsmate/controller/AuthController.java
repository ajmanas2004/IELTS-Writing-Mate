package com.ieltsmate.controller;

import com.ieltsmate.entity.User;
import com.ieltsmate.service.AuthService;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = Objects.requireNonNull(authService, "authService must not be null");
    }

    public Optional<User> login(String username, String password) throws SQLException {
        return authService.login(username, password);
    }

    public void logout() {
        authService.logout();
    }

    public Optional<User> getCurrentUser() {
        return authService.getCurrentUser();
    }
}
