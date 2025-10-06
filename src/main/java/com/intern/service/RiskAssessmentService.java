package com.intern.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.intern.entity.LoanApplication;

@Service
public class RiskAssessmentService {

    private static final double MAX_RISK_SCORE = 100.0;
    private static final double ACCEPTABLE_THRESHOLD = 50.0;
    private static final BigDecimal HIGH_LOAN_THRESHOLD = new BigDecimal("100000");

    public double calculateRiskScore(LoanApplication application) {
        double riskScore = 0.0;

        // Income-to-loan ratio (loan / income) - using double conversion
        if (application.getAnnualIncome() != null && application.getLoanAmount() != null
                && application.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {

            double loanAmount = application.getLoanAmount().doubleValue();
            double annualIncome = application.getAnnualIncome().doubleValue();
            double incomeRatio = loanAmount / annualIncome;

            riskScore += incomeRatio * 30; // Weight: 30%
        }

        // Credit score impact (lower score → higher risk)
        if (application.getCreditScore() != null) {
            riskScore += (850 - application.getCreditScore()) / 850.0 * 40; // Weight: 40%
        }

        // Employment status impact
        if ("UNEMPLOYED".equalsIgnoreCase(application.getEmploymentStatus())) {
            riskScore += 20; // Weight: 20%
        } else if ("PART_TIME".equalsIgnoreCase(application.getEmploymentStatus())) {
            riskScore += 10; // Weight: 10%
        }

        // Loan amount impact
        if (application.getLoanAmount() != null
                && application.getLoanAmount().compareTo(HIGH_LOAN_THRESHOLD) > 0) {
            riskScore += 10; // Weight: 10%
        }

        return Math.min(MAX_RISK_SCORE, riskScore);
    }

    public boolean isRiskAcceptable(double riskScore) {
        return riskScore <= ACCEPTABLE_THRESHOLD;
    }

    public String getRiskRejectionReason(double riskScore) {
        if (!isRiskAcceptable(riskScore)) {
            return "Risk score too high: " + String.format("%.2f", riskScore)
                    + ". Maximum acceptable: " + ACCEPTABLE_THRESHOLD;
        }
        return null;
    }
}