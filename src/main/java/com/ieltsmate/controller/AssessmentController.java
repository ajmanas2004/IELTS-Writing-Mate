package com.ieltsmate.controller;

import com.ieltsmate.entity.TeacherAssessment;
import com.ieltsmate.entity.RubricEvidence;
import com.ieltsmate.service.AssessmentService;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.List;

public class AssessmentController {
    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = Objects.requireNonNull(
                assessmentService, "assessmentService must not be null");
    }

    public TeacherAssessment gradeEssay(TeacherAssessment assessment) throws SQLException {
        return assessmentService.gradeEssay(assessment);
    }

    public TeacherAssessment updateAssessment(TeacherAssessment assessment) throws SQLException {
        return assessmentService.updateAssessment(assessment);
    }

    public TeacherAssessment saveAssessmentDraft(TeacherAssessment assessment) throws SQLException {
        return assessmentService.saveAssessmentDraft(assessment);
    }

    public Optional<TeacherAssessment> getAssessment(int essayId) throws SQLException {
        return assessmentService.getAssessmentByEssay(essayId);
    }

    public void saveRubricEvidence(long essayId, List<RubricEvidence> evidence)
            throws SQLException {
        assessmentService.saveRubricEvidence(essayId, evidence);
    }

    public List<RubricEvidence> getRubricEvidence(long essayId) throws SQLException {
        return assessmentService.getRubricEvidence(essayId);
    }

    public double calculateOverallBand(double task, double coherence, double lexical, double grammar) {
        return assessmentService.calculateOverallBand(task, coherence, lexical, grammar);
    }
}
