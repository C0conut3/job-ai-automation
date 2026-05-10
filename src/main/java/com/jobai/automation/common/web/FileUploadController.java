package com.jobai.automation.common.web;

import com.jobai.automation.common.api.ApiResponse;
import com.jobai.automation.common.exception.ApiException;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.support.AuthSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    private final Path uploadDir;

    public FileUploadController() throws IOException {
        this.uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    @PostMapping("/upload")
    public ApiResponse<String> uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER, UserRole.RECRUITER);

        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "文件为空");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFilename.contains("..")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "文件名不合法");
        }

        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            extension = originalFilename.substring(i);
        }

        String newFilename = UUID.randomUUID().toString() + extension;
        try {
            Path targetLocation = this.uploadDir.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            String fileUrl = "/uploads/" + newFilename;
            return ApiResponse.ok(fileUrl);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "无法存储文件");
        }
    }
}