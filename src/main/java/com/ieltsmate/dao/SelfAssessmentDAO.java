package com.ieltsmate.dao;

import com.ieltsmate.entity.SelfAssessment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SelfAssessmentDAO extends BaseDAO<SelfAssessment> {
    public SelfAssessmentDAO(java.sql.Connection connection) {
        super(connection);
    }

    @Override
    public Optional<SelfAssessment> findById(int id) throws SQLException {
        String sql = "SELECT id, essay_id, estimated_band, comment FROM self_assessments WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapAssessment(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<SelfAssessment> findAll() throws SQLException {
        String sql = "SELECT id, essay_id, estimated_band, comment FROM self_assessments ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<SelfAssessment> assessments = new ArrayList<>();
            while (result.next()) {
                assessments.add(mapAssessment(result));
            }
            return assessments;
        }
    }

    public Optional<SelfAssessment> findByEssayId(int essayId) throws SQLException {
        String sql = "SELECT id, essay_id, estimated_band, comment "
                + "FROM self_assessments WHERE essay_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, essayId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapAssessment(result)) : Optional.empty();
            }
        }
    }

    @Override
    public void save(SelfAssessment assessment) throws SQLException {
        String sql = "INSERT INTO self_assessments (essay_id, estimated_band, comment) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindAssessment(statement, assessment);
            statement.executeUpdate();
            assessment.setId(generatedId(statement));
        }
    }

    @Override
    public void update(SelfAssessment assessment) throws SQLException {
        String sql = "UPDATE self_assessments SET essay_id = ?, estimated_band = ?, comment = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindAssessment(statement, assessment);
            statement.setLong(4, assessment.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM self_assessments WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public void upsertByEssayId(SelfAssessment assessment) throws SQLException {
        String sql = "INSERT INTO self_assessments (essay_id, estimated_band, comment) VALUES (?, ?, ?) "
                + "ON CONFLICT(essay_id) DO UPDATE SET estimated_band = excluded.estimated_band, "
                + "comment = excluded.comment";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindAssessment(statement, assessment);
            statement.executeUpdate();
        }
        setIdByEssayId(assessment);
    }

    private void setIdByEssayId(SelfAssessment assessment) throws SQLException {
        String sql = "SELECT id FROM self_assessments WHERE essay_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessment.getEssayId());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    assessment.setId(result.getLong("id"));
                }
            }
        }
    }

    private static void bindAssessment(PreparedStatement statement, SelfAssessment assessment)
            throws SQLException {
        statement.setLong(1, assessment.getEssayId());
        if (assessment.getEstimatedBand() == null) {
            statement.setNull(2, java.sql.Types.REAL);
        } else {
            statement.setDouble(2, assessment.getEstimatedBand());
        }
        statement.setString(3, assessment.getComment());
    }

    private static SelfAssessment mapAssessment(ResultSet result) throws SQLException {
        double band = result.getDouble("estimated_band");
        Double estimatedBand = result.wasNull() ? null : band;
        return new SelfAssessment(result.getLong("id"), result.getLong("essay_id"),
                estimatedBand, result.getString("comment"));
    }
}
