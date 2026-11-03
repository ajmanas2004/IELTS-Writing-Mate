package com.ieltsmate.dao;

import com.ieltsmate.entity.ChecklistAnswer;
import com.ieltsmate.entity.ChecklistResponse;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChecklistResponseDAO extends BaseDAO<ChecklistResponse> {
    public ChecklistResponseDAO(java.sql.Connection connection) {
        super(connection);
    }

    @Override
    public Optional<ChecklistResponse> findById(int id) throws SQLException {
        String sql = "SELECT id, essay_id, section_name, criterion, answer "
                + "FROM checklist_responses WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapResponse(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<ChecklistResponse> findAll() throws SQLException {
        String sql = "SELECT id, essay_id, section_name, criterion, answer "
                + "FROM checklist_responses ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<ChecklistResponse> responses = new ArrayList<>();
            while (result.next()) {
                responses.add(mapResponse(result));
            }
            return responses;
        }
    }

    public List<ChecklistResponse> findByEssayId(int essayId) throws SQLException {
        String sql = "SELECT id, essay_id, section_name, criterion, answer "
                + "FROM checklist_responses WHERE essay_id = ? ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, essayId);
            try (ResultSet result = statement.executeQuery()) {
                List<ChecklistResponse> responses = new ArrayList<>();
                while (result.next()) {
                    responses.add(mapResponse(result));
                }
                return responses;
            }
        }
    }

    @Override
    public void save(ChecklistResponse response) throws SQLException {
        String sql = "INSERT INTO checklist_responses (essay_id, section_name, criterion, answer) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindResponse(statement, response);
            statement.executeUpdate();
            response.setId(generatedId(statement));
        }
    }

    @Override
    public void update(ChecklistResponse response) throws SQLException {
        String sql = "UPDATE checklist_responses SET essay_id = ?, section_name = ?, criterion = ?, "
                + "answer = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindResponse(statement, response);
            statement.setLong(5, response.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM checklist_responses WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public void saveResponsesForEssay(int essayId, List<ChecklistResponse> responses)
            throws SQLException {
        inTransaction(() -> {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM checklist_responses WHERE essay_id = ?")) {
                delete.setInt(1, essayId);
                delete.executeUpdate();
            }

            String sql = "INSERT INTO checklist_responses (essay_id, section_name, criterion, answer) "
                    + "VALUES (?, ?, ?, ?)";
            try (PreparedStatement insert = connection.prepareStatement(sql)) {
                for (ChecklistResponse response : responses) {
                    insert.setInt(1, essayId);
                    insert.setString(2, response.getSectionName());
                    insert.setString(3, response.getCriterion());
                    insert.setString(4, response.getAnswer().name());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        });
    }

    private static void bindResponse(PreparedStatement statement, ChecklistResponse response)
            throws SQLException {
        statement.setLong(1, response.getEssayId());
        statement.setString(2, response.getSectionName());
        statement.setString(3, response.getCriterion());
        statement.setString(4, response.getAnswer().name());
    }

    private static ChecklistResponse mapResponse(ResultSet result) throws SQLException {
        try {
            return new ChecklistResponse(result.getLong("id"), result.getLong("essay_id"),
                    result.getString("section_name"), result.getString("criterion"),
                    ChecklistAnswer.valueOf(result.getString("answer")));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new SQLException("Database contains an unsupported checklist answer", exception);
        }
    }
}
