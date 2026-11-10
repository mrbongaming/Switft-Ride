# SwiftRide backend

Java 21, Spring Boot 4.1.2, Spring Cloud Gateway. Các module:

| Module | Vai trò | Lưu trữ |
|---|---|---|
| `trip-service` | Người dùng demo, chuyến đi, chuyển trạng thái | SQL Server `TripDB` |
| `matching_pricing_service` | Báo giá, vị trí và ghép tài xế | MongoDB `MatchDB` |
| `payment-service` | Thanh toán demo, hoàn tiền, sổ cái, ví tài xế | PostgreSQL `PayDB` |
| `api-gateway` | Định tuyến `/api/**`, CORS | Không |
| `messaging-contracts` | Kiểu sự kiện và tên queue | Không |

Trong mỗi service, phụ thuộc đi từ `api` và `infrastructure` vào `application`, từ `application` vào `domain`. `domain` không phụ thuộc Spring. Service trong `application` điều phối nghiệp vụ; interface repository/client đặt ở `application`, hiện thực ở `infrastructure`. Xem [kiến trúc Matching/Pricing](matching_pricing_service/ARCHITECTURE.md).

## API qua Gateway

| Đường dẫn | Chức năng |
|---|---|
| `GET /api/users?role=rider|driver` | Tài khoản demo |
| `POST /api/trips`, `GET /api/trips`, `GET /api/trips/{id}` | Tạo và xem chuyến |
| `POST /api/quote`, `POST /api/match`, `GET /api/match/offers?driverId=...` | Báo giá và ghép tài xế |
| `POST /api/trips/{id}/confirm-quote`, `/cancel`, `/driver-accept`, `/driver-reject`, `/en-route`, `/pickup`, `/dropoff` | Chuyển trạng thái chuyến |
| `GET /api/payments?riderId=...`, `GET /api/payments/{id}` | Lịch sử thanh toán |
| `POST /api/payments`, `POST /api/payments/{id}/refund` | Thanh toán demo và hoàn tiền; yêu cầu `Idempotency-Key` |
| `GET /api/drivers/{id}/wallet` | Ví tài xế |

`Trip Service` còn có `POST /trips/{id}/quote`, `POST /trips/{id}/offer` và `GET /trips/offers`, được Matching/Pricing Service gọi nội bộ. Các API demo chưa có kiểm tra danh tính, nên chỉ chạy trong môi trường cục bộ.

## Hạ tầng và build

Sao chép `.env.example` thành `.env`, đổi mật khẩu, rồi chạy `docker compose up -d`. Compose tạo SQL Server, MongoDB, PostgreSQL, RabbitMQ và một tác vụ `sqlserver-init` tạo `TripDB`. Script `load-env.ps1` nạp cùng thông số vào terminal PowerShell trước khi khởi động Spring Boot. `mvn test` chạy kiểm tra domain; `mvn package` tạo JAR. Frontend được chạy riêng bằng Vite và chuyển tiếp `/api` tới Gateway.
