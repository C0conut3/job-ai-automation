package com.jobai.automation.applicationrecord.repository;

import com.jobai.automation.applicationrecord.domain.JobApplicationRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRecordRepository extends JpaRepository<JobApplicationRecord, Long> {

    boolean existsByJob_IdAndSeekerUserId(Long jobId, Long seekerUserId);

    java.util.Optional<JobApplicationRecord> findByJob_IdAndSeekerUserId(Long jobId, Long seekerUserId);

    List<JobApplicationRecord> findBySeekerUserIdOrderByUpdatedAtDesc(Long seekerUserId);
    
    List<JobApplicationRecord> findByJob_RecruiterUserIdOrderByUpdatedAtDesc(Long recruiterUserId);

    void deleteBySeekerUserId(Long seekerUserId);

    void deleteByJob_Id(Long jobId);
}
