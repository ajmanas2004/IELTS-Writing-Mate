package com.ieltsmate.entity;

import com.ieltsmate.annotation.RubricCriterion;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class TeacherAssessment implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long id;
    private long essayId;
    private long teacherId;

    @RubricCriterion(
            name = "Task Response",
            task1Name = "Task Achievement",
            description = "Đáp ứng đầy đủ yêu cầu và phát triển nội dung phù hợp với đề bài."
    )
    private double taskScore;

    @RubricCriterion(
            name = "Coherence & Cohesion",
            description = "Tổ chức ý tưởng mạch lạc và sử dụng phương tiện liên kết hiệu quả."
    )
    private double coherenceScore;

    @RubricCriterion(
            name = "Lexical Resource",
            description = "Sử dụng vốn từ vựng đa dạng, chính xác và phù hợp."
    )
    private double lexicalScore;

    @RubricCriterion(
            name = "Grammatical Range & Accuracy",
            description = "Sử dụng đa dạng cấu trúc ngữ pháp với độ chính xác phù hợp."
    )
    private double grammarScore;

    private double overallBand;
    private String feedback;
    private LocalDateTime gradedAt;

    public TeacherAssessment() {
    }

    public TeacherAssessment(long id, long essayId, long teacherId, double taskScore,
                             double coherenceScore, double lexicalScore, double grammarScore,
                             double overallBand, String feedback, LocalDateTime gradedAt) {
        this.id = id;
        this.essayId = essayId;
        this.teacherId = teacherId;
        this.taskScore = taskScore;
        this.coherenceScore = coherenceScore;
        this.lexicalScore = lexicalScore;
        this.grammarScore = grammarScore;
        this.overallBand = overallBand;
        this.feedback = feedback;
        this.gradedAt = gradedAt;
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

    public long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(long teacherId) {
        this.teacherId = teacherId;
    }

    public double getTaskScore() {
        return taskScore;
    }

    public void setTaskScore(double taskScore) {
        this.taskScore = taskScore;
    }

    public double getCoherenceScore() {
        return coherenceScore;
    }

    public void setCoherenceScore(double coherenceScore) {
        this.coherenceScore = coherenceScore;
    }

    public double getLexicalScore() {
        return lexicalScore;
    }

    public void setLexicalScore(double lexicalScore) {
        this.lexicalScore = lexicalScore;
    }

    public double getGrammarScore() {
        return grammarScore;
    }

    public void setGrammarScore(double grammarScore) {
        this.grammarScore = grammarScore;
    }

    public double getOverallBand() {
        return overallBand;
    }

    public void setOverallBand(double overallBand) {
        this.overallBand = overallBand;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }
}
