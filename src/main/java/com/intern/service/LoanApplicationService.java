package com.intern.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.intern.entity.ApplicationStatus;
import com.intern.entity.LoanApplication;
import com.intern.repo.LoanApplicationRepository;

@Service
public class LoanApplicationService {

    @Autowired
    private LoanApplicationRepository repository;

    @Autowired
    private CamundaProcessService camundaProcessService;

    public LoanApplication submitApplication(LoanApplication application) {
        // Set initial values
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmissionDate(LocalDateTime.now());
        application.setLastUpdated(LocalDateTime.now());

        // Save application first
        LoanApplication saved = repository.save(application);

        // Start Camunda process
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicationId", saved.getId());
        variables.put("applicantName", saved.getApplicantName());
        variables.put("loanAmount", saved.getLoanAmount());
        variables.put("annualIncome", saved.getAnnualIncome());

        String processInstanceId = camundaProcessService.startProcess("loan-application-process", variables);

        // Update application with process instance ID
        saved.setProcessInstanceId(processInstanceId);
        return repository.save(saved);
    }

    public List<LoanApplication> getAllApplications() {
        return repository.findAll();
    }

    public Optional<LoanApplication> getApplicationById(Long id) {
        return repository.findById(id);
    }

    public List<LoanApplication> getApplicationsByStatus(ApplicationStatus status) {
        return repository.findByStatus(status);
    }

    public LoanApplication save(LoanApplication application) {
        application.setLastUpdated(LocalDateTime.now());
        return repository.save(application);
    }

    public void deleteApplication(Long id) {
        repository.deleteById(id);
    }
}