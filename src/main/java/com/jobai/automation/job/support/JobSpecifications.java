package com.jobai.automation.job.support;

import com.jobai.automation.job.domain.Job;
import com.jobai.automation.job.domain.JobStatus;
import com.jobai.automation.job.domain.WorkType;
import com.jobai.automation.job.web.dto.JobSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class JobSpecifications {

    private JobSpecifications() {}

    public static Specification<Job> bySearchCriteria(JobSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), JobStatus.PUBLISHED));

            if (criteria.title() != null) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + criteria.title().toLowerCase(Locale.ROOT) + "%"));
            }
            if (criteria.city() != null) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + criteria.city().toLowerCase(Locale.ROOT) + "%"));
            }
            if (criteria.salaryMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salaryMax"), criteria.salaryMin()));
            }
            if (criteria.salaryMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salaryMin"), criteria.salaryMax()));
            }
            if (criteria.workType() != null) {
                WorkType workType = parseWorkType(criteria.workType());
                if (workType != null) {
                    predicates.add(cb.equal(root.get("workType"), workType));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static WorkType parseWorkType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return WorkType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
