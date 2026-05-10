package com.jobai.automation.user.repository;

import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.domain.UserRoleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    boolean existsByUserIdAndRole(Long userId, UserRole role);

    List<UserRoleEntity> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
