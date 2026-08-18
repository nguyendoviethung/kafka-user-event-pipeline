package com.example.servera.kafka;

import com.example.servera.event.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Đây là điểm "cầu nối" bất đồng bộ giữa Server A và Server B.
 * Server A KHÔNG gọi HTTP trực tiếp sang Server B, mà chỉ đẩy (publish)
 * message lên Kafka topic. Server B là Kafka Consumer, tự đọc message này
 * thông qua Kafka client (Kafka protocol), độc lập hoàn toàn với Server A.
 */
@Component
public class UserEventProducer {

    private static final Logger log = LoggerFactory.getLogger(UserEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topics.user-events}")
    private String userEventsTopic;

    // Constructor viết tay thay cho @RequiredArgsConstructor,
    // tránh phụ thuộc vào việc Lombok annotation processing có được bật hay không.
    public UserEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(UserEvent event) {
        // key = userId -> đảm bảo các event của cùng 1 user luôn vào cùng 1 partition,
        // giữ đúng thứ tự xử lý (created -> updated -> deleted) ở phía consumer.
        String key = String.valueOf(event.getUserId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(userEventsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Đã publish event [{}] cho userId={} tới partition={}, offset={}",
                        event.getEventType(), event.getUserId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Publish event [{}] cho userId={} THẤT BẠI: {}",
                        event.getEventType(), event.getUserId(), ex.getMessage(), ex);
                // Ở đây có thể mở rộng: lưu vào bảng "outbox" để retry sau (Outbox Pattern)
            }
        });
    }
}
