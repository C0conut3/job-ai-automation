package com.jobai.automation.resume.web;

import com.jobai.automation.common.api.ApiResponse;
import com.jobai.automation.resume.service.AiResumeService;
import com.jobai.automation.resume.service.ResumeParseService;
import com.jobai.automation.resume.service.ResumeService;
import com.jobai.automation.resume.web.dto.OptimizeRequest;
import com.jobai.automation.resume.web.dto.ParsedResumeDto;
import com.jobai.automation.resume.web.dto.ResumeResponse;
import com.jobai.automation.resume.web.dto.ResumeWriteRequest;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.support.AuthSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeParseService resumeParseService;
    private final AiResumeService aiResumeService;

    public ResumeController(ResumeService resumeService, ResumeParseService resumeParseService, AiResumeService aiResumeService) {
        this.resumeService = resumeService;
        this.resumeParseService = resumeParseService;
        this.aiResumeService = aiResumeService;
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("resume-module");
    }

    @GetMapping("/me")
    public ApiResponse<ResumeResponse> getMyResume(HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        return ApiResponse.ok(resumeService.getMyResume(userId));
    }

    @PostMapping("/me")
    public ApiResponse<ResumeResponse> saveMyResume(
            @Valid @RequestBody ResumeWriteRequest body, HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        return ApiResponse.ok(resumeService.saveMyResume(userId, body));
    }

    @GetMapping("/list-for-recruiter")
    public ApiResponse<List<ResumeResponse>> listForRecruiter(HttpServletRequest request) {
        AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.RECRUITER);
        return ApiResponse.ok(resumeService.listAllResumes());
    }

    @PostMapping("/parse")
    public ApiResponse<ParsedResumeDto> parseResume(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        String parsedContent = resumeParseService.parseResume(file);
        ParsedResumeDto categorized = aiResumeService.parseAndCategorizeResume(parsedContent);
        return ApiResponse.ok(categorized);
    }

    @PostMapping("/optimize")
    public ApiResponse<ParsedResumeDto> optimizeResume(@RequestBody OptimizeRequest body, HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        String model = body.model();
        ParsedResumeDto optimized = (model == null || model.isBlank())
                ? aiResumeService.optimizeAndCategorizeResume(body.content())
                : aiResumeService.optimizeAndCategorizeResume(body.content(), model);
        return ApiResponse.ok(optimized);
    }
}
