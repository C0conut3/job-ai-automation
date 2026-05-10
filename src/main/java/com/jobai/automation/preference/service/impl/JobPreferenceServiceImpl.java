package com.jobai.automation.preference.service.impl;

import com.jobai.automation.job.domain.Job;
import com.jobai.automation.job.domain.WorkType;
import com.jobai.automation.job.web.dto.JobSearchCriteria;
import com.jobai.automation.preference.domain.JobPreference;
import com.jobai.automation.preference.repository.JobPreferenceRepository;
import com.jobai.automation.preference.service.JobPreferenceService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class JobPreferenceServiceImpl implements JobPreferenceService {

    private final JobPreferenceRepository jobPreferenceRepository;

    public JobPreferenceServiceImpl(JobPreferenceRepository jobPreferenceRepository) {
        this.jobPreferenceRepository = jobPreferenceRepository;
    }

    @Override
    public void recordSearchPreference(Long seekerUserId, JobSearchCriteria criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return;
        }
        WorkType workType = parseWorkType(criteria.workType());
        JobPreference preference = jobPreferenceRepository
                .findBySeekerUserIdAndKeywordAndCityAndSalaryMinAndSalaryMaxAndWorkType(
                        seekerUserId,
                        criteria.title(),
                        criteria.city(),
                        criteria.salaryMin(),
                        criteria.salaryMax(),
                        workType)
                .orElseGet(JobPreference::new);
        if (preference.getId() == null) {
            preference.setSeekerUserId(seekerUserId);
            preference.setKeyword(criteria.title());
            preference.setCity(criteria.city());
            preference.setSalaryMin(criteria.salaryMin());
            preference.setSalaryMax(criteria.salaryMax());
            preference.setWorkType(workType);
            preference.setCount(0L);
        }
        preference.setCount(preference.getCount() + 1);
        jobPreferenceRepository.save(preference);
    }

    @Override
    public List<JobPreference> listPreferences(Long seekerUserId) {
        return jobPreferenceRepository.findBySeekerUserIdOrderByCountDesc(seekerUserId);
    }

    @Override
    public double calculatePreferenceScore(Job job, List<JobPreference> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return 0D;
        }
        double score = 0D;
        for (JobPreference pref : preferences) {
            int matchCount = 0;
            int fieldCount = 0;
            if (pref.getKeyword() != null) {
                fieldCount++;
                if (job.getTitle() != null && job.getTitle().toLowerCase(Locale.ROOT).contains(pref.getKeyword().toLowerCase(Locale.ROOT))) {
                    matchCount++;
                }
            }
            if (pref.getCity() != null) {
                fieldCount++;
                if (job.getCity() != null && job.getCity().toLowerCase(Locale.ROOT).contains(pref.getCity().toLowerCase(Locale.ROOT))) {
                    matchCount++;
                }
            }
            if (pref.getSalaryMin() != null) {
                fieldCount++;
                if (job.getSalaryMax() != null && job.getSalaryMax() >= pref.getSalaryMin()) {
                    matchCount++;
                }
            }
            if (pref.getSalaryMax() != null) {
                fieldCount++;
                if (job.getSalaryMin() != null && job.getSalaryMin() <= pref.getSalaryMax()) {
                    matchCount++;
                }
            }
            if (pref.getWorkType() != null) {
                fieldCount++;
                if (job.getWorkType() == pref.getWorkType()) {
                    matchCount++;
                }
            }
            if (fieldCount > 0) {
                score += (double) matchCount / fieldCount * pref.getCount();
            }
        }
        return score;
    }

    private WorkType parseWorkType(String raw) {
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
