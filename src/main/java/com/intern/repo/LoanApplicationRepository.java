package com.intern.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intern.entity.ApplicationStatus;
import com.intern.entity.LoanApplication;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByStatus(ApplicationStatus status);

    List<LoanApplication> findByApplicantNameContainingIgnoreCase(String applicantName);

    List<LoanApplication> findByProcessInstanceId(String processInstanceId);
}