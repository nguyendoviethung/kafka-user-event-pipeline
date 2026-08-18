package com.example.serverb.controller;

import com.example.serverb.event.EventType;
import com.example.serverb.repository.EventLogRepository;
import com.example.serverb.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint phụ trợ để KIỂM TRA kết quả xử lý bất đồng bộ của Server B
 * (không phải là điểm nhận dữ liệu từ Server A — dữ liệu vẫn tới qua Kafka).
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final EventLogRepository eventLogRepository;

    @GetMapping
    public Map<EventType, Long> getStatistics() {
        return statisticsService.getSnapshot();
    }

    @GetMapping("/logs/count")
    public Map<String, Long> getLogCounts() {
        return Map.of(
                "USER_CREATED", eventLogRepository.countByEventType("USER_CREATED"),
                "USER_UPDATED", eventLogRepository.countByEventType("USER_UPDATED"),
                "USER_DELETED", eventLogRepository.countByEventType("USER_DELETED")
        );
    }
}
