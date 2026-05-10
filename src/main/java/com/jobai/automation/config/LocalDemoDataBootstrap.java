package com.jobai.automation.config;

import com.jobai.automation.job.domain.JobCategory;
import com.jobai.automation.job.repository.JobCategoryRepository;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.domain.User;
import com.jobai.automation.user.domain.UserRoleEntity;
import com.jobai.automation.user.repository.UserRepository;
import com.jobai.automation.user.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 仅在 {@code local} profile 下写入演示管理员与职位类别，便于联调（生产请勿启用此 profile）。
 */
@Component
@Profile("local")
public class LocalDemoDataBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDemoDataBootstrap.class);

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JobCategoryRepository jobCategoryRepository;

    public LocalDemoDataBootstrap(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            JobCategoryRepository jobCategoryRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jobCategoryRepository = jobCategoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdminIfAbsent();
        seedCategoriesIfEmpty();
    }

    private void seedAdminIfAbsent() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setNickname("演示管理员");
        userRepository.save(admin);

        for (UserRole role : UserRole.values()) {
            userRoleRepository.save(new UserRoleEntity(admin.getId(), role));
        }
        log.warn("已创建演示管理员: 用户名 admin，密码 admin123（仅 local profile）");
    }

    private void seedCategoriesIfEmpty() {
        if (jobCategoryRepository.count() > 0) {
            return;
        }
        JobCategory dev = new JobCategory();
        dev.setName("软件开发");
        dev.setDescription("后端、前端、全栈等");
        dev.setSortOrder(10);
        jobCategoryRepository.save(dev);

        JobCategory pm = new JobCategory();
        pm.setName("产品运营");
        pm.setDescription("产品、运营、市场等");
        pm.setSortOrder(20);
        jobCategoryRepository.save(pm);
    }
}
