package com.jobai.automation.job.service.impl;

import com.jobai.automation.job.repository.JobRepository;
import com.jobai.automation.job.service.JobService;
import org.springframework.stereotype.Service;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    public JobServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // TODO: 实现职位领域服务
}
