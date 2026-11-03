package com.ieltsmate;

import com.ieltsmate.dao.EssayDAO;
import com.ieltsmate.dao.TeacherAssessmentDAO;
import com.ieltsmate.dao.RubricEvidenceDAO;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.EssayStatus;
import com.ieltsmate.entity.RubricEvidence;
import com.ieltsmate.entity.TaskType;
import com.ieltsmate.dao.UserDAO;
import com.ieltsmate.service.AssessmentService;
import com.ieltsmate.service.AuthService;
import com.ieltsmate.service.RubricService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentServiceTest extends DatabaseTestSupport {
    private AssessmentService assessmentService;
    private AuthService authService;

    @BeforeEach
    void createAssessmentService() {
        authService = new AuthService(new UserDAO(connection));
        assessmentService = new AssessmentService(
                new EssayDAO(connection), new TeacherAssessmentDAO(connection),
                new RubricEvidenceDAO(connection), authService);
    }

    @Test
    void calculatesIeltsOverallBandRounding() {
        assertEquals(6.5, assessmentService.calculateOverallBand(6.0, 6.0, 6.0, 7.0));
        assertEquals(7.0, assessmentService.calculateOverallBand(6.5, 6.5, 7.0, 7.0));
        assertEquals(6.0, assessmentService.calculateOverallBand(6.0, 6.0, 6.0, 6.5));
        assertEquals(6.0, assessmentService.calculateOverallBand(6.0, 6.0, 6.0, 6.0));
    }

    @Test
    void rejectsScoresOutsideRangeOrNotOnHalfBandSteps() {
        assertThrows(IllegalArgumentException.class,
                () -> assessmentService.calculateOverallBand(-0.5, 6.0, 6.0, 6.0));
        assertThrows(IllegalArgumentException.class,
                () -> assessmentService.calculateOverallBand(9.5, 6.0, 6.0, 6.0));
        assertThrows(IllegalArgumentException.class,
                () -> assessmentService.calculateOverallBand(6.25, 6.0, 6.0, 6.0));
        assertThrows(IllegalArgumentException.class,
                () -> assessmentService.calculateOverallBand(Double.NaN, 6.0, 6.0, 6.0));
    }

    @Test
    void rubricServiceReadsAndValidatesRuntimeAnnotations() {
        RubricService rubricService = new RubricService();

        assertTrue(rubricService.validateCriteria());
        assertEquals(4, rubricService.getRubricCriteria().size());
        assertFalse(rubricService.getRubricCriteria().stream()
                .anyMatch(criterion -> criterion.annotation().name().isBlank()));
        assertEquals("Task Achievement", rubricService
                .getGuidance(TaskType.TASK_1, "taskScore").displayName());
        assertEquals("Task Response", rubricService
                .getGuidance(TaskType.TASK_2, "taskScore").displayName());
        assertTrue(rubricService.getGuidance(TaskType.TASK_2, "grammarScore")
                .descriptorAnchors().contains("7:"));
    }

    @Test
    void persistsEvidenceAndRequiresTeacherAndCompleteIndicators() throws Exception {
        Essay essay = new Essay();
        essay.setStudentId(1);
        essay.setTaskId(1);
        essay.setTitle("Essay");
        essay.setContent("Essay content");
        essay.setStatus(EssayStatus.SUBMITTED);
        essay.setCreatedAt(java.time.LocalDateTime.now());
        new EssayDAO(connection).save(essay);

        authService.login("teacher", "correct-password");
        List<RubricEvidence> evidenceList = List.of(
                new RubricEvidence("taskScore", true, true, false, "Overview and key detail", "Clear coverage"),
                new RubricEvidence("coherenceScore", true, true, true, "Paragraph structure", "Logical flow"),
                new RubricEvidence("lexicalScore", true, true, false, "Word choice", "Mostly accurate"),
                new RubricEvidence("grammarScore", true, true, true, "Complex sentences", "Range with few errors"));

        assessmentService.saveRubricEvidence(essay.getId(), evidenceList);

        List<RubricEvidence> persisted = assessmentService.getRubricEvidence(essay.getId());
        assertEquals(evidenceList.size(), persisted.size());
        assertTrue(persisted.containsAll(evidenceList));
        List<RubricEvidence> unchecked = List.of(
                new RubricEvidence("taskScore", false, false, false, "Text", "Reason"),
                evidenceList.get(1), evidenceList.get(2), evidenceList.get(3));
        assessmentService.saveRubricEvidence(essay.getId(), unchecked);
        assertTrue(assessmentService.getRubricEvidence(essay.getId()).contains(unchecked.get(0)));

        authService.login("student-a", "correct-password");
        assertThrows(SecurityException.class,
                () -> assessmentService.getRubricEvidence(essay.getId()));
    }
}
