package com.jobai.automation.applicationrecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.jobai.automation.job.domain.Job;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "job_application_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_jar_job_seeker", columnNames = {"job_id", "seeker_user_id"}))
@EntityListeners(AuditingEntityListener.class)
public class JobApplicationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "seeker_user_id", nullable = false)
    private Long seekerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobApplicationStatus status = JobApplicationStatus.SUBMITTED;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt = Instant.now();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
