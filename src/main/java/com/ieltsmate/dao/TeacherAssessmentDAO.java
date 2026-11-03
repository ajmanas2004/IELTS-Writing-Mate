package com.ieltsmate.dao;

import com.ieltsmate.entity.TeacherAssessment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TeacherAssessmentDAO extends BaseDAO<TeacherAssessment> {
    public TeacherAssessmentDAO(java.sql.Connection connection) {
        super(connection);
    }

    @Override
    public Optional<TeacherAssessment> findById(int id) throws SQLException {
        String sql = "SELECT id, essay_id, teacher_id, task_score, coherence_score, lexical_score, "
                + "grammar_score, overall_band, feedback, graded_at FROM teacher_assessments WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapAssessment(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<TeacherAssessment> findAll() throws SQLException {
        String sql = "SELECT id, essay_id, teacher_id, task_score, coherence_score, lexical_score, "
                + "grammar_score, overall_band, feedback, graded_at FROM teacher_assessments ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<TeacherAssessment> assessments = new ArrayList<>();
            while (result.next()) {
                assessments.add(mapAssessment(result));
            }
            return assessments;
        }
    }

    public Optional<TeacherAssessment> findByEssayId(int essayId) throws SQLException {
        String sql = "SELECT id, essay_id, teacher_id, task_score, coherence_score, lexical_score, "
                + "grammar_score, overall_band, feedback, graded_at "
                + "FROM teacher_assessments WHERE essay_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, essayId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapAssessment(result)) : Optional.empty();
            }
        }
    }

    @Override
    public void save(TeacherAssessment assessment) throws SQLException {
        String sql = "INSERT INTO teacher_assessments "
                + "(essay_id, teacher_id, task_score, coherence_score, lexical_score, grammar_score, "
                + "overall_band, feedback, graded_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindAssessment(statement, assessment);
            statement.executeUpdate();
            assessment.setId(generatedId(statement));
        }
    }

    @Override
    public void update(TeacherAssessment assessment) throws SQLException {
        String sql = "UPDATE teacher_assessments SET essay_id = ?, teacher_id = ?, task_score = ?, "
                + "coherence_score = ?, lexical_score = ?, grammar_score = ?, overall_band = ?, "
                + "feedback = ?, graded_at = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindAssessment(statement, assessment);
            statement.setLong(10, assessment.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM teacher_assessments WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public void upsertByEssayId(TeacherAssessment assessment) throws SQLException {
        String sql = "INSERT INTO teacher_assessments "
                + "(essay_id, teacher_id, task_score, coherence_score, lexical_score, grammar_score, "
                + "overall_band, feedback, graded_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(essay_id) DO UPDATE SET teacher_id = excluded.teacher_id, "
                + "task_score = excluded.task_score, coherence_score = excluded.coherence_score, "
                + "lexical_score = excluded.lexical_score, grammar_score = excluded.grammar_score, "
                + "overall_band = excluded.overall_band, feedback = excluded.feedback, "
                + "graded_at = excluded.graded_at";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindAssessment(statement, assessment);
            statement.executeUpdate();
        }
        try (PreparedStatement statement =
                     connection.prepareStatement("SELECT id FROM teacher_assessments WHERE essay_id = ?")) {
            statement.setLong(1, assessment.getEssayId());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    assessment.setId(result.getLong("id"));
                }
            }
        }
    }

    private static void bindAssessment(PreparedStatement statement, TeacherAssessment assessment)
            throws SQLException {
        statement.setLong(1, assessment.getEssayId());
        statement.setLong(2, assessment.getTeacherId());
        statement.setDouble(3, assessment.getTaskScore());
        statement.setDouble(4, assessment.getCoherenceScore());
        statement.setDouble(5, assessment.getLexicalScore());
        statement.setDouble(6, assessment.getGrammarScore());
        statement.setDouble(7, assessment.getOverallBand());
        statement.setString(8, assessment.getFeedback());
        statement.setString(9, assessment.getGradedAt().toString());
    }

    private static TeacherAssessment mapAssessment(ResultSet result) throws SQLException {
        return new TeacherAssessment(result.getLong("id"), result.getLong("essay_id"),
                result.getLong("teacher_id"), result.getDouble("task_score"),
                result.getDouble("coherence_score"), result.getDouble("lexical_score"),
                result.getDouble("grammar_score"), result.getDouble("overall_band"),
                result.getString("feedback"), parseDateTime(result.getString("graded_at")));
    }

    private static LocalDateTime parseDateTime(String value) throws SQLException {
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (java.time.format.DateTimeParseException | NullPointerException exception) {
            throw new SQLException("Database contains an invalid date/time value", exception);
        }
    }
}
