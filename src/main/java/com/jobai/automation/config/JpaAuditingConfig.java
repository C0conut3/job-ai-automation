package com.jobai.automation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 审计等横切能力入口。
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // TODO: 如需 createdBy / lastModifiedBy，在此注册 AuditorAware<?> Bean
}
