package com.ieltsmate.util;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;

public final class PasswordUtil {
    private PasswordUtil() {
    }

    public static String hashPassword(String password) {
        Objects.requireNonNull(password, "password must not be null");
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean verifyPassword(String password, String passwordHash) {
        Objects.requireNonNull(password, "password must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        return checkPassword(password, passwordHash);
    }

    public static boolean checkPassword(String password, String passwordHash) {
        Objects.requireNonNull(password, "password must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        return BCrypt.checkpw(password, passwordHash);
    }
}
