package com.jobai.automation.applicationrecord.service;

import com.jobai.automation.applicationrecord.web.dto.ApplicationRecordDto;
import java.util.List;

public interface ApplicationRecordService {
    List<ApplicationRecordDto> getSeekerRecords(Long seekerUserId);
    List<ApplicationRecordDto> getRecruiterRecords(Long recruiterUserId);
}
