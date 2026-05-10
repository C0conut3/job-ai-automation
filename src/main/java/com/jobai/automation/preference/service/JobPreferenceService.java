package com.jobai.automation.preference.service;

import com.jobai.automation.job.domain.Job;
import com.jobai.automation.job.web.dto.JobSearchCriteria;
import com.jobai.automation.preference.domain.JobPreference;
import java.util.List;

public interface JobPreferenceService {

    void recordSearchPreference(Long seekerUserId, JobSearchCriteria criteria);

    List<JobPreference> listPreferences(Long seekerUserId);

    double calculatePreferenceScore(Job job, List<JobPreference> preferences);
}
