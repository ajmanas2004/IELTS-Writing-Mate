package com.ieltsmate.entity;

import java.io.Serial;
import java.io.Serializable;

public class WritingTask implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long id;
    private TaskType taskType;
    private String prompt;
    private Integer timeLimit;

    public WritingTask() {
    }

    public WritingTask(long id, TaskType taskType, String prompt, Integer timeLimit) {
        this.id = id;
        this.taskType = taskType;
        this.prompt = prompt;
        this.timeLimit = timeLimit;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }
}
