package com.intern.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Entity
@Table(name = "loan_applications")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Applicant name is required")
    private String applicantName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Loan amount is required")
    @Positive(message = "Loan amount must be positive")
    private BigDecimal loanAmount;

    @NotNull(message = "Annual income is required")
    @Positive(message = "Annual income must be positive")
    private BigDecimal annualIncome;

    @NotBlank(message = "Employment status is required")
    private String employmentStatus;

    private Integer creditScore;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private String processInstanceId;

    private String missingDocuments;

    private String rejectionReason;

    private boolean agreementSigned = false;

    private LocalDateTime submissionDate;

    private LocalDateTime lastUpdated;

    // Constructor
    public LoanApplication() {
        this.submissionDate = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.status = ApplicationStatus.SUBMITTED;
    }

    public void setFundsDisbursed(boolean b) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setFundsDisbursed'");
    }
}