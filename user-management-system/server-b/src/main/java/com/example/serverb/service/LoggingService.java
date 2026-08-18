package com.example.serverb.service;

import com.example.serverb.entity.EventLog;
import com.example.serverb.event.UserEvent;
import com.example.serverb.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Ghi lại toàn bộ event nhận được từ Kafka vào database riêng của Server B
 * (phục vụ audit trail / truy vết sau này).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingService {

    private final EventLogRepository eventLogRepository;

    public void logEvent(UserEvent event, int partition, long offset) {
        EventLog eventLog = EventLog.builder()
                .eventType(event.getEventType().name())
                .userId(event.getUserId())
                .username(event.getUsername())
                .kafkaPartition(partition)
                .kafkaOffset(offset)
                .build();

        eventLogRepository.save(eventLog);
        log.info(">>> [LOGGING] Đã lưu audit log cho event={}, userId={}, partition={}, offset={}",
                event.getEventType(), event.getUserId(), partition, offset);
    }
}
