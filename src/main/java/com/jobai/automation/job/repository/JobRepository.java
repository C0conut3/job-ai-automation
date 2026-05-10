package com.jobai.automation.job.repository;

import com.jobai.automation.job.domain.Job;
import com.jobai.automation.job.domain.JobStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    @EntityGraph(attributePaths = {"category"})
    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Job> findByRecruiterUserId(Long recruiterUserId, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    java.util.Optional<Job> findDetailById(Long id);

    List<Job> findAllByRecruiterUserId(Long recruiterUserId);

    @EntityGraph(attributePaths = {"category"})
    @Query("select j from Job j")
    Page<Job> findAllPaged(Pageable pageable);
}
