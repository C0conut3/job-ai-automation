package com.jobai.automation.applicationrecord.service.impl;

import com.jobai.automation.applicationrecord.domain.JobApplicationRecord;
import com.jobai.automation.applicationrecord.repository.JobApplicationRecordRepository;
import com.jobai.automation.applicationrecord.service.ApplicationRecordService;
import com.jobai.automation.applicationrecord.web.dto.ApplicationRecordDto;
import com.jobai.automation.user.domain.User;
import com.jobai.automation.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationRecordServiceImpl implements ApplicationRecordService {

    private final JobApplicationRecordRepository jobApplicationRecordRepository;
    private final UserRepository userRepository;

    public ApplicationRecordServiceImpl(JobApplicationRecordRepository jobApplicationRecordRepository, UserRepository userRepository) {
        this.jobApplicationRecordRepository = jobApplicationRecordRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationRecordDto> getSeekerRecords(Long seekerUserId) {
        return jobApplicationRecordRepository.findBySeekerUserIdOrderByUpdatedAtDesc(seekerUserId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationRecordDto> getRecruiterRecords(Long recruiterUserId) {
        return jobApplicationRecordRepository.findByJob_RecruiterUserIdOrderByUpdatedAtDesc(recruiterUserId)
                .stream().map(this::toDto).toList();
    }

    private ApplicationRecordDto toDto(JobApplicationRecord record) {
        String seekerName = userRepository.findById(record.getSeekerUserId())
                .map(User::getNickname)
                .orElse("未知求职者");

        return new ApplicationRecordDto(
                record.getId(),
                record.getJob().getId(),
                record.getJob().getTitle(),
                record.getJob().getCompanyName(),
                record.getSeekerUserId(),
                seekerName,
                record.getStatus().name(),
                record.getAppliedAt(),
                record.getUpdatedAt()
        );
    }
}
