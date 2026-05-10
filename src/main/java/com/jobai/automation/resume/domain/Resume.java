package com.jobai.automation.resume.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

@Getter
@Setter
@Entity
@Table(name = "resumes")
@EntityListeners(AuditingEntityListener.class)
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "personal_info", columnDefinition = "TEXT")
    private String personalInfo;

    @Column(columnDefinition = "TEXT")
    private String education;

    @Column(name = "job_status", length = 64)
    private String jobStatus;

    @Column(name = "work_experience", columnDefinition = "TEXT")
    private String workExperience;

    @Column(name = "project_experience", columnDefinition = "TEXT")
    private String projectExperience;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
