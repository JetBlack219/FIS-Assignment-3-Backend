package com.intern.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intern.entity.ApplicationStatus;
import com.intern.entity.LoanApplication;
import com.intern.service.CamundaProcessService;
import com.intern.service.LoanApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/loan-applications")
@CrossOrigin(origins = "*")
public class LoanApplicationController {

    private static final Logger logger = LoggerFactory.getLogger(LoanApplicationController.class);

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Autowired
    private CamundaProcessService camundaProcessService;

    @PostMapping
    public ResponseEntity<LoanApplication> submitApplication(@Valid @RequestBody LoanApplication application) {
        try {
            LoanApplication saved = loanApplicationService.submitApplication(application);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error submitting application", e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<LoanApplication>> getAllApplications() {
        List<LoanApplication> applications = loanApplicationService.getAllApplications();
        return new ResponseEntity<>(applications, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplication> getApplicationById(@PathVariable Long id) {
        Optional<LoanApplication> application = loanApplicationService.getApplicationById(id);
        return application.map(app -> new ResponseEntity<>(app, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LoanApplication>> getApplicationsByStatus(@PathVariable ApplicationStatus status) {
        List<LoanApplication> applications = loanApplicationService.getApplicationsByStatus(status);
        return new ResponseEntity<>(applications, HttpStatus.OK);
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<Map<String, Object>> reviewApplication(@PathVariable Long id,
            @RequestBody Map<String, Object> reviewData) {
        try {
            Optional<LoanApplication> optApp = loanApplicationService.getApplicationById(id);
            if (optApp.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            LoanApplication app = optApp.get();
            String processInstanceId = app.getProcessInstanceId();

            // Get current task
            List<Task> tasks = camundaProcessService.getTasksForProcess(processInstanceId);
            Task reviewTask = tasks.stream()
                    .filter(task -> "Activity_138c7bf".equals(task.getTaskDefinitionKey()))
                    .findFirst()
                    .orElse(null);

            if (reviewTask != null) {
                Map<String, Object> variables = new HashMap<>();

                // Check if application is complete
                boolean isComplete = isApplicationComplete(app);
                variables.put("applicationComplete", isComplete);

                String missingDocs = null; // Declare outside the if block

                if (!isComplete) {
                    missingDocs = getMissingDocuments(app);
                    variables.put("missingDocuments", missingDocs);
                    app.setMissingDocuments(missingDocs);
                    app.setStatus(ApplicationStatus.MISSING_INFORMATION);
                } else {
                    app.setStatus(ApplicationStatus.UNDER_REVIEW);
                }

                loanApplicationService.save(app);
                camundaProcessService.completeUserTask(reviewTask.getId(), variables);

                Map<String, Object> response = new HashMap<>();
                response.put("applicationId", id);
                response.put("isComplete", isComplete);
                response.put("status", app.getStatus());
                if (!isComplete) {
                    response.put("missingDocuments", missingDocs);
                }

                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error reviewing application", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}/credit-check")
    public ResponseEntity<Map<String, Object>> performCreditCheck(@PathVariable Long id,
            @RequestBody Map<String, Object> creditData) {
        try {
            Optional<LoanApplication> optApp = loanApplicationService.getApplicationById(id);
            if (optApp.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            LoanApplication app = optApp.get();
            String processInstanceId = app.getProcessInstanceId();

            // Get current task
            List<Task> tasks = camundaProcessService.getTasksForProcess(processInstanceId);
            Task creditTask = tasks.stream()
                    .filter(task -> "Activity_1sohyjc".equals(task.getTaskDefinitionKey()))
                    .findFirst()
                    .orElse(null);

            if (creditTask != null) {
                // Simulate credit check
                Integer creditScore = app.getCreditScore();
                if (creditScore == null) {
                    creditScore = (Integer) creditData.getOrDefault("creditScore",
                            (int) (Math.random() * 550) + 300);
                    app.setCreditScore(creditScore);
                }

                boolean creditApproved = creditScore >= 650;

                Map<String, Object> variables = new HashMap<>();
                variables.put("creditScore", creditScore);
                variables.put("creditApproved", creditApproved);

                if (creditApproved) {
                    app.setStatus(ApplicationStatus.CREDIT_APPROVED);
                } else {
                    app.setStatus(ApplicationStatus.CREDIT_REJECTED);
                    String rejectionReason = "Credit score too low: " + creditScore + ". Minimum required: 650";
                    app.setRejectionReason(rejectionReason);
                    variables.put("rejectionReason", rejectionReason);
                }

                loanApplicationService.save(app);
                camundaProcessService.completeUserTask(creditTask.getId(), variables);

                Map<String, Object> response = new HashMap<>();
                response.put("applicationId", id);
                response.put("creditScore", creditScore);
                response.put("creditApproved", creditApproved);
                response.put("status", app.getStatus());

                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error performing credit check", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}/approve-loan")
    public ResponseEntity<Map<String, Object>> approveLoan(@PathVariable Long id) {
        try {
            Optional<LoanApplication> optApp = loanApplicationService.getApplicationById(id);
            if (optApp.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            LoanApplication app = optApp.get();
            String processInstanceId = app.getProcessInstanceId();

            // Get current task
            List<Task> tasks = camundaProcessService.getTasksForProcess(processInstanceId);
            Task approvalTask = tasks.stream()
                    .filter(task -> "Activity_0nyafzx".equals(task.getTaskDefinitionKey()))
                    .findFirst()
                    .orElse(null);

            if (approvalTask != null) {
                app.setStatus(ApplicationStatus.LOAN_APPROVED);
                loanApplicationService.save(app);

                Map<String, Object> variables = new HashMap<>();
                variables.put("loanApproved", true);

                camundaProcessService.completeUserTask(approvalTask.getId(), variables);

                Map<String, Object> response = new HashMap<>();
                response.put("applicationId", id);
                response.put("status", app.getStatus());
                response.put("message", "Loan approved successfully");

                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error approving loan", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}/prepare-agreement")
    public ResponseEntity<Map<String, Object>> prepareLoanAgreement(@PathVariable Long id) {
        try {
            Optional<LoanApplication> optApp = loanApplicationService.getApplicationById(id);
            if (optApp.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            LoanApplication app = optApp.get();
            String processInstanceId = app.getProcessInstanceId();

            // Get current task
            List<Task> tasks = camundaProcessService.getTasksForProcess(processInstanceId);
            Task prepareTask = tasks.stream()
                    .filter(task -> "Activity_1m5wqcw".equals(task.getTaskDefinitionKey()))
                    .findFirst()
                    .orElse(null);

            if (prepareTask != null) {
                app.setStatus(ApplicationStatus.AGREEMENT_PREPARED);
                loanApplicationService.save(app);

                Map<String, Object> variables = new HashMap<>();
                variables.put("agreementPrepared", true);

                camundaProcessService.completeUserTask(prepareTask.getId(), variables);

                Map<String, Object> response = new HashMap<>();
                response.put("applicationId", id);
                response.put("status", app.getStatus());
                response.put("message", "Loan agreement prepared");

                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error preparing agreement", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}/sign-agreement")
    public ResponseEntity<Map<String, Object>> signLoanAgreement(@PathVariable Long id) {
        try {
            Optional<LoanApplication> optApp = loanApplicationService.getApplicationById(id);
            if (optApp.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            LoanApplication app = optApp.get();
            String processInstanceId = app.getProcessInstanceId();

            // Get current task
            List<Task> tasks = camundaProcessService.getTasksForProcess(processInstanceId);
            Task signTask = tasks.stream()
                    .filter(task -> "Activity_0ga4ksm".equals(task.getTaskDefinitionKey()))
                    .findFirst()
                    .orElse(null);

            if (signTask != null) {
                app.setStatus(ApplicationStatus.AGREEMENT_SIGNED);
                app.setAgreementSigned(true);
                loanApplicationService.save(app);

                Map<String, Object> variables = new HashMap<>();
                variables.put("agreementSigned", true);

                camundaProcessService.completeUserTask(signTask.getId(), variables);

                Map<String, Object> response = new HashMap<>();
                response.put("applicationId", id);
                response.put("status", app.getStatus());
                response.put("message", "Loan agreement signed successfully");

                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error signing agreement", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<Task>> getTasksForApplication(@PathVariable Long id) {
        try {
            Optional<LoanApplication> optApp = loanApplicationService.getApplicationById(id);
            if (optApp.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            LoanApplication app = optApp.get();
            String processInstanceId = app.getProcessInstanceId();

            if (processInstanceId != null) {
                List<Task> tasks = camundaProcessService.getTasksForProcess(processInstanceId);
                return new ResponseEntity<>(tasks, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error getting tasks for application", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isApplicationComplete(LoanApplication app) {
        return app.getApplicantName() != null && !app.getApplicantName().trim().isEmpty() &&
                app.getEmail() != null && !app.getEmail().trim().isEmpty() &&
                app.getLoanAmount() != null &&
                app.getAnnualIncome() != null &&
                app.getEmploymentStatus() != null && !app.getEmploymentStatus().trim().isEmpty();
    }

    private String getMissingDocuments(LoanApplication app) {
        StringBuilder missing = new StringBuilder();

        if (app.getApplicantName() == null || app.getApplicantName().trim().isEmpty()) {
            missing.append("Applicant name, ");
        }
        if (app.getEmail() == null || app.getEmail().trim().isEmpty()) {
            missing.append("Email address, ");
        }
        if (app.getLoanAmount() == null) {
            missing.append("Loan amount, ");
        }
        if (app.getAnnualIncome() == null) {
            missing.append("Annual income, ");
        }
        if (app.getEmploymentStatus() == null || app.getEmploymentStatus().trim().isEmpty()) {
            missing.append("Employment status, ");
        }

        String result = missing.toString();
        return result.endsWith(", ") ? result.substring(0, result.length() - 2) : result;
    }
}
