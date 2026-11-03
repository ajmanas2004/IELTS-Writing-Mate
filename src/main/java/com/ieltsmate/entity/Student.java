package com.ieltsmate.entity;

import java.io.Serial;
import java.io.Serializable;

public class Student extends User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public Student() {
        setRole(Role.STUDENT);
    }

    public Student(long id, String username, String passwordHash, String fullName) {
        super(id, username, passwordHash, fullName, Role.STUDENT);
    }
}
