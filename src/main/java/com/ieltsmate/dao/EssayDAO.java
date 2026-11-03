package com.ieltsmate.dao;

import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.EssayStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EssayDAO extends BaseDAO<Essay> {
    private static final String COLUMNS =
            "id, student_id, task_id, title, content, status, created_at, submitted_at, "
                    + "(SELECT full_name FROM users WHERE id = essays.student_id) AS student_name, "
                    + "(SELECT task_type FROM writing_tasks WHERE id = essays.task_id) AS task_type";

    public EssayDAO(java.sql.Connection connection) {
        super(connection);
    }

    @Override
    public Optional<Essay> findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM essays WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapEssay(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Essay> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM essays ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<Essay> essays = new ArrayList<>();
            while (result.next()) {
                essays.add(mapEssay(result));
            }
            return essays;
        }
    }

    public List<Essay> findByStudentId(int studentId) throws SQLException {
        return findMany("SELECT " + COLUMNS + " FROM essays WHERE student_id = ? ORDER BY id",
                statement -> statement.setInt(1, studentId));
    }

    public List<Essay> findByStatus(EssayStatus status) throws SQLException {
        return findMany("SELECT " + COLUMNS + " FROM essays WHERE status = ? ORDER BY id",
                statement -> statement.setString(1, status.name()));
    }

    @Override
    public void save(Essay essay) throws SQLException {
        String sql = "INSERT INTO essays (student_id, task_id, title, content, status, created_at, submitted_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEssay(statement, essay, false);
            statement.executeUpdate();
            essay.setId(generatedId(statement));
        }
    }

    @Override
    public void update(Essay essay) throws SQLException {
        String sql = "UPDATE essays SET student_id = ?, task_id = ?, title = ?, content = ?, "
                + "status = ?, created_at = ?, submitted_at = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEssay(statement, essay, false);
            statement.setLong(8, essay.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM essays WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public void restoreAll(List<Essay> essays) throws SQLException {
        List<Essay> eligible = new ArrayList<>();
        for (Essay essay : essays) {
            if (essay == null) {
                continue;
            }
            Essay normalized = new Essay(
                    essay.getId(),
                    essay.getStudentId(),
                    essay.getTaskId(),
                    essay.getTitle(),
                    essay.getContent(),
                    EssayStatus.DRAFT,
                    essay.getCreatedAt(),
                    null);
            normalized.setStudentName(essay.getStudentName());
            normalized.setTaskType(essay.getTaskType());
            eligible.add(normalized);
        }
        String sql = "INSERT INTO essays (id, student_id, task_id, title, content, status, created_at, submitted_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(id) DO UPDATE SET student_id = excluded.student_id, "
                + "task_id = excluded.task_id, title = excluded.title, content = excluded.content, "
                + "status = excluded.status, created_at = excluded.created_at, submitted_at = excluded.submitted_at "
                + "WHERE essays.status = 'DRAFT'";
        inTransaction(() -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Essay essay : eligible) {
                    statement.setLong(1, essay.getId());
                    bindEssay(statement, essay, true);
                    statement.addBatch();
                }
                if (!eligible.isEmpty()) {
                    statement.executeBatch();
                }
            }
        });
    }

    private List<Essay> findMany(String sql, StatementBinder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                List<Essay> essays = new ArrayList<>();
                while (result.next()) {
                    essays.add(mapEssay(result));
                }
                return essays;
            }
        }
    }

    private static void bindEssay(PreparedStatement statement, Essay essay, boolean hasId)
            throws SQLException {
        int offset = hasId ? 1 : 0;
        statement.setLong(offset + 1, essay.getStudentId());
        statement.setLong(offset + 2, essay.getTaskId());
        statement.setString(offset + 3, essay.getTitle());
        statement.setString(offset + 4, essay.getContent());
        statement.setString(offset + 5, essay.getStatus().name());
        statement.setString(offset + 6, essay.getCreatedAt().toString());
        if (essay.getSubmittedAt() == null) {
            statement.setNull(offset + 7, java.sql.Types.VARCHAR);
        } else {
            statement.setString(offset + 7, essay.getSubmittedAt().toString());
        }
    }

    private static Essay mapEssay(ResultSet result) throws SQLException {
        EssayStatus status;
        try {
            status = EssayStatus.valueOf(result.getString("status"));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new SQLException("Database contains an unsupported essay status", exception);
        }
        Essay essay = new Essay(result.getLong("id"), result.getLong("student_id"),
                result.getLong("task_id"), result.getString("title"), result.getString("content"),
                status, parseDateTime(result.getString("created_at")),
                parseDateTime(result.getString("submitted_at")));
        essay.setStudentName(result.getString("student_name"));
        String taskType = result.getString("task_type");
        if (taskType != null) {
            try {
                essay.setTaskType(com.ieltsmate.entity.TaskType.valueOf(taskType));
            } catch (IllegalArgumentException exception) {
                throw new SQLException("Database contains an unsupported writing task type", exception);
            }
        }
        return essay;
    }

    private static LocalDateTime parseDateTime(String value) throws SQLException {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (java.time.format.DateTimeParseException exception) {
            throw new SQLException("Database contains an invalid date/time value", exception);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}