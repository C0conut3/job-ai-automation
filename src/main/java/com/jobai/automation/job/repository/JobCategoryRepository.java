package com.jobai.automation.job.repository;

import com.jobai.automation.job.domain.JobCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<JobCategory> findAllByOrderBySortOrderAscIdAsc();
}
