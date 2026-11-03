package com.ieltsmate.service;

import com.ieltsmate.dao.EssayDAO;
import com.ieltsmate.dao.WritingTaskDAO;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.EssayStatus;
import com.ieltsmate.entity.Role;
import com.ieltsmate.entity.User;
import com.ieltsmate.entity.WritingTask;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EssayService {
    private final EssayDAO essayDAO;
    private final WritingTaskDAO writingTaskDAO;
    private final AuthService authService;

    public EssayService(EssayDAO essayDAO, WritingTaskDAO writingTaskDAO, AuthService authService) {
        this.essayDAO = Objects.requireNonNull(essayDAO, "essayDAO must not be null");
        this.writingTaskDAO = Objects.requireNonNull(writingTaskDAO, "writingTaskDAO must not be null");
        this.authService = Objects.requireNonNull(authService, "authService must not be null");
    }

    public Essay createEssay(int taskId, String title, String content) throws SQLException {
        User student = authService.requireRole(Role.STUDENT);
        WritingTask task = writingTaskDAO.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Writing task was not found"));
        Essay essay = new Essay(0, student.getId(), task.getId(), title, content,
                EssayStatus.DRAFT, LocalDateTime.now(), null);
        essay.setTaskType(task.getTaskType());
        essay.setStudentName(student.getFullName());
        essayDAO.save(essay);
        return essay;
    }

    public Essay saveDraft(int essayId, String title, String content) throws SQLException {
        Essay essay = requireOwnedDraft(essayId);
        essay.setTitle(title);
        essay.setContent(content);
        essayDAO.update(essay);
        return essay;
    }

    public Essay submitEssay(int essayId, String title, String content) throws SQLException {
        Essay essay = requireOwnedDraft(essayId);
        requireNonBlank(title, "Title must not be blank");
        requireNonBlank(content, "Essay content must not be blank");
        essay.setTitle(title.trim());
        essay.setContent(content.trim());
        essay.setStatus(EssayStatus.SUBMITTED);
        essay.setSubmittedAt(LocalDateTime.now());
        essayDAO.update(essay);
        return essay;
    }

    public Optional<Essay> getEssayById(int id) throws SQLException {
        Optional<Essay> essay = essayDAO.findById(id);
        if (essay.isEmpty()) {
            return Optional.empty();
        }
        ensureCanRead(essay.get());
        return essay;
    }

    public List<Essay> getEssaysByStudent(int studentId) throws SQLException {
        User user = authService.requireRole(Role.STUDENT);
        if (user.getId() != studentId) {
            throw new SecurityException("Students can only view their own essays");
        }
        return essayDAO.findByStudentId(studentId);
    }

    public List<Essay> getEssaysByStatus(EssayStatus status) throws SQLException {
        authService.requireRole(Role.TEACHER);
        Objects.requireNonNull(status, "status must not be null");
        if (status == EssayStatus.DRAFT) {
            throw new SecurityException("Teachers cannot access draft essays");
        }
        return essayDAO.findByStatus(status);
    }

    public void restoreEssays(List<Essay> essays) throws SQLException {
        User student = authService.requireRole(Role.STUDENT);
        Objects.requireNonNull(essays, "essays must not be null");
        for (Essay essay : essays) {
            Objects.requireNonNull(essay, "restored essays must not contain null");
            if (essay.getStudentId() != student.getId()) {
                throw new SecurityException("Students can only restore their own essays");
            }
            if (essay.getStatus() == null || essay.getCreatedAt() == null) {
                throw new IllegalArgumentException("Restored essays require a status and creation date");
            }
        }
        essayDAO.restoreAll(essays);
    }

    private Essay requireOwnedDraft(int essayId) throws SQLException {
        User student = authService.requireRole(Role.STUDENT);
        Essay essay = essayDAO.findById(essayId)
                .orElseThrow(() -> new IllegalArgumentException("Essay was not found"));
        if (essay.getStudentId() != student.getId()) {
            throw new SecurityException("Students can only modify their own essays");
        }
        if (essay.getStatus() != EssayStatus.DRAFT) {
            throw new IllegalStateException("Only draft essays can be modified or submitted");
        }
        return essay;
    }

    private void ensureCanRead(Essay essay) {
        User user = authService.requireCurrentUser();
        if (user.getRole() == Role.STUDENT && essay.getStudentId() != user.getId()) {
            throw new SecurityException("Students can only view their own essays");
        }
        if (user.getRole() == Role.TEACHER
                && essay.getStatus() != EssayStatus.SUBMITTED
                && essay.getStatus() != EssayStatus.GRADED) {
            throw new SecurityException("Teachers can only view submitted or graded essays");
        }
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
