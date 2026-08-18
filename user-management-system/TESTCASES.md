# Test Cases — Hệ thống Server A (CRUD + Kafka Producer) / Server B (Kafka Consumer)

## Chuẩn bị trước khi test

1. `docker compose up -d` (Postgres A, Postgres B, Kafka, Zookeeper, Kafdrop)
2. Chạy `ServerAApplication` (port `8080`)
3. Chạy `ServerBApplication` (port `8081`)
4. Mở sẵn log của cả 2 server để quan sát khi test luồng Kafka
5. (Tuỳ chọn) Mở Kafdrop tại `http://localhost:9000` để xem message thực tế trong topic `user-events`

> **Thứ tự chạy test quan trọng**: phải tạo `Role` + `Organization` trước, rồi mới tạo được `Department` (cần `organizationId`), rồi mới tạo được `User` (cần `roleId` + `departmentId`).

---

## A. TEST CASE — ORGANIZATION (`/api/organizations`)

| ID | Mô tả | Method & Endpoint | Input | Kết quả mong đợi |
|---|---|---|---|---|
| ORG-01 | Tạo Organization hợp lệ | POST `/api/organizations` | `{"name":"Cong ty ABC","description":"Tru so chinh"}` | `201 Created`, trả về object có `id` |
| ORG-02 | Tạo Organization trùng tên | POST `/api/organizations` | Lặp lại `name` giống ORG-01 | `409 Conflict`, message "Tổ chức đã tồn tại..." |
| ORG-03 | Tạo Organization thiếu `name` | POST `/api/organizations` | `{"description":"..."}` | `400 Bad Request`, `fieldErrors.name` báo lỗi |
| ORG-04 | Lấy danh sách Organization | GET `/api/organizations` | — | `200 OK`, trả về mảng chứa org vừa tạo |
| ORG-05 | Lấy Organization theo id hợp lệ | GET `/api/organizations/{id}` | `id` của ORG-01 | `200 OK`, đúng dữ liệu |
| ORG-06 | Lấy Organization theo id không tồn tại | GET `/api/organizations/99999` | — | `404 Not Found` |
| ORG-07 | Cập nhật Organization | PUT `/api/organizations/{id}` | `{"name":"Cong ty ABC Updated","description":"..."}` | `200 OK`, `name` được cập nhật |
| ORG-08 | Cập nhật Organization không tồn tại | PUT `/api/organizations/99999` | body bất kỳ | `404 Not Found` |
| ORG-09 | Xoá Organization (thực hiện SAU CÙNG, sau khi đã test xong Department) | DELETE `/api/organizations/{id}` | — | `204 No Content`; các Department thuộc org này cũng bị xoá theo (cascade) |
| ORG-10 | Xoá Organization không tồn tại | DELETE `/api/organizations/99999` | — | `404 Not Found` |

---

## B. TEST CASE — ROLE (`/api/roles`)

| ID | Mô tả | Method & Endpoint | Input | Kết quả mong đợi |
|---|---|---|---|---|
| ROLE-01 | Tạo Role hợp lệ | POST `/api/roles` | `{"name":"ADMIN","description":"Quan tri he thong"}` | `201 Created` |
| ROLE-02 | Tạo Role trùng tên | POST `/api/roles` | `name` = "ADMIN" (trùng ROLE-01) | `409 Conflict` |
| ROLE-03 | Tạo Role thiếu `name` | POST `/api/roles` | `{"description":"..."}` | `400 Bad Request` |
| ROLE-04 | Lấy danh sách Role | GET `/api/roles` | — | `200 OK` |
| ROLE-05 | Lấy Role theo id không tồn tại | GET `/api/roles/99999` | — | `404 Not Found` |
| ROLE-06 | Cập nhật Role | PUT `/api/roles/{id}` | `{"name":"ADMIN","description":"Updated"}` | `200 OK` |
| ROLE-07 | Xoá Role đang được User tham chiếu | DELETE `/api/roles/{id}` (id đang gán cho 1 User) | — | Thất bại (lỗi FK constraint / 500) — **ghi nhận là hành vi cần xử lý thêm**, xem mục "Ghi chú" |

---

## C. TEST CASE — DEPARTMENT (`/api/departments`)

