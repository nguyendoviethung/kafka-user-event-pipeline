package com.example.serverb.repository;

import com.example.serverb.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    List<EventLog> findByUserId(Long userId);
    long countByEventType(String eventType);
}
