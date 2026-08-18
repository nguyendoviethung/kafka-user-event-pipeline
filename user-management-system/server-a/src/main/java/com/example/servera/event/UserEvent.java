package com.example.servera.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Payload event được Server A publish lên Kafka topic "user-events".
 * Server B (Kafka Consumer) sẽ deserialize đúng cấu trúc này để xử lý
 * nghiệp vụ phụ (notification, logging, statistics...).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserEvent {

    private EventType eventType;

    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private Long roleId;
    private String roleName;
    private Long departmentId;
    private String departmentName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime occurredAt;

    // Ai / nguồn nào gây ra sự kiện, hữu ích cho việc log/audit
    private String source;
}
