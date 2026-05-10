package com.jobai.automation.resume.service.impl;

import com.jobai.automation.resume.domain.Resume;
import com.jobai.automation.resume.repository.ResumeRepository;
import com.jobai.automation.resume.service.ResumeService;
import com.jobai.automation.resume.web.dto.ResumeResponse;
import com.jobai.automation.resume.web.dto.ResumeWriteRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;

    public ResumeServiceImpl(ResumeRepository resumeRepository) {
        this.resumeRepository = resumeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getMyResume(Long userId) {
        return resumeRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public ResumeResponse saveMyResume(Long userId, ResumeWriteRequest request) {
        Resume resume = resumeRepository.findByUserId(userId).orElseGet(() -> {
            Resume r = new Resume();
            r.setUserId(userId);
            return r;
        });

        resume.setName(request.name());
        resume.setPersonalInfo(request.personalInfo());
        resume.setEducation(request.education());
        resume.setJobStatus(request.jobStatus());
        resume.setWorkExperience(request.workExperience());
        resume.setProjectExperience(request.projectExperience());

        Resume saved = resumeRepository.save(resume);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> listAllResumes() {
        return resumeRepository.findAll().stream().map(this::toResponse).toList();
    }

    private ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getUserId(),
                resume.getName(),
                resume.getPersonalInfo(),
                resume.getEducation(),
                resume.getJobStatus(),
                resume.getWorkExperience(),
                resume.getProjectExperience(),
                resume.getCreatedAt(),
                resume.getUpdatedAt());
    }
}
