package com.ieltsmate.controller;

import com.ieltsmate.entity.ChecklistResponse;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.EssayStatus;
import com.ieltsmate.entity.SelfAssessment;
import com.ieltsmate.entity.TaskType;
import com.ieltsmate.entity.User;
import com.ieltsmate.entity.WritingTask;
import com.ieltsmate.service.BackupService;
import com.ieltsmate.service.ChecklistService;
import com.ieltsmate.service.EssayService;
import com.ieltsmate.service.ExportService;
import com.ieltsmate.service.WritingTaskService;
import com.ieltsmate.service.AuthService;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EssayController {
    private final EssayService essayService;
    private final WritingTaskService writingTaskService;
    private final ChecklistService checklistService;
    private final ExportService exportService;
    private final BackupService backupService;
    private final AuthService authService;

    public EssayController(EssayService essayService,
                           WritingTaskService writingTaskService,
                           ChecklistService checklistService,
                           ExportService exportService,
                           BackupService backupService,
                           AuthService authService) {
        this.essayService = Objects.requireNonNull(essayService);
        this.writingTaskService = Objects.requireNonNull(writingTaskService);
        this.checklistService = Objects.requireNonNull(checklistService);
        this.exportService = Objects.requireNonNull(exportService);
        this.backupService = Objects.requireNonNull(backupService);
        this.authService = Objects.requireNonNull(authService);
    }

    public User getCurrentUser() {
        return authService.requireCurrentUser();
    }

    public List<WritingTask> getTasks() throws SQLException {
        return writingTaskService.getAllTasks();
    }

    public List<WritingTask> getTasks(TaskType type) throws SQLException {
        return writingTaskService.getTasksByType(type);
    }

    public Optional<WritingTask> getTask(int taskId) throws SQLException {
        return writingTaskService.getTaskById(taskId);
    }

    public List<Essay> getMyEssays() throws SQLException {
        User user = getCurrentUser();
        return essayService.getEssaysByStudent(Math.toIntExact(user.getId()));
    }

    public List<Essay> getEssays(EssayStatus status) throws SQLException {
        return essayService.getEssaysByStatus(status);
    }

    public Optional<Essay> getEssay(int essayId) throws SQLException {
        return essayService.getEssayById(essayId);
    }

    public Essay createEssay(int taskId) throws SQLException {
        return essayService.createEssay(taskId, "", "");
    }

    public Essay saveDraft(int essayId, String title, String content) throws SQLException {
        return essayService.saveDraft(essayId, title, content);
    }

    public Essay submitEssay(int essayId, String title, String content) throws SQLException {
        return essayService.submitEssay(essayId, title, content);
    }

    public List<ChecklistService.ChecklistItem> getChecklistTemplate(TaskType type) {
        return checklistService.getChecklistTemplate(type);
    }

    public List<ChecklistResponse> getChecklistResponses(int essayId) throws SQLException {
        return checklistService.getResponsesByEssay(essayId);
    }

    public void saveChecklistResponses(int essayId, List<ChecklistResponse> responses)
            throws SQLException {
        checklistService.saveResponses(essayId, responses);
    }

    public int getChecklistCompletion(int essayId) throws SQLException {
        return checklistService.calculateCompletionPercent(essayId);
    }

    public Optional<SelfAssessment> getSelfAssessment(int essayId) throws SQLException {
        return checklistService.getSelfAssessment(essayId);
    }

    public void saveSelfAssessment(SelfAssessment assessment) throws SQLException {
        checklistService.saveSelfAssessment(assessment);
    }

    public void exportEssay(Essay essay, Path path) throws IOException {
        exportService.exportEssayToTxt(essay, path);
    }

    public void backupEssays(List<Essay> essays, Path path) throws IOException {
        backupService.backupEssays(essays, path);
    }

    public List<Essay> restoreEssays(Path path) throws IOException, ClassNotFoundException {
        return backupService.restoreEssays(path);
    }

    public void persistRestoredEssays(List<Essay> essays) throws SQLException {
        essayService.restoreEssays(essays);
    }
}
