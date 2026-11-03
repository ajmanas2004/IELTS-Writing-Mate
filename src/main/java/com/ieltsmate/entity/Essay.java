package com.ieltsmate.entity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class Essay implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long id;
    private long studentId;
    private long taskId;
    private String title;
    private String content;
    private EssayStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private String studentName;
    private TaskType taskType;

    public Essay() {
    }

    public Essay(long id, long studentId, long taskId, String title, String content,
                 EssayStatus status, LocalDateTime createdAt, LocalDateTime submittedAt) {
        this.id = id;
        this.studentId = studentId;
        this.taskId = taskId;
        this.title = title;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
        this.submittedAt = submittedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getStudentId() {
        return studentId;
    }

    public void setStudentId(long studentId) {
        this.studentId = studentId;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public EssayStatus getStatus() {
        return status;
    }

    public void setStatus(EssayStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
}
