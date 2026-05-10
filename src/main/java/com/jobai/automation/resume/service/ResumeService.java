package com.jobai.automation.resume.service;

import com.jobai.automation.resume.web.dto.ResumeResponse;
import com.jobai.automation.resume.web.dto.ResumeWriteRequest;
import java.util.List;

public interface ResumeService {
    ResumeResponse getMyResume(Long userId);
    ResumeResponse saveMyResume(Long userId, ResumeWriteRequest request);
    List<ResumeResponse> listAllResumes();
}
