echo $env:MAVEN_HOME# Hệ thống Đồng bộ / Bất đồng bộ: Server A + Kafka + Server B

Kiến trúc tách bạch xử lý **đồng bộ** (Server A – CRUD chính, trả response HTTP ngay)
và **bất đồng bộ** (Server B – xử lý nghiệp vụ phụ qua Kafka, không chặn luồng chính).

```
Client (Postman/FE)
        │  HTTP/REST
        ▼
   Server A (8080)              PostgreSQL (5432)
   Spring Boot                  users / roles
   Controller→Service→Repo  ──► departments / organizations
        │   
        │ publish (Kafka Producer)
        ▼
   Kafka topic: user-events
        │
        │ Kafka Client (poll) — KHÔNG phải HTTP
        ▼
   Server B (8081)              PostgreSQL (5433)
   @KafkaListener               event_logs (audit)
   Notification / Logging / Statistics
```

**Điểm quan trọng:** Server A và Server B **không** gọi HTTP trực tiếp với nhau.
Server A chỉ `kafkaTemplate.send(...)` lên topic; Server B là **Kafka Consumer**
(`@KafkaListener`) tự poll message bằng Kafka client/protocol, hoàn toàn độc lập.

---

## 1. Cấu trúc thư mục

```
user-management-system/
├── docker-compose.yml        # Postgres x2, Zookeeper, Kafka, Kafdrop (UI xem topic)
├── server-a/                 # CRUD chính + Kafka Producer (port 8080)
│   └── src/main/java/com/example/servera/
│       ├── entity/           # User, Role, Department, Organization
│       ├── dto/               # Request/Response
│       ├── repository/        # Spring Data JPA
│       ├── service/           # Nghiệp vụ + publish Kafka event
│       ├── controller/        # REST API
│       ├── event/             # UserEvent (event contract)
│       ├── kafka/             # UserEventProducer
│       ├── config/            # KafkaProducerConfig
│       └── exception/         # Xử lý lỗi tập trung
└── server-b/                 # Kafka Consumer (port 8081)
    └── src/main/java/com/example/serverb/
        ├── event/              # UserEvent (phải khớp schema với Server A)
        ├── kafka/              # UserEventConsumer (@KafkaListener)
        ├── config/             # KafkaConsumerConfig
        ├── service/            # NotificationService, LoggingService, StatisticsService
        ├── entity/ repository/ # EventLog (audit trail)
        └── controller/         # StatisticsController (xem kết quả xử lý)
```

## 2. Chạy hạ tầng (Postgres + Kafka)

```bash
cd user-management-system
docker compose up -d
```

- Postgres Server A: `localhost:5432` (db: `server_a_db`)
- Postgres Server B: `localhost:5433` (db: `server_b_db`)
- Kafka broker: `localhost:9092`
- Kafdrop (xem topic/message trực quan): http://localhost:9000

## 3. Chạy Server A

```bash
cd server-a
mvn spring-boot:run
```

Server A chạy ở `




















`.

### API mẫu

**Tạo Organization**
```
POST /api/organizations
{ "name": "Cong ty ABC", "description": "Tru so chinh" }
```

**Tạo Department**
```
POST /api/departments
{ "name": "Phong IT", "description": "...", "organizationId": 1 }
```

**Tạo Role**
```
POST /api/roles
{ "name": "ADMIN", "description": "Quan tri he thong" }
```

**Tạo User** (thao tác này sẽ tự động publish `UserEvent` lên Kafka)
```
POST /api/users
{
  "username": "hung",
  "email": "hung@gmail.com",
  "fullName": "Nguyen Van Hung",
  "roleId": 1,
  "departmentId": 1
}
```

Các endpoint còn lại: `GET /api/users`, `GET /api/users/{id}`, `PUT /api/users/{id}`,
`DELETE /api/users/{id}` (tương tự cho `roles`, `departments`, `organizations`).

## 4. Chạy Server B

```bash
cd server-b
mvn spring-boot:run
```

Server B chạy ở `http://localhost:8086`, tự động subscribe topic `user-events`.

Khi Server A tạo/sửa/xoá user, log của Server B sẽ hiện:
```
>>> [NOTIFICATION] Gửi email chào mừng tới Nguyen Van Hung (hung@gmail.com)
>>> [STATISTICS] Tổng số event USER_CREATED tính đến hiện tại: 1
>>> [LOGGING] Đã lưu audit log cho event=USER_CREATED, userId=1, partition=0, offset=0
```

Kiểm tra kết quả xử lý:
```
GET http://localhost:8081/api/statistics
GET http://localhost:8081/api/statistics/logs/count
```

## 5. Vì sao tách Server A / Kafka / Server B như vậy?

| Thành phần | Vai trò | Giao tiếp |
|---|---|---|
| Server A | Nghiệp vụ chính, CRUD, trả response ngay cho client | HTTP/REST (đồng bộ) với client; **publish** message lên Kafka (không chờ Server B) |
| Kafka | Message broker trung gian, lưu event bền vững, đảm bảo thứ tự theo key | Kafka protocol (nhị phân, khác HTTP) |
| Server B | Nghiệp vụ phụ: notification, logging, statistics — không ảnh hưởng tốc độ phản hồi của Server A | **Kafka Consumer** (`@KafkaListener`) tự poll, không có endpoint nhận dữ liệu từ Server A |

Lợi ích:
- **Giảm coupling**: Server A không cần biết Server B có đang chạy hay không.
- **Chịu lỗi tốt hơn**: Server B down tạm thời → message vẫn nằm trong Kafka, xử lý bù (replay) khi Server B lên lại.
- **Scale độc lập**: có thể tăng consumer instance của Server B mà không đụng vào Server A.
- **Tách trách nhiệm rõ ràng**: nghiệp vụ chính (đồng bộ, cần phản hồi nhanh) tách khỏi nghiệp vụ phụ (bất đồng bộ, có thể trễ vài giây/phút không sao).

## 6. Mở rộng gợi ý

- Thêm **Dead Letter Topic (DLT)** cho message xử lý lỗi liên tục ở Server B.
- Áp dụng **Outbox Pattern** ở Server A để đảm bảo publish event không bị mất khi DB commit thành công nhưng Kafka lỗi.
- Dùng **Avro + Schema Registry** thay JSON thô để quản lý version của event contract chặt chẽ hơn.
- Thêm Spring Security (JWT) cho các endpoint CRUD.
