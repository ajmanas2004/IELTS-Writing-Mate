package com.ieltsmate;

import com.ieltsmate.dao.UserDAO;
import com.ieltsmate.entity.Role;
import com.ieltsmate.entity.Student;
import com.ieltsmate.entity.Teacher;
import com.ieltsmate.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest extends DatabaseTestSupport {
    private AuthService authService;

    @BeforeEach
    void createAuthService() {
        authService = new AuthService(new UserDAO(connection));
    }

    @Test
    void logsInStudentAndTeacherWithTheirMatchingRoles() throws Exception {
        assertInstanceOf(Student.class, authService.login("student-a", "correct-password").orElseThrow());
        assertEquals(Role.STUDENT, authService.getCurrentUser().orElseThrow().getRole());

        authService.logout();
        assertInstanceOf(Teacher.class, authService.login("teacher", "correct-password").orElseThrow());
        assertEquals(Role.TEACHER, authService.getCurrentUser().orElseThrow().getRole());
    }

    @Test
    void rejectsWrongPasswordAndUnknownUsername() throws Exception {
        assertTrue(authService.login("student-a", "wrong-password").isEmpty());
        assertTrue(authService.getCurrentUser().isEmpty());
        assertTrue(authService.login("unknown-user", "correct-password").isEmpty());
        assertTrue(authService.getCurrentUser().isEmpty());
    }

    @Test
    void rejectsBlankUsernameOrPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("", "correct-password"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("student-a", "  "));
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(null, "correct-password"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("student-a", null));
    }
}
