package com.example.serverb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Server B - KHÔNG nhận request trực tiếp từ Server A qua HTTP.
 * Server B đóng vai trò Kafka Consumer: tự kết nối tới Kafka broker,
 * subscribe topic "user-events" và xử lý các nghiệp vụ phụ
 * (notification, logging, statistics...) một cách bất đồng bộ.
 */
@SpringBootApplication
public class ServerBApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerBApplication.class, args);
    }
}
