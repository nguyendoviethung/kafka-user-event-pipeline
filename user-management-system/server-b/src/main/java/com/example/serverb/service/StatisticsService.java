package com.example.serverb.service;

import com.example.serverb.event.EventType;
import com.example.serverb.event.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thống kê số lượng event theo loại (in-memory, đơn giản hoá cho mục đích demo).
 * Trong thực tế có thể đẩy dữ liệu này vào Redis / Elasticsearch / một bảng thống kê riêng.
 */
@Service
public class StatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsService.class);

    private final Map<EventType, AtomicLong> counters = new ConcurrentHashMap<>();

    public void record(UserEvent event) {
        AtomicLong counter = counters.computeIfAbsent(event.getEventType(), k -> new AtomicLong(0));
        long current = counter.incrementAndGet();
        log.info(">>> [STATISTICS] Tổng số event {} tính đến hiện tại: {}",
                event.getEventType(), current);
    }

    public Map<EventType, Long> getSnapshot() {
        Map<EventType, Long> snapshot = new ConcurrentHashMap<>();
        counters.forEach((k, v) -> snapshot.put(k, v.get()));
        return snapshot;
    }
}
