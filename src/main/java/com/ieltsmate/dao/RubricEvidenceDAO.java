package com.ieltsmate.dao;

import com.ieltsmate.entity.RubricEvidence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RubricEvidenceDAO {
    private final java.sql.Connection connection;

    public RubricEvidenceDAO(java.sql.Connection connection) {
        this.connection = java.util.Objects.requireNonNull(connection);
    }

    public synchronized List<RubricEvidence> findByEssayId(long essayId) throws SQLException {
        String sql = "SELECT criterion_key, opening_covered, body_covered, conclusion_covered, "
                + "evidence, rationale FROM assessment_rubric_evidence WHERE essay_id = ? "
                + "ORDER BY criterion_key";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, essayId);
            try (ResultSet result = statement.executeQuery()) {
                List<RubricEvidence> evidenceList = new ArrayList<>();
                while (result.next()) {
                    evidenceList.add(mapEvidence(result));
                }
                return evidenceList;
            }
        }
    }

    public synchronized void saveForEssay(long essayId, List<RubricEvidence> evidenceList)
            throws SQLException {
        String sql = """
                INSERT INTO assessment_rubric_evidence
                    (essay_id, criterion_key, opening_covered, body_covered,
                     conclusion_covered, evidence, rationale)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(essay_id, criterion_key) DO UPDATE SET
                    opening_covered = excluded.opening_covered,
                    body_covered = excluded.body_covered,
                    conclusion_covered = excluded.conclusion_covered,
                    evidence = excluded.evidence,
                    rationale = excluded.rationale
                """;
        boolean ownsTransaction = connection.getAutoCommit();
        if (ownsTransaction) {
            connection.setAutoCommit(false);
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (RubricEvidence evidence : evidenceList) {
                statement.setLong(1, essayId);
                statement.setString(2, evidence.criterionKey());
                statement.setBoolean(3, evidence.openingCovered());
                statement.setBoolean(4, evidence.bodyCovered());
                statement.setBoolean(5, evidence.conclusionCovered());
                statement.setString(6, evidence.evidence());
                statement.setString(7, evidence.rationale());
                statement.addBatch();
            }
            statement.executeBatch();
            if (ownsTransaction) {
                connection.commit();
            }
        } catch (SQLException | RuntimeException exception) {
            if (ownsTransaction) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }
            throw exception;
        } finally {
            if (ownsTransaction) {
                connection.setAutoCommit(true);
            }
        }
    }

    public synchronized void deleteForEssay(long essayId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM assessment_rubric_evidence WHERE essay_id = ?")) {
            statement.setLong(1, essayId);
            statement.executeUpdate();
        }
    }

    private static RubricEvidence mapEvidence(ResultSet result) throws SQLException {
        return new RubricEvidence(result.getString("criterion_key"),
                result.getBoolean("opening_covered"), result.getBoolean("body_covered"),
                result.getBoolean("conclusion_covered"), result.getString("evidence"),
                result.getString("rationale"));
    }
}
