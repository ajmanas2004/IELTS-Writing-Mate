package com.ieltsmate.service;

import com.ieltsmate.dao.EssayDAO;
import com.ieltsmate.dao.RubricEvidenceDAO;
import com.ieltsmate.dao.TeacherAssessmentDAO;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.EssayStatus;
import com.ieltsmate.entity.Role;
import com.ieltsmate.entity.RubricEvidence;
import com.ieltsmate.entity.TeacherAssessment;
import com.ieltsmate.entity.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.Set;

public class AssessmentService {
    private final EssayDAO essayDAO;
    private final TeacherAssessmentDAO teacherAssessmentDAO;
    private final RubricEvidenceDAO rubricEvidenceDAO;
    private final AuthService authService;

    public AssessmentService(EssayDAO essayDAO, TeacherAssessmentDAO teacherAssessmentDAO,
                             RubricEvidenceDAO rubricEvidenceDAO,
                             AuthService authService) {
        this.essayDAO = Objects.requireNonNull(essayDAO, "essayDAO must not be null");
        this.teacherAssessmentDAO = Objects.requireNonNull(
                teacherAssessmentDAO, "teacherAssessmentDAO must not be null");
        this.rubricEvidenceDAO = Objects.requireNonNull(
                rubricEvidenceDAO, "rubricEvidenceDAO must not be null");
        this.authService = Objects.requireNonNull(authService, "authService must not be null");
    }

    public void saveRubricEvidence(long essayId, List<RubricEvidence> evidenceList)
            throws SQLException {
        authService.requireRole(Role.TEACHER);
        Essay essay = essayDAO.findById(Math.toIntExact(essayId))
                .orElseThrow(() -> new IllegalArgumentException("Essay was not found"));
        if (essay.getStatus() != EssayStatus.SUBMITTED && essay.getStatus() != EssayStatus.GRADED) {
            throw new IllegalStateException("Rubric evidence can only be saved for submitted essays");
        }
        validateRubricEvidence(evidenceList);
        rubricEvidenceDAO.saveForEssay(essayId, evidenceList);
    }

    public List<RubricEvidence> getRubricEvidence(long essayId) throws SQLException {
        authService.requireRole(Role.TEACHER);
        essayDAO.findById(Math.toIntExact(essayId))
                .orElseThrow(() -> new IllegalArgumentException("Essay was not found"));
        return rubricEvidenceDAO.findByEssayId(essayId);
    }

    public TeacherAssessment gradeEssay(TeacherAssessment assessment) throws SQLException {
        Objects.requireNonNull(assessment, "assessment must not be null");
        User teacher = authService.requireRole(Role.TEACHER);
        Essay essay = essayDAO.findById(Math.toIntExact(assessment.getEssayId()))
                .orElseThrow(() -> new IllegalArgumentException("Essay was not found"));
        if (essay.getStatus() != EssayStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted essays can be graded");
        }
        validateScores(assessment);
        requireMeaningfulGrade(assessment);
        assessment.setTeacherId(teacher.getId());
        assessment.setOverallBand(calculateOverallBand(assessment.getTaskScore(),
                assessment.getCoherenceScore(), assessment.getLexicalScore(),
                assessment.getGrammarScore()));
        if (assessment.getGradedAt() == null) {
            assessment.setGradedAt(LocalDateTime.now());
        }

        if (!essayDAO.sharesConnectionWith(teacherAssessmentDAO)) {
            throw new IllegalStateException("Assessment and essay DAOs must share a database connection");
        }
        essayDAO.inTransaction(() -> {
            teacherAssessmentDAO.upsertByEssayId(assessment);
            essay.setStatus(EssayStatus.GRADED);
            essayDAO.update(essay);
        });
        return assessment;
    }

    public TeacherAssessment saveAssessmentDraft(TeacherAssessment assessment) throws SQLException {
        Objects.requireNonNull(assessment, "assessment must not be null");
        User teacher = authService.requireRole(Role.TEACHER);
        Essay essay = essayDAO.findById(Math.toIntExact(assessment.getEssayId()))
                .orElseThrow(() -> new IllegalArgumentException("Essay was not found"));
        if (essay.getStatus() != EssayStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted essays can have a draft assessment");
        }
        validateScores(assessment);
        assessment.setTeacherId(teacher.getId());
        assessment.setOverallBand(calculateOverallBand(assessment.getTaskScore(),
                assessment.getCoherenceScore(), assessment.getLexicalScore(),
                assessment.getGrammarScore()));
        if (assessment.getGradedAt() == null) {
            assessment.setGradedAt(LocalDateTime.now());
        }
        teacherAssessmentDAO.upsertByEssayId(assessment);
        return assessment;
    }

