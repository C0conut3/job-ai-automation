package com.jobai.automation.job.web.dto;

import org.springframework.util.StringUtils;

public record JobSearchCriteria(
        String title,
        String city,
        Integer salaryMin,
        Integer salaryMax,
        String workType) {

    public static JobSearchCriteria of(String title, String city, Integer salaryMin, Integer salaryMax, String workType) {
        return new JobSearchCriteria(blankToNull(title), blankToNull(city), salaryMin, salaryMax, blankToNull(workType));
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public boolean isEmpty() {
        return title == null && city == null && salaryMin == null && salaryMax == null && workType == null;
    }
}
