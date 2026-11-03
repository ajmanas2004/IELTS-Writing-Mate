package com.ieltsmate.dao;

import com.ieltsmate.entity.TaskType;
import com.ieltsmate.entity.WritingTask;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WritingTaskDAO extends BaseDAO<WritingTask> {
    public WritingTaskDAO(java.sql.Connection connection) {
        super(connection);
    }

    @Override
    public Optional<WritingTask> findById(int id) throws SQLException {
        String sql = "SELECT id, task_type, prompt, time_limit FROM writing_tasks WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapTask(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<WritingTask> findAll() throws SQLException {
        String sql = "SELECT id, task_type, prompt, time_limit FROM writing_tasks ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<WritingTask> tasks = new ArrayList<>();
            while (result.next()) {
                tasks.add(mapTask(result));
            }
            return tasks;
        }
    }

    public List<WritingTask> findByType(TaskType taskType) throws SQLException {
        String sql = "SELECT id, task_type, prompt, time_limit FROM writing_tasks WHERE task_type = ? ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, taskType.name());
            try (ResultSet result = statement.executeQuery()) {
                List<WritingTask> tasks = new ArrayList<>();
                while (result.next()) {
                    tasks.add(mapTask(result));
                }
                return tasks;
            }
        }
    }

    @Override
    public void save(WritingTask task) throws SQLException {
        String sql = "INSERT INTO writing_tasks (task_type, prompt, time_limit) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindTask(statement, task);
            statement.executeUpdate();
            task.setId(generatedId(statement));
        }
    }

    @Override
    public void update(WritingTask task) throws SQLException {
        String sql = "UPDATE writing_tasks SET task_type = ?, prompt = ?, time_limit = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindTask(statement, task);
            statement.setLong(4, task.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM writing_tasks WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private static void bindTask(PreparedStatement statement, WritingTask task) throws SQLException {
        statement.setString(1, task.getTaskType().name());
        statement.setString(2, task.getPrompt());
        if (task.getTimeLimit() == null) {
            statement.setNull(3, java.sql.Types.INTEGER);
        } else {
            statement.setInt(3, task.getTimeLimit());
        }
    }

    private static WritingTask mapTask(ResultSet result) throws SQLException {
        String type = result.getString("task_type");
        try {
            int timeLimit = result.getInt("time_limit");
            return new WritingTask(result.getLong("id"), TaskType.valueOf(type),
                    result.getString("prompt"), result.wasNull() ? null : timeLimit);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new SQLException("Database contains an unsupported writing task type", exception);
        }
    }
}
