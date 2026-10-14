package com.TutorIn.repository;

import com.TutorIn.entity.SystemAuditLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemAuditLogsRepository extends JpaRepository<SystemAuditLogs, UUID> {
    List<SystemAuditLogs> findTop50ByOrderByTimestampDesc();
}
