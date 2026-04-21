package com.endfielders.erl.model;

public class RankedCarrier extends Carrier {

    private double score;
    private int riskScore;
    private String grade;
    private String aiInsight;

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getAiInsight() { return aiInsight; }
    public void setAiInsight(String aiInsight) { this.aiInsight = aiInsight; }
}