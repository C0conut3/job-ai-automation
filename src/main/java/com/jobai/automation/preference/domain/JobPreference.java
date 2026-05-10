package com.jobai.automation.preference.domain;

import com.jobai.automation.job.domain.WorkType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "job_preferences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_preferences_search_signature", columnNames = {
                "seeker_user_id", "keyword", "city", "salary_min", "salary_max", "work_type"
        })
})
public class JobPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seeker_user_id", nullable = false)
    private Long seekerUserId;

    @Column(name = "keyword", length = 255)
    private String keyword;

    @Column(name = "city", length = 128)
    private String city;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", length = 32)
    private WorkType workType;

    @Column(name = "preference_count", nullable = false)
    private Long count = 0L;

    public Long getId() {
        return id;
    }

    public Long getSeekerUserId() {
        return seekerUserId;
    }

    public void setSeekerUserId(Long seekerUserId) {
        this.seekerUserId = seekerUserId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(Integer salaryMin) {
        this.salaryMin = salaryMin;
    }

    public Integer getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(Integer salaryMax) {
        this.salaryMax = salaryMax;
    }

    public WorkType getWorkType() {
        return workType;
    }

    public void setWorkType(WorkType workType) {
        this.workType = workType;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