> Cần có `organizationId` hợp lệ từ mục A trước khi test.

| ID | Mô tả | Method & Endpoint | Input | Kết quả mong đợi |
|---|---|---|---|---|
| DEPT-01 | Tạo Department hợp lệ | POST `/api/departments` | `{"name":"Phong IT","description":"...","organizationId": <id ORG-01>}` | `201 Created` |
| DEPT-02 | Tạo Department với `organizationId` không tồn tại | POST `/api/departments` | `{"name":"Phong X","organizationId": 99999}` | `404 Not Found` |
| DEPT-03 | Tạo Department thiếu `organizationId` | POST `/api/departments` | `{"name":"Phong Y"}` | `400 Bad Request` |
| DEPT-04 | Lấy danh sách Department | GET `/api/departments` | — | `200 OK`, có `organizationName` kèm theo |
| DEPT-05 | Lấy Department theo id không tồn tại | GET `/api/departments/99999` | — | `404 Not Found` |
| DEPT-06 | Cập nhật Department | PUT `/api/departments/{id}` | đổi `name` hoặc `organizationId` khác | `200 OK` |
| DEPT-07 | Cập nhật Department sang `organizationId` không tồn tại | PUT `/api/departments/{id}` | `organizationId: 99999` | `404 Not Found` |
| DEPT-08 | Xoá Department đang được User tham chiếu | DELETE `/api/departments/{id}` (id đang gán cho 1 User) | — | Thất bại (lỗi FK constraint) — xem mục "Ghi chú" |

---

## D. TEST CASE — USER (`/api/users`) — quan trọng nhất, có publish Kafka event

> Cần `roleId` (mục B) và `departmentId` (mục C) hợp lệ trước khi test.

| ID | Mô tả | Method & Endpoint | Input | Kết quả mong đợi |
|---|---|---|---|---|
| USER-01 | Tạo User hợp lệ | POST `/api/users` | `{"username":"hung","email":"hung@gmail.com","fullName":"Nguyen Van Hung","roleId":<id>,"departmentId":<id>}` | `201 Created`, đồng thời **log Server A** hiện `Đã publish event [USER_CREATED]...`, **log Server B** hiện `[NOTIFICATION]`, `[STATISTICS]`, `[LOGGING]` |
| USER-02 | Tạo User trùng `username` | POST `/api/users` | `username` = "hung" (trùng USER-01) | `409 Conflict`, **không** có event mới nào được publish (kiểm tra log Server B không tăng) |
| USER-03 | Tạo User trùng `email` | POST `/api/users` | `email` = "hung@gmail.com", `username` khác | `409 Conflict` |
| USER-04 | Tạo User với `roleId` không tồn tại | POST `/api/users` | `roleId: 99999` | `404 Not Found` |
| USER-05 | Tạo User với `departmentId` không tồn tại | POST `/api/users` | `departmentId: 99999` | `404 Not Found` |
| USER-06 | Tạo User thiếu `email` | POST `/api/users` | bỏ field `email` | `400 Bad Request`, `fieldErrors.email` |
| USER-07 | Tạo User với `email` sai định dạng | POST `/api/users` | `email: "khong-phai-email"` | `400 Bad Request` |
| USER-08 | Lấy danh sách User | GET `/api/users` | — | `200 OK` |
| USER-09 | Lấy User theo id | GET `/api/users/{id}` | id của USER-01 | `200 OK`, có `roleName`, `departmentName` |
| USER-10 | Lấy User không tồn tại | GET `/api/users/99999` | — | `404 Not Found` |
| USER-11 | Cập nhật User | PUT `/api/users/{id}` | đổi `fullName`, `email` | `200 OK`, log Server A hiện `USER_UPDATED`, log Server B hiện notification "cập nhật" |
| USER-12 | Cập nhật User không tồn tại | PUT `/api/users/99999` | body bất kỳ | `404 Not Found` |
| USER-13 | Xoá (soft-delete) User | DELETE `/api/users/{id}` | — | `204 No Content`; GET lại User đó → `status: "DELETED"` (bản ghi vẫn còn, không mất hẳn); log Server B hiện `USER_DELETED` |
| USER-14 | Xoá User không tồn tại | DELETE `/api/users/99999` | — | `404 Not Found` |

