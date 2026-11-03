package com.ieltsmate.entity;

import java.io.Serial;
import java.io.Serializable;

public class ChecklistResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long id;
    private long essayId;
    private String sectionName;
    private String criterion;
    private ChecklistAnswer answer;

    public ChecklistResponse() {
    }

    public ChecklistResponse(long id, long essayId, String sectionName, String criterion,
                             ChecklistAnswer answer) {
        this.id = id;
        this.essayId = essayId;
        this.sectionName = sectionName;
        this.criterion = criterion;
        this.answer = answer;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getEssayId() {
        return essayId;
    }

    public void setEssayId(long essayId) {
        this.essayId = essayId;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getCriterion() {
        return criterion;
    }

    public void setCriterion(String criterion) {
        this.criterion = criterion;
    }

    public ChecklistAnswer getAnswer() {
        return answer;
    }

    public void setAnswer(ChecklistAnswer answer) {
        this.answer = answer;
    }
}
