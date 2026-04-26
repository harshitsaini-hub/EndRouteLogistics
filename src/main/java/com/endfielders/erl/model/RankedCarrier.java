package com.endfielders.erl.model;

import java.util.List;

public class RankedCarrier extends Carrier {

    private double score;
    private int riskScore;
    private String grade;
    private String aiInsight;
    private int confidenceScore;
    private String explanation;

    private List<String> aiReasons;

    public List<String> getAiReasons() {
        return aiReasons;
    }

    public void setAiReasons(List<String> aiReasons) {
        this.aiReasons = aiReasons;
    }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getAiInsight() { return aiInsight; }
    public void setAiInsight(String aiInsight) { this.aiInsight = aiInsight; }

    public int getConfidenceScore() {
    return confidenceScore;
    }

    public void setConfidenceScore(int confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}