package com.jobai.automation.resume.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ResumeParseService {

    private final Tika tika = new Tika();

    public String parseResume(MultipartFile file) throws IOException {
        try {
            return tika.parseToString(file.getInputStream());
        } catch (TikaException e) {
            throw new RuntimeException("Failed to parse resume file", e);
        }
    }
}