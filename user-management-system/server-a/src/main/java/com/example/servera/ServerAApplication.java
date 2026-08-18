package com.example.servera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Server A - xử lý nghiệp vụ chính (CRUD User, Role, Department, Organization)
 * và đóng vai trò Kafka Producer, phát event bất đồng bộ cho Server B xử lý.
 */
@SpringBootApplication
public class ServerAApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerAApplication.class, args);
    }
}
