package com.intern.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.intern.entity.LoanApplication;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void sendRejectionNotification(LoanApplication application, String reason) {
        logger.info("Sending rejection notification to: {}", application.getEmail());

        String message = String.format(
                "Dear %s,\n\n" +
                        "We regret to inform you that your loan application #%d has been declined.\n\n" +
                        "Reason: %s\n\n" +
                        "Thank you for your interest in our services.\n\n" +
                        "Best regards,\n" +
                        "Loan Processing Team",
                application.getApplicantName(),
                application.getId(),
                reason);

        simulateEmailSending(application.getEmail(), "Loan Application Declined", message);
    }

    public void sendDisbursementNotification(LoanApplication application, String transactionId) {
        logger.info("Sending disbursement notification to: {}", application.getEmail());

        String message = String.format(
                "Dear %s,\n\n" +
                        "Your loan funds have been successfully disbursed.\n\n" +
                        "Loan Amount: $%s\n" +
                        "Transaction ID: %s\n" +
                        "Date: %s\n\n" +
                        "The funds should appear in your account within 1-2 business days.\n\n" +
                        "Best regards,\n" +
                        "Loan Processing Team",
                application.getApplicantName(),
                application.getLoanAmount(),
                transactionId,
                java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        simulateEmailSending(application.getEmail(), "Loan Funds Disbursed", message);
    }

    private void simulateEmailSending(String email, String subject, String message) {
        try {
            Thread.sleep(500);
            logger.info("Email sent to: {} | Subject: {}", email, subject);
            logger.debug("Email content: {}", message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Email sending interrupted", e);
        }
    }
}
