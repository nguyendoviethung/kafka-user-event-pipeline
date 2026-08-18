package com.example.serverb.kafka;

import com.example.serverb.event.UserEvent;
import com.example.serverb.service.LoggingService;
import com.example.serverb.service.NotificationService;
import com.example.serverb.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * ĐÂY LÀ ĐIỂM MẤU CHỐT: Server B không có bất kỳ Controller/HTTP endpoint nào
 * nhận dữ liệu từ Server A. Thay vào đó, @KafkaListener sử dụng Kafka Client
 * để tự động poll message từ topic "user-events" thông qua Kafka Protocol.
 *
 * Luồng xử lý:
 *   Kafka topic -> @KafkaListener nhận ConsumerRecord
 *                -> gọi các Service xử lý nghiệp vụ phụ
 *                -> ack thủ công (manual ack) sau khi xử lý xong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final NotificationService notificationService;
    private final LoggingService loggingService;
    private final StatisticsService statisticsService;

    @KafkaListener(
            topics = "${spring.kafka.topics.user-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, UserEvent> record, Acknowledgment acknowledgment) {
        UserEvent event = record.value();
        try {
            log.info("Nhận được event từ Kafka: topic={}, partition={}, offset={}, key={}, eventType={}",
                    record.topic(), record.partition(), record.offset(), record.key(), event.getEventType());

            switch (event.getEventType()) {
                case USER_CREATED -> {
                    notificationService.notifyUserCreated(event);
                    statisticsService.record(event);
                }
                case USER_UPDATED -> {
                    notificationService.notifyUserUpdated(event);
                    statisticsService.record(event);
                }
                case USER_DELETED -> {
                    notificationService.notifyUserDeleted(event);
                    statisticsService.record(event);
                }
                default -> log.warn("Loại event không xác định: {}", event.getEventType());
            }

            // Logging luôn được thực hiện cho mọi loại event
            loggingService.logEvent(event, record.partition(), record.offset());

            // Chỉ commit offset SAU KHI toàn bộ nghiệp vụ phụ xử lý thành công.
            // Nếu có exception ở trên, offset sẽ KHÔNG được commit -> Kafka sẽ
            // redeliver lại message này ở lần poll tiếp theo (đảm bảo at-least-once).
            acknowledgment.acknowledge();

        } catch (Exception ex) {
            log.error("Lỗi khi xử lý event userId={}, eventType={}: {}",
                    event.getUserId(), event.getEventType(), ex.getMessage(), ex);
            // Không ack -> message sẽ được retry. Có thể mở rộng thêm:
            // - Dead Letter Topic (DLT) sau N lần retry thất bại
            // - Retry với backoff (spring-retry / DefaultErrorHandler)
        }
    }
}
