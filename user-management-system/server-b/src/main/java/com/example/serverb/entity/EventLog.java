package com.example.serverb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bảng lưu lại lịch sử toàn bộ event mà Server B đã nhận & xử lý từ Kafka.
 * Phục vụ mục đích audit / debug / thống kê sau này.
 */
@Entity
@Table(name = "event_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "user_id")
    private Long userId;

    private String username;

    @Column(name = "kafka_partition")
    private Integer kafkaPartition;

    @Column(name = "kafka_offset")
    private Long kafkaOffset;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.processedAt = LocalDateTime.now();
    }
}
