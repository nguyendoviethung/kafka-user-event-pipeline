package com.example.serverb.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Cấu trúc PHẢI khớp với UserEvent bên Server A (đây là "hợp đồng" - event contract
 * giữa 2 service). Trong thực tế nên tách ra thành 1 thư viện (shared-events / avro
 * schema registry) dùng chung để tránh lệch schema giữa 2 phía.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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

    private String source;
}
