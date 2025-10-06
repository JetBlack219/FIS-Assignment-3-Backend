package com.intern.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.intern.entity.ApplicationStatus;
import com.intern.entity.LoanApplication;
import com.intern.service.LoanApplicationService;
import com.intern.service.RiskAssessmentService;

@Component("automatedRiskAssessmentDelegate")
public class AutomatedRiskAssessmentDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(AutomatedRiskAssessmentDelegate.class);

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info("Starting automated risk assessment for process instance: {}",
                execution.getProcessInstanceId());

        try {
            // Get loan application ID from process variables
            Long applicationId = (Long) execution.getVariable("applicationId");

            if (applicationId == null) {
                throw new RuntimeException("Application ID not found in process variables");
            }

            // Get loan application data
            LoanApplication application = loanApplicationService.getApplicationById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Loan application not found: " + applicationId));

            // Update status
            application.setStatus(ApplicationStatus.RISK_ASSESSMENT_IN_PROGRESS);
            loanApplicationService.save(application);

            // Perform risk assessment
            double riskScore = riskAssessmentService.calculateRiskScore(application);
            boolean riskAcceptable = riskAssessmentService.isRiskAcceptable(riskScore);

            // Update application with risk score
            application.setCreditScore((int) riskScore);
            if (riskAcceptable) {
                application.setStatus(ApplicationStatus.RISK_APPROVED);
            } else {
                application.setStatus(ApplicationStatus.RISK_REJECTED);
                String rejectionReason = riskAssessmentService.getRiskRejectionReason(riskScore);
                application.setRejectionReason(rejectionReason);
            }

            loanApplicationService.save(application);

            // Set process variables for gateway decision
            execution.setVariable("riskScore", riskScore);
            execution.setVariable("riskAcceptable", riskAcceptable);
            execution.setVariable("riskAssessmentComplete", true);

            logger.info("Risk assessment completed. Score: {}, Acceptable: {}",
                    riskScore, riskAcceptable);

        } catch (Exception e) {
            logger.error("Error during automated risk assessment: {}", e.getMessage(), e);
            execution.setVariable("riskAssessmentError", e.getMessage());
            throw e;
        }
    }
}