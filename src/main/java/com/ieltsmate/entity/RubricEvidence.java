package com.ieltsmate.entity;

public record RubricEvidence(
        String criterionKey,
        boolean openingCovered,
        boolean bodyCovered,
        boolean conclusionCovered,
        String evidence,
        String rationale) {
}
