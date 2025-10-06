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
import com.intern.service.NotificationService;

@Component("notifyRejectDelegate")
public class NotifyRejectDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(NotifyRejectDelegate.class);

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private LoanApplicationService loanApplicationService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info("Starting rejection notification for process instance: {}", 
                   execution.getProcessInstanceId());
        
        try {
            Long applicationId = (Long) execution.getVariable("applicationId");
            
            if (applicationId == null) {
                throw new RuntimeException("Application ID not found in process variables");
            }
            
            LoanApplication application = loanApplicationService.getApplicationById(applicationId)
                .orElseThrow(() -> new RuntimeException("Loan application not found: " + applicationId));
            
            // Get rejection reason from process variables or application
            String rejectionReason = (String) execution.getVariable("rejectionReason");
            if (rejectionReason == null) {
                rejectionReason = application.getRejectionReason();
            }
            if (rejectionReason == null) {
                rejectionReason = "Application did not meet approval criteria";
            }
            
            // Update application status
            application.setStatus(ApplicationStatus.REJECTED);
            application.setRejectionReason(rejectionReason);
            loanApplicationService.save(application);
            
            // Send rejection notification
            notificationService.sendRejectionNotification(application, rejectionReason);
            
            // Set process variables
            execution.setVariable("notificationSent", true);
            execution.setVariable("finalStatus", "REJECTED");
            
            logger.info("Rejection notification sent for application: {}", applicationId);
            
        } catch (Exception e) {
            logger.error("Error during rejection notification: {}", e.getMessage(), e);
            execution.setVariable("notificationError", e.getMessage());
            throw e;
        }
    }

}
