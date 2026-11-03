package com.ieltsmate.entity;

import java.io.Serial;
import java.io.Serializable;

public class SelfAssessment implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long id;
    private long essayId;
    private Double estimatedBand;
    private String comment;

    public SelfAssessment() {
    }

    public SelfAssessment(long id, long essayId, Double estimatedBand, String comment) {
        this.id = id;
        this.essayId = essayId;
        this.estimatedBand = estimatedBand;
        this.comment = comment;
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

    public Double getEstimatedBand() {
        return estimatedBand;
    }

    public void setEstimatedBand(Double estimatedBand) {
        this.estimatedBand = estimatedBand;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
