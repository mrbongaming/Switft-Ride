# Backend SwiftRide

Spring Boot 4.1.2, Spring Cloud 2025.1.4, Java 21. `messaging-contracts` là thư viện JAR dùng chung, không phải microservice thứ tư. Có ba service nghiệp vụ và một API Gateway.

## Đối chiếu mục 5 của tài liệu

| Service | Trách nhiệm và file để bạn triển khai | API tiêu biểu |
|---|---|---|
| Trip Service | `domain/Trip.java`, `domain/TripState.java`, các file rỗng ở `domain/state/`, `domain/event/`, `application/TripApplicationService.java`; dữ liệu riêng trong SQL Server | `POST /trips`, `POST /trips/{id}/driver-accept`, `POST /trips/{id}/pickup`, `POST /trips/{id}/dropoff` trong `api/TripController.java` |
| Matching và Pricing Service | `application/GeoDriverQuery.java`, `infrastructure/persistence/mongo/DriverLocationRepository.java` cho MongoDB geo-query; `domain/pricing/PricingStrategy.java`, `StandardPricing.java`, `SurgePricing.java`, `PromoPricing.java` | `POST /match`, `POST /quote` trong hai controller ở `api/` |
| Payment Service | `domain/Payment.java`, `LedgerTransaction.java`, `LedgerEntry.java`, `PaymentMethodToken.java`; `application/PaymentApplicationService.java`, `RefundApplicationService.java`, `SagaCompensationService.java`, `IdempotencyService.java`; repository riêng trong PostgreSQL | `POST /payments`, `POST /payments/{id}/refund` trong `api/PaymentController.java` |

Các file nghiệp vụ nêu trong bảng là file rỗng. Chỉ hạ tầng Gateway/RabbitMQ, event contract và file khởi động Spring Boot đã có code. Payment Service chỉ nên lưu token do cổng thanh toán cấp, không lưu số thẻ hoặc CVV. Các endpoint GET và thao tác bổ sung trong frontend là hợp đồng mở rộng cho giao diện, không thay thế tám API tiêu biểu của tài liệu.

## Chạy hạ tầng RabbitMQ

```powershell
Copy-Item .env.example .env
# Sửa mật khẩu trong .env trước khi chạy.
docker compose up -d rabbitmq
```

RabbitMQ AMQP ở `localhost:5672`, giao diện quản trị ở `http://localhost:15672`. Docker Compose chỉ chạy RabbitMQ. SQL Server, MongoDB và PostgreSQL cần được chạy/cấp riêng rồi thiết lập biến môi trường cho từng service. File `.env` phục vụ Docker Compose; Spring Boot chạy từ terminal/IDE **không tự đọc** file đó.

## Biến môi trường cho Spring Boot

| Biến | Dùng cho |
|---|---|
| `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | Trip, Matching, Payment; bắt buộc |
| `RABBITMQ_HOST`, `RABBITMQ_PORT` | Mặc định `localhost:5672` |
| `TRIP_DB_URL`, `TRIP_DB_USER`, `TRIP_DB_PASSWORD` | SQL Server/TripDB |
| `MATCH_DB_URI` | MongoDB/MatchDB |
| `PAY_DB_URL`, `PAY_DB_USER`, `PAY_DB_PASSWORD` | PostgreSQL/PayDB |
| `TRIP_SERVICE_URL`, `MATCHING_PRICING_SERVICE_URL`, `PAYMENT_SERVICE_URL` | Gateway; mặc định cổng 8081/8082/8083 |
| `FRONTEND_ORIGIN` | CORS Gateway; mặc định `http://localhost:5173` |

Trong PowerShell, thiết lập biến môi trường cho phiên chạy, ví dụ `$env:RABBITMQ_USER = 'swiftride'` và `$env:RABBITMQ_PASSWORD = '<mật khẩu trong .env>'`. Không commit mật khẩu thật. Build toàn bộ bằng `mvn -DskipTests package` tại thư mục `backend/`. Nếu cache Maven ngoài workspace bị hạn chế, dùng `mvn '-Dmaven.repo.local=.m2/repository' -DskipTests package`.

## Event và nơi nối nghiệp vụ

| Publisher | Routing key | Consumer | Việc bạn cần viết |
|---|---|---|---|
| Trip | `trip.accepted.v1` | Matching | Cập nhật trạng thái sẵn sàng của tài xế |
| Trip | `ride.completed.v1` | Payment | Trừ tiền/ghi ledger idempotent |
| Payment | `payment.settled.v1` | Trip | Chuyển chuyến sang `Paid` |
| Payment | `payment.failed.v1` | Trip | Chuyển sang chờ xử lý/lỗi theo state machine |

Topology nằm trong `infrastructure/messaging/*RabbitConfiguration.java`; publisher nằm cùng package. Event records và tên exchange/queue ở `messaging-contracts`. Mỗi queue có DLQ riêng. Listener retry có giới hạn rồi chuyển message lỗi vào DLQ. Publisher dùng confirm và mandatory routing; phương thức publish trả `CompletableFuture<Void>` để application/Outbox worker theo dõi kết quả.

Các listener đang có `swiftride.messaging.consumers-enabled: false` trong `application.yml`. Sau khi bạn triển khai `TripAcceptedHandler`, `RideCompletedHandler` và `PaymentResultHandler` với transaction + chống xử lý trùng `eventId`/`idempotencyKey`, đổi cờ này thành `true` cho service tương ứng. Nếu bật trước khi có handler, service sẽ lỗi lúc khởi động thay vì xác nhận và làm mất message. `Outbox` chưa được triển khai vì cần gắn với transaction của các lớp nghiệp vụ/DB mà bạn chưa viết; không gọi publisher trực tiếp trong transaction rồi coi đó là bảo đảm nhất quán.

Các endpoint HTTP vẫn cần bạn triển khai trong các controller còn rỗng. Gateway đã định tuyến `/api/trips/**`, `/api/quote`, `/api/match/**`, `/api/payments/**` đến từng service. Xác thực JWT và phân quyền vẫn là phần bạn cần viết trước khi triển khai thực tế.


