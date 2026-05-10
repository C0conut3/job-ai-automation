package com.jobai.automation.preference.repository;

import com.jobai.automation.job.domain.WorkType;
import com.jobai.automation.preference.domain.JobPreference;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPreferenceRepository extends JpaRepository<JobPreference, Long> {

    Optional<JobPreference> findBySeekerUserIdAndKeywordAndCityAndSalaryMinAndSalaryMaxAndWorkType(
            Long seekerUserId,
            String keyword,
            String city,
            Integer salaryMin,
            Integer salaryMax,
            WorkType workType);

    List<JobPreference> findBySeekerUserIdOrderByCountDesc(Long seekerUserId);
}
