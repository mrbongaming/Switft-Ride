# SwiftRide

Frontend React + TypeScript + Tailwind CSS cho hai vai trò **Rider** và **Driver**, dựa trên tài liệu kiến trúc được cung cấp. Backend dùng Spring Boot và Spring Cloud Gateway. Maven, YAML và hạ tầng RabbitMQ đã được cấu hình; các lớp nghiệp vụ chuyến đi, matching, giá và thanh toán vẫn để bạn tự triển khai. Không có mã map.

## Chạy frontend

```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev
```

Mở `http://localhost:5173`. `VITE_API_BASE_URL=/api` dùng Vite proxy tới `GATEWAY_TARGET=http://localhost:8080`. Có thể đổi hai biến trong `frontend/.env`. Khi backend chưa có, frontend vẫn hiển thị nhưng các yêu cầu API sẽ báo lỗi kết nối; giao diện không tạo dữ liệu giả.

## Cấu trúc backend để tự code

```text
backend/
  pom.xml                         # parent Maven
  messaging-contracts/            # thư viện event dùng chung, không phải service
  api-gateway/                     # Spring Cloud Gateway
  trip-service/                    # tạo chuyến, State pattern, domain events; SQL Server
    domain/state/ domain/event/ application/ infrastructure/ api/
  matching_pricing_service/        # geo-query + Standard/Surge/Promo Pricing; MongoDB
    domain/pricing/ application/ infrastructure/ api/
  payment-service/                 # payment, double-entry ledger, refund, Saga, idempotency; PostgreSQL
    domain/ application/ infrastructure/ api/
```

Các thư mục Java thực tế ở `src/main/java/com/swiftride/...`. `pom.xml` và `application.yml` của từng module đã có nội dung. Các file nghiệp vụ cho state, ba chiến lược giá, geo-query, ledger, refund, compensation và idempotency vẫn rỗng để bạn tự code. Tài liệu gốc đề xuất .NET/YARP; theo yêu cầu của bạn, khung này chuyển sang Spring Boot/Spring Cloud Gateway và giữ đúng ba service nghiệp vụ.

## Hợp đồng API qua Gateway

Tất cả đường dẫn dưới đây nằm sau prefix `/api` của Gateway. Frontend dùng JSON, gửi `Authorization: Bearer <JWT>` khi có token, `X-Correlation-Id` trên mỗi request, timeout 12 giây. Các thao tác thanh toán và hoàn tiền gửi `Idempotency-Key` ổn định qua lần thử lại. JWT chỉ được giữ trong `sessionStorage`; không nhập số thẻ/CVV.

| Service | Method / path | Mục đích |
|---|---|---|
| Trip | `POST /trips` | Tạo chuyến `Requested` từ `riderId`, `pickup`, `dropoff` |
| Trip | `GET /trips?riderId=...` hoặc `?driverId=...` | Danh sách cho từng vai trò |
| Trip | `GET /trips/{id}` | Chi tiết chuyến |
| Trip | `POST /trips/{id}/confirm-quote` | Rider chấp nhận báo giá |
| Trip | `POST /trips/{id}/cancel` | Rider hủy |
| Trip | `POST /trips/{id}/driver-accept` | Tài xế chấp nhận |
| Trip | `POST /trips/{id}/driver-reject` | Tài xế từ chối |
| Trip | `POST /trips/{id}/en-route` | Tài xế đang đến |
| Trip | `POST /trips/{id}/pickup` | Đã đón khách |
| Trip | `POST /trips/{id}/dropoff` | Đã trả khách |
| Matching/Pricing | `POST /quote` | Báo giá từ `tripId` |
| Matching/Pricing | `POST /match` | Gợi ý tài xế gần nhất từ `tripId` |
| Matching/Pricing | `GET /match/offers?driverId=...` | Lời mời chờ tài xế |
| Payment | `GET /payments?riderId=...` | Lịch sử giao dịch |
| Payment | `GET /payments/{id}` | Chi tiết giao dịch |
| Payment | `POST /payments` | Tạo giao dịch; frontend có sẵn API client nhưng luồng dropoff dùng sự kiện nội bộ |
| Payment | `POST /payments/{id}/refund` | Hoàn tiền với `reason` |

Các endpoint **được nêu trực tiếp trong tài liệu**: `POST /trips`, `POST /trips/{id}/driver-accept`, `POST /trips/{id}/pickup`, `POST /trips/{id}/dropoff`, `POST /match`, `POST /quote`, `POST /payments`, `POST /payments/{id}/refund`. Các endpoint GET, xác nhận/từ chối/hủy và `en-route` là **hợp đồng bổ sung cho giao diện đầy đủ**; bạn có thể dùng đúng các đường dẫn này khi code backend hoặc chỉnh tập trung trong `frontend/src/lib/api.ts`.

### Dữ liệu JSON mong đợi

- `LocationInput`: `{ "address": "...", "latitude": 10.77, "longitude": 106.69 }`. Vĩ độ/kinh độ là tùy chọn cho đến khi bạn tích hợp map.
- `Trip`: `{ "id": "...", "riderId": "...", "driverId": "...", "pickup": { ... }, "dropoff": { ... }, "status": "Requested", "quotedFare": 50000, "finalFare": 50000, "currency": "VND", "createdAt": "2026-11-09T08:00:00Z" }`.
- `Quote`: `{ "tripId": "...", "amount": 50000, "currency": "VND", "distanceKm": 5, "durationMinutes": 15, "surgeMultiplier": 1 }`.
- `MatchResult`: `{ "tripId": "...", "driverId": "...", "driverName": "...", "vehicle": "...", "etaMinutes": 4, "attempt": 1, "maxAttempts": 3 }`.
- `Payment`: `{ "id": "...", "tripId": "...", "amount": 50000, "currency": "VND", "status": "Succeeded", "createdAt": "2026-11-09T08:30:00Z", "method": "WALLET" }`.

Danh sách trả về **mảng JSON**. Các lệnh cập nhật chuyến trả về `Trip` mới nhất. Lỗi trả JSON có `message`, `detail` hoặc `title`. Trạng thái chuyến: `Requested`, `Quoted`, `DriverOffered`, `DriverAccepted`, `EnRoute`, `PickedUp`, `DroppedOff`, `PaymentPending`, `Paid`; nhánh cuối `Cancelled`/`Failed`. Thanh toán: `Pending`, `Succeeded`, `Failed`, `Refunded`.

Flow theo tài liệu: tạo chuyến → yêu cầu báo giá và matching → rider xác nhận → tài xế nhận/từ chối → đón/trả khách → Trip Service phát `RideCompleted` → Payment Service thanh toán và ghi sổ cái → Trip Service nhận kết quả và đóng chuyến. Rider chỉ theo dõi kết quả trên frontend; backend phải kiểm tra chuyển trạng thái, giá hợp lệ, quyền truy cập và idempotency. Frontend không tính giá hoặc giả lập matching.

## Phần dành cho bạn

- **Map:** vùng bản đồ trên giao diện cố ý để trống. Hiện có ô nhập địa chỉ và tọa độ thủ công để kiểm thử API.
- **RabbitMQ:** exchange, queue, DLQ, publisher, listener và event contract đã có. Listener mặc định tắt cho đến khi bạn viết handler nghiệp vụ; xem [hướng dẫn backend](backend/README.md).
- **Backend:** chưa có database schema, xác thực hay xử lý nghiệp vụ. Bạn cần điền các lớp domain/application/api/repository còn rỗng.


