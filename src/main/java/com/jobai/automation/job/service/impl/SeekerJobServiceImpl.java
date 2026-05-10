package com.jobai.automation.job.service.impl;

import com.jobai.automation.applicationrecord.domain.JobApplicationRecord;
import com.jobai.automation.applicationrecord.domain.JobApplicationStatus;
import com.jobai.automation.applicationrecord.repository.JobApplicationRecordRepository;
import com.jobai.automation.chat.service.ChatService;
import com.jobai.automation.common.api.PagedResponse;
import com.jobai.automation.common.exception.ApiException;
import com.jobai.automation.job.domain.Job;
import com.jobai.automation.job.domain.JobStatus;
import com.jobai.automation.preference.service.JobPreferenceService;
import com.jobai.automation.job.repository.JobRepository;
import com.jobai.automation.job.service.SeekerJobService;
import com.jobai.automation.job.support.JobMappers;
import com.jobai.automation.job.support.JobSpecifications;
import com.jobai.automation.job.web.dto.JobDetailResponse;
import com.jobai.automation.job.web.dto.JobSearchCriteria;
import com.jobai.automation.job.web.dto.JobSummaryResponse;
import com.jobai.automation.resume.repository.ResumeRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeekerJobServiceImpl implements SeekerJobService {

    private final JobRepository jobRepository;
    private final JobApplicationRecordRepository jobApplicationRecordRepository;
    private final ChatService chatService;
    private final ResumeRepository resumeRepository;
    private final JobPreferenceService jobPreferenceService;

    public SeekerJobServiceImpl(JobRepository jobRepository,
                                JobApplicationRecordRepository jobApplicationRecordRepository,
                                ChatService chatService,
                                ResumeRepository resumeRepository,
                                JobPreferenceService jobPreferenceService) {
        this.jobRepository = jobRepository;
        this.jobApplicationRecordRepository = jobApplicationRecordRepository;
        this.chatService = chatService;
        this.resumeRepository = resumeRepository;
        this.jobPreferenceService = jobPreferenceService;
    }

    @Override
    @Transactional
    public PagedResponse<JobSummaryResponse> listPublished(Pageable pageable, JobSearchCriteria criteria, Long seekerUserId) {
        if (!criteria.isEmpty()) {
            jobPreferenceService.recordSearchPreference(seekerUserId, criteria);
            Page<Job> page = jobRepository.findAll(JobSpecifications.bySearchCriteria(criteria), pageable);
            return PagedResponse.of(page.map(JobMappers::toSummary));
        }

        Page<Job> page = jobRepository.findByStatus(JobStatus.PUBLISHED, pageable);
        List<com.jobai.automation.preference.domain.JobPreference> preferences = jobPreferenceService.listPreferences(seekerUserId);
        if (preferences.isEmpty()) {
            return PagedResponse.of(page.map(JobMappers::toSummary));
        }

        List<JobSummaryResponse> sortedSummaries = page.stream()
                .sorted(Comparator.comparingDouble((Job job) -> -jobPreferenceService.calculatePreferenceScore(job, preferences)))
                .map(JobMappers::toSummary)
                .collect(Collectors.toList());
        Page<JobSummaryResponse> sortedPage = new PageImpl<>(sortedSummaries, pageable, page.getTotalElements());
        return PagedResponse.of(sortedPage);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailResponse getPublished(Long jobId) {
        Job job = jobRepository.findDetailById(jobId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "职位不存在"));
        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.NOT_FOUND, "职位不存在或已下架");
        }
        return JobMappers.toDetail(job);
    }

    @Override
    @Transactional
    public void apply(Long seekerUserId, Long jobId) {
        Job job = jobRepository.findDetailById(jobId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "职位不存在"));
        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "该职位当前不可投递");
        }
        if (job.getRecruiterUserId().equals(seekerUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能投递自己发布的职位");
        }
        JobApplicationRecord existingRecord = jobApplicationRecordRepository.findByJob_IdAndSeekerUserId(jobId, seekerUserId).orElse(null);
        if (existingRecord != null) {
            if (existingRecord.getStatus() != JobApplicationStatus.INVITED) {
                throw new ApiException(HttpStatus.CONFLICT, "你已投递过该职位");
            }
            existingRecord.setStatus(JobApplicationStatus.SUBMITTED);
            existingRecord.setAppliedAt(Instant.now());
            jobApplicationRecordRepository.save(existingRecord);
        } else {
            JobApplicationRecord record = new JobApplicationRecord();
            record.setJob(job);
            record.setSeekerUserId(seekerUserId);
            record.setStatus(JobApplicationStatus.SUBMITTED);
            record.setAppliedAt(Instant.now());
            jobApplicationRecordRepository.save(record);
        }

        String resumeContent = resumeRepository.findByUserId(seekerUserId)
                .map(r -> {
                    StringBuilder sb = new StringBuilder();
                    if (r.getPersonalInfo() != null) sb.append("【个人信息】\n").append(r.getPersonalInfo()).append("\n\n");
                    if (r.getJobStatus() != null) sb.append("【求职状态】\n").append(r.getJobStatus()).append("\n\n");
                    if (r.getEducation() != null) sb.append("【教育经历】\n").append(r.getEducation()).append("\n\n");
                    if (r.getWorkExperience() != null) sb.append("【工作经历】\n").append(r.getWorkExperience()).append("\n\n");
                    if (r.getProjectExperience() != null) sb.append("【项目经历】\n").append(r.getProjectExperience());
                    return sb.toString().trim();
                })
                .filter(s -> !s.isEmpty())
                .orElse("求职者尚未填写详细简历内容。");
        chatService.initiateApplicationChat(seekerUserId, job, resumeContent);
    }
}
