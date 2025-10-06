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
import com.intern.service.PaymentService;

@Component("disburseFundsDelegate")
public class DisburseFundsDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(DisburseFundsDelegate.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info("Starting funds disbursement for process instance: {}",
                execution.getProcessInstanceId());

        try {
            Long applicationId = (Long) execution.getVariable("applicationId");

            if (applicationId == null) {
                throw new RuntimeException("Application ID not found in process variables");
            }

            LoanApplication application = loanApplicationService.getApplicationById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Loan application not found: " + applicationId));

            // Verify agreement is signed
            if (!Boolean.TRUE.equals(application.isAgreementSigned())) {
                throw new RuntimeException("Cannot disburse funds: loan agreement not signed");
            }

            // Process payment
            String transactionId = paymentService.disburseFunds(application);

            // Update application status
            application.setStatus(ApplicationStatus.FUNDS_DISBURSED);
            application.setFundsDisbursed(true);
            loanApplicationService.save(application);

            // Send confirmation notification
            notificationService.sendDisbursementNotification(application, transactionId);

            // Set process variables
            execution.setVariable("transactionId", transactionId);
            execution.setVariable("fundsDisbursed", true);
            execution.setVariable("disbursementDate", java.time.LocalDateTime.now().toString());
            execution.setVariable("finalStatus", "FUNDS_DISBURSED");

            logger.info("Funds disbursed successfully for application: {}. Transaction ID: {}",
                    applicationId, transactionId);

        } catch (Exception e) {
            logger.error("Error during funds disbursement: {}", e.getMessage(), e);
            execution.setVariable("disbursementError", e.getMessage());
            throw e;
        }
    }
}