    public TeacherAssessment updateAssessment(TeacherAssessment assessment) throws SQLException {
        Objects.requireNonNull(assessment, "assessment must not be null");
        User teacher = authService.requireRole(Role.TEACHER);
        Essay essay = essayDAO.findById(Math.toIntExact(assessment.getEssayId()))
                .orElseThrow(() -> new IllegalArgumentException("Essay was not found"));
        if (essay.getStatus() != EssayStatus.GRADED) {
            throw new IllegalStateException("Only graded essays can be updated");
        }
        TeacherAssessment existing = teacherAssessmentDAO.findByEssayId(Math.toIntExact(essay.getId())).orElse(null);
        if (existing != null) {
            assessment.setId(existing.getId());
        }
        validateScores(assessment);
        requireMeaningfulGrade(assessment);
        assessment.setTeacherId(teacher.getId());
        assessment.setOverallBand(calculateOverallBand(assessment.getTaskScore(),
                assessment.getCoherenceScore(), assessment.getLexicalScore(),
                assessment.getGrammarScore()));
        assessment.setGradedAt(LocalDateTime.now());
        essayDAO.inTransaction(() -> teacherAssessmentDAO.upsertByEssayId(assessment));
        return assessment;
    }

    public Optional<TeacherAssessment> getAssessmentByEssay(int essayId) throws SQLException {
        User user = authService.requireCurrentUser();
        Essay essay = essayDAO.findById(essayId)
                .orElseThrow(() -> new IllegalArgumentException("Essay was not found"));
        if (user.getRole() == Role.STUDENT && essay.getStudentId() != user.getId()) {
            throw new SecurityException("Students can only view assessments for their own essays");
        }
        if (user.getRole() != Role.STUDENT && user.getRole() != Role.TEACHER) {
            throw new SecurityException("Unsupported user role");
        }
        if (user.getRole() == Role.STUDENT && essay.getStatus() != EssayStatus.GRADED) {
            return Optional.empty();
        }
        return teacherAssessmentDAO.findByEssayId(essayId);
    }

    private static void requireMeaningfulGrade(TeacherAssessment assessment) {
        boolean hasPositiveScore = assessment.getTaskScore() > 0.0
                || assessment.getCoherenceScore() > 0.0
                || assessment.getLexicalScore() > 0.0
                || assessment.getGrammarScore() > 0.0;

        if (!hasPositiveScore) {
            throw new IllegalArgumentException("Vui lòng nhập ít nhất một điểm tiêu chí > 0.0 trước khi hoàn tất chấm.");
        }

        // Disabled: teachers can finalize assessments even when feedback/evidence fields are empty.
        // The rubric evidence validation below is also intentionally relaxed for the same reason.
    }

    public double calculateOverallBand(double taskScore, double coherenceScore,
                                       double lexicalScore, double grammarScore) {
        validateScore(taskScore);
        validateScore(coherenceScore);
        validateScore(lexicalScore);
        validateScore(grammarScore);
        double mean = (taskScore + coherenceScore + lexicalScore + grammarScore) / 4.0;
        return Math.floor(mean * 2.0 + 0.5) / 2.0;
    }

    private static void validateScores(TeacherAssessment assessment) {
        validateScore(assessment.getTaskScore());
        validateScore(assessment.getCoherenceScore());
        validateScore(assessment.getLexicalScore());
        validateScore(assessment.getGrammarScore());
    }

    private static void validateRubricEvidence(List<RubricEvidence> evidenceList) {
        Set<String> required = Set.of(
                "taskScore", "coherenceScore", "lexicalScore", "grammarScore");
        if (evidenceList == null || evidenceList.size() != required.size()
                || evidenceList.stream().anyMatch(Objects::isNull)
                || !evidenceList.stream().map(RubricEvidence::criterionKey)
                .collect(java.util.stream.Collectors.toSet()).equals(required)) {
            throw new IllegalArgumentException("Evidence and rationale are required for all four criteria");
        }
        // Disabled: evidence/rationale are no longer required for grading to proceed.
        // Teachers can save/complete assessments even when these fields are left blank.
        for (RubricEvidence evidence : evidenceList) {
            if (evidence == null || !required.contains(evidence.criterionKey())) {
                throw new IllegalArgumentException(
                        "Rubric criteria must be present for all four score fields");
            }
        }
    }

    private static void validateScore(double score) {
        if (!Double.isFinite(score) || score < 0.0 || score > 9.0
                || Math.abs(score * 2.0 - Math.rint(score * 2.0)) > 1e-9) {
            throw new IllegalArgumentException("Each rubric score must be from 0 to 9 in 0.5 increments");
        }
    }
}