---

## E. TEST CASE — LUỒNG BẤT ĐỒNG BỘ (Server A → Kafka → Server B)

Mục tiêu: xác nhận Server A **không chờ** Server B, và Server B xử lý **độc lập** qua Kafka.

| ID | Mô tả | Cách thực hiện | Kết quả mong đợi |
|---|---|---|---|
| ASYNC-01 | Response của Server A trả về ngay, không phụ thuộc Server B | Tắt hẳn Server B, sau đó gọi `POST /api/users` ở Server A | Server A vẫn trả `201 Created` bình thường (vì chỉ publish Kafka, không gọi HTTP sang B) |
| ASYNC-02 | Server B nhận lại được message sau khi khởi động lại | Với message vừa tạo ở ASYNC-01 (lúc Server B đang tắt), bật lại Server B | Server B tự động consume message còn tồn trong Kafka (do `auto-offset-reset: earliest` + `group-id` chưa từng commit offset đó) → log Server B hiện xử lý event vừa rồi |
| ASYNC-03 | Thứ tự xử lý event đúng theo user | Tạo 1 user, sau đó update liên tiếp 2-3 lần thật nhanh | Log Server B xử lý đúng thứ tự `CREATED → UPDATED → UPDATED` (nhờ key = userId đảm bảo cùng partition) |
| ASYNC-04 | Kiểm tra thống kê tăng đúng số lượng | Gọi `GET http://localhost:8081/api/statistics` sau khi tạo N user | Trả về `{"USER_CREATED": N, ...}` khớp số lượng thật đã tạo |
| ASYNC-05 | Kiểm tra log audit trong DB Server B | Gọi `GET http://localhost:8081/api/statistics/logs/count` | Số đếm theo từng loại event khớp với số thao tác đã làm ở mục D |
| ASYNC-06 | Xem trực tiếp message trong Kafka | Mở Kafdrop `http://localhost:9000` → chọn topic `user-events` → View Messages | Thấy đúng JSON payload (`eventType`, `userId`, `email`...) của các thao tác vừa test |

---

## F. TEST CASE — KIỂM TRA DATABASE (qua pgAdmin hoặc psql)

| ID | Mô tả | Cách thực hiện | Kết quả mong đợi |
|---|---|---|---|
| DB-01 | Bảng được tạo tự động ở Server A | Kết nối `server_a_db` (port 5432), xem danh sách bảng | Có `users`, `roles`, `departments`, `organizations` |
| DB-02 | Bảng được tạo tự động ở Server B | Kết nối `server_b_db` (port 5433), xem danh sách bảng | Có `event_logs` |
| DB-03 | Dữ liệu User khớp giữa request và DB | Sau USER-01, query `SELECT * FROM users WHERE username='hung'` | Dữ liệu khớp đúng những gì đã gửi |
| DB-04 | `event_logs` ghi đúng | Sau khi test xong mục D, query `SELECT * FROM event_logs ORDER BY id` | Số dòng và `event_type` khớp với các thao tác Create/Update/Delete User đã thực hiện |

---

## Ghi chú / Giới hạn hiện tại (biết trước để không nhầm là bug)

1. **ROLE-07 / DEPT-08**: hiện tại code **chưa** có validate "không cho xoá Role/Department nếu đang có User tham chiếu" → sẽ gây lỗi FK constraint (`500 Internal Server Error`) thay vì trả lỗi rõ ràng (`409 Conflict`). Đây là điểm có thể cải thiện thêm nếu cần, không phải lỗi hệ thống nghiêm trọng.
2. **Xoá User là soft-delete** (chỉ đổi `status` sang `DELETED`, không xoá hẳn khỏi DB) — vì vậy `username`/`email` của user đã "xoá" vẫn được coi là trùng nếu tạo lại (USER-02/USER-03 vẫn áp dụng).
3. Khi test **ASYNC-02**, cần đảm bảo Server B dùng đúng `group-id` cũ (`server-b-group` trong `application.yml`) — nếu đổi group-id mới, Kafka sẽ coi là consumer mới và đọc lại từ đầu tuỳ theo `auto-offset-reset`.
