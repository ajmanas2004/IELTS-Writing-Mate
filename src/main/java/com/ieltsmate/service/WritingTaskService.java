package com.ieltsmate.service;

import com.ieltsmate.dao.WritingTaskDAO;
import com.ieltsmate.entity.TaskType;
import com.ieltsmate.entity.WritingTask;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WritingTaskService {
    private final WritingTaskDAO writingTaskDAO;

    public WritingTaskService(WritingTaskDAO writingTaskDAO) {
        this.writingTaskDAO = Objects.requireNonNull(writingTaskDAO, "writingTaskDAO must not be null");
    }

    public List<WritingTask> getAllTasks() throws SQLException {
        return writingTaskDAO.findAll();
    }

    public Optional<WritingTask> getTaskById(int id) throws SQLException {
        return writingTaskDAO.findById(id);
    }

    public List<WritingTask> getTasksByType(TaskType type) throws SQLException {
        return writingTaskDAO.findByType(Objects.requireNonNull(type, "type must not be null"));
    }
}
