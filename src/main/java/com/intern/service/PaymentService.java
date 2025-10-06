package com.intern.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.intern.entity.LoanApplication;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    public String disburseFunds(LoanApplication application) {
        logger.info("Processing payment disbursement for application: {}", application.getId());

        try {
            // Simulate payment processing delay
            Thread.sleep(1000);

            // Generate transaction ID
            String transactionId = "TXN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "-" + UUID.randomUUID().toString().substring(0, 8);

            logger.info("Funds disbursed: ${} to account for {}. Transaction ID: {}",
                    application.getLoanAmount(),
                    application.getApplicantName(),
                    transactionId);

            return transactionId;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        } catch (Exception e) {
            logger.error("Payment processing failed for application: {}", application.getId(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }
}
