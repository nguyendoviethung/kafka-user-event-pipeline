package com.example.serverb.service;

import com.example.serverb.event.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Giả lập nghiệp vụ gửi thông báo (email / SMS / push notification...)
 * khi có user mới được tạo / cập nhật / xoá.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyUserCreated(UserEvent event) {
        // TODO: tích hợp thật với email service (SMTP), SMS gateway, hoặc push service
        log.info(">>> [NOTIFICATION] Gửi email chào mừng tới {} ({})",
                event.getFullName(), event.getEmail());
    }

    public void notifyUserUpdated(UserEvent event) {
        log.info(">>> [NOTIFICATION] Gửi email thông báo cập nhật thông tin tới {}",
                event.getEmail());
    }

    public void notifyUserDeleted(UserEvent event) {
        log.info(">>> [NOTIFICATION] Gửi email thông báo tài khoản {} đã bị vô hiệu hoá",
                event.getUsername());
    }
}
