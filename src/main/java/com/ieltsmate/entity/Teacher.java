package com.ieltsmate.entity;

import java.io.Serial;
import java.io.Serializable;

public class Teacher extends User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public Teacher() {
        setRole(Role.TEACHER);
    }

    public Teacher(long id, String username, String passwordHash, String fullName) {
        super(id, username, passwordHash, fullName, Role.TEACHER);
    }
}
