package com.ieltsmate.service;

import com.ieltsmate.dao.ChecklistResponseDAO;
import com.ieltsmate.dao.EssayDAO;
import com.ieltsmate.dao.SelfAssessmentDAO;
import com.ieltsmate.dao.WritingTaskDAO;
import com.ieltsmate.entity.ChecklistAnswer;
import com.ieltsmate.entity.ChecklistResponse;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.EssayStatus;
import com.ieltsmate.entity.Role;
import com.ieltsmate.entity.SelfAssessment;
import com.ieltsmate.entity.TaskType;
import com.ieltsmate.entity.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ChecklistService {
    private final ChecklistResponseDAO checklistResponseDAO;
    private final SelfAssessmentDAO selfAssessmentDAO;
    private final EssayDAO essayDAO;
    private final WritingTaskDAO writingTaskDAO;
    private final AuthService authService;

    public ChecklistService(ChecklistResponseDAO checklistResponseDAO,
                            SelfAssessmentDAO selfAssessmentDAO,
                            EssayDAO essayDAO,
                            WritingTaskDAO writingTaskDAO,
                            AuthService authService) {
        this.checklistResponseDAO = Objects.requireNonNull(checklistResponseDAO);
        this.selfAssessmentDAO = Objects.requireNonNull(selfAssessmentDAO);
        this.essayDAO = Objects.requireNonNull(essayDAO);
        this.writingTaskDAO = Objects.requireNonNull(writingTaskDAO);
        this.authService = Objects.requireNonNull(authService);
    }

    public List<ChecklistItem> getChecklistTemplate(TaskType type) {
        Objects.requireNonNull(type, "type must not be null");
        String resource = type == TaskType.TASK_1
                ? "/checklist/task-1.tsv" : "/checklist/task-2.tsv";
        InputStream input = ChecklistService.class.getResourceAsStream(resource);
        if (input == null) {
            throw new IllegalStateException("Checklist template resource is missing: " + resource);
        }

        List<ChecklistItem> items = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] columns = line.split("\t", 2);
                if (columns.length != 2 || columns[0].isBlank() || columns[1].isBlank()) {
                    throw new IllegalStateException("Invalid checklist template row in " + resource);
                }
                items.add(new ChecklistItem(columns[0], columns[1]));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read checklist template: " + resource, exception);
        }
        validateTemplate(items, resource);
        return List.copyOf(items);
    }

    public void saveResponses(int essayId, List<ChecklistResponse> responses) throws SQLException {
        Essay essay = requireOwnedEssay(essayId);
        if (essay.getStatus() != EssayStatus.DRAFT) {
            throw new IllegalStateException("Only draft essays can be edited");
        }
        Objects.requireNonNull(responses, "responses must not be null");
        for (ChecklistResponse response : responses) {
            Objects.requireNonNull(response, "responses must not contain null");
            if (response.getSectionName() == null || response.getSectionName().isBlank()
                    || response.getCriterion() == null || response.getCriterion().isBlank()
                    || response.getAnswer() == null) {
                throw new IllegalArgumentException("Checklist section, criterion, and answer are required");
            }
        }
        checklistResponseDAO.saveResponsesForEssay(essayId, responses);
    }

    public List<ChecklistResponse> getResponsesByEssay(int essayId) throws SQLException {
        requireOwnedEssay(essayId);
        return checklistResponseDAO.findByEssayId(essayId);
    }

    public int calculateCompletionPercent(int essayId) throws SQLException {
        Essay essay = requireOwnedEssay(essayId);
        TaskType taskType = writingTaskDAO.findById(Math.toIntExact(essay.getTaskId()))
                .orElseThrow(() -> new IllegalStateException("Writing task for essay was not found"))
                .getTaskType();
        return calculateCompletionPercent(taskType, checklistResponseDAO.findByEssayId(essayId));
    }

    public int calculateCompletionPercent(TaskType type, List<ChecklistResponse> responses) {
        Objects.requireNonNull(responses, "responses must not be null");
        List<ChecklistItem> template = getChecklistTemplate(type);
        int totalWeight = template.size() * 100;
        int earnedWeight = 0;
        for (ChecklistItem item : template) {
            ChecklistResponse response = responses.stream()
                    .filter(candidate -> candidate != null
                            && item.sectionName().equals(candidate.getSectionName())
                            && item.criterion().equals(candidate.getCriterion()))
                    .findFirst()
                    .orElse(null);
            if (response == null || response.getAnswer() == null) {
                continue;
            }
            earnedWeight += answerWeight(response.getAnswer());
        }
        return totalWeight == 0 ? 0 : (int) Math.round(earnedWeight * 100.0 / totalWeight);
    }

    public void saveSelfAssessment(SelfAssessment assessment) throws SQLException {
        Objects.requireNonNull(assessment, "assessment must not be null");
        Essay essay = requireOwnedEssay((int) assessment.getEssayId());
        if (essay.getStatus() != EssayStatus.DRAFT) {
            throw new IllegalStateException("Only draft essays can be edited");
        }
        if (assessment.getEstimatedBand() != null
                && (!Double.isFinite(assessment.getEstimatedBand())
                || assessment.getEstimatedBand() < 0 || assessment.getEstimatedBand() > 9
                || Math.abs(assessment.getEstimatedBand() * 2
                - Math.rint(assessment.getEstimatedBand() * 2)) > 1e-9)) {
            throw new IllegalArgumentException("Estimated band must be between 0 and 9 in 0.5 increments");
        }
        selfAssessmentDAO.upsertByEssayId(assessment);
    }

    public Optional<SelfAssessment> getSelfAssessment(int essayId) throws SQLException {
        requireOwnedEssay(essayId);
        return selfAssessmentDAO.findByEssayId(essayId);
    }

    private Essay requireOwnedEssay(int essayId) throws SQLException {
        User student = authService.requireRole(Role.STUDENT);
        Essay essay = essayDAO.findById(essayId)
                .orElseThrow(() -> new IllegalArgumentException("Essay was not found"));
        if (essay.getStudentId() != student.getId()) {
            throw new SecurityException("Students can only access their own essays");
        }
        return essay;
    }

    private static void validateTemplate(List<ChecklistItem> items, String resource) {
        if (items.isEmpty()) {
            throw new IllegalStateException("Checklist template is empty: " + resource);
        }
        var counts = new java.util.HashMap<String, Integer>();
        for (ChecklistItem item : items) {
            counts.merge(item.sectionName(), 1, Integer::sum);
        }
        if (counts.values().stream().anyMatch(count -> count < 3)) {
            throw new IllegalStateException("Each checklist section must contain at least three criteria: "
                    + resource);
        }
    }

    private static int answerWeight(ChecklistAnswer answer) {
        if (answer == null) {
            return 0;
        }
        return switch (answer) {
            case YES -> 100;
            case PARTIALLY -> 50;
            case NO -> 0;
        };
    }

    public record ChecklistItem(String sectionName, String criterion) {
    }
}