# SwiftRide

Demo đặt xe gồm giao diện React/TypeScript, API Gateway và ba Spring Boot service. Backend tổ chức theo **Clean Architecture**: `domain` chứa quy tắc nghiệp vụ thuần Java, `application` chứa các service và interface lưu trữ, `infrastructure` triển khai JPA/MongoDB/RabbitMQ/HTTP, `api` chuyển HTTP thành lệnh gọi service. Không có lớp `UseCase`, `port` hay `adapter`.

## Chạy dự án

Yêu cầu Java 21+, Maven, Node.js 20+, Docker Desktop. Trong `backend`:

```powershell
Copy-Item .env.example .env
# Đổi mật khẩu trong .env
docker compose up -d
docker compose ps                 # chờ sqlserver-init thoát với mã 0
. .\load-env.ps1                 # nạp biến môi trường vào terminal hiện tại
mvn package
```

Mở **bốn terminal PowerShell** trong `backend`, chạy `. .\load-env.ps1` ở mỗi terminal rồi chạy một lệnh tương ứng:

```powershell
java -jar trip-service/target/trip-service-1.0.0-SNAPSHOT.jar
java -jar matching_pricing_service/target/matching_pricing_service-1.0.0-SNAPSHOT.jar
java -jar payment-service/target/payment-service-1.0.0-SNAPSHOT.jar
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
```

Trong terminal khác:

```powershell
cd frontend
npm ci
npm run dev
```

Mở `http://localhost:5173`. Gateway ở cổng 8080. Lần khởi động đầu tự tạo hai tài xế và một khách demo. Vị trí ban đầu của tài xế ở trung tâm TP.HCM; hãy chọn điểm đón trong bán kính 10 km để thử luồng ghép xe.

## Luồng hoạt động

1. Rider chọn điểm đón và điểm đến, tạo chuyến, lấy giá từ Matching/Pricing Service rồi tìm tài xế.
2. Rider xác nhận giá; tài xế nhận lời mời, bắt đầu đi đón, đón khách và trả khách.
3. Trip Service phát sự kiện `RideCompleted`. Payment Service ghi các bút toán cân bằng cho rider, tài xế (80%) và nền tảng (20%), rồi phát `PaymentSettled`. Trip Service chuyển chuyến sang `Paid`.
4. Rider xem thanh toán/hoàn tiền; tài xế xem ví và lịch sử thu nhập.

Các trạng thái chuyến được kiểm soát trong `Trip` domain. Giá do backend tính từ tọa độ (khoảng cách đường chim bay nhân 1,3; giá mở cửa 12.000đ, 9.000đ/km). Giao diện dùng OSM/Nominatim/OSRM để tìm địa điểm, xem bản đồ và xem trước tuyến đường; nếu tuyến xem trước không tải được, backend vẫn báo giá từ tọa độ.

## Giới hạn demo

Đây là **demo học tập**, chưa có đăng nhập/phân quyền và chưa kết nối cổng thanh toán thật. Danh sách người dùng trong giao diện là tài khoản demo, và thanh toán `WALLET` được xác nhận giả lập. Không dùng hệ thống này để thu tiền thật hoặc triển khai công khai. Sự kiện đã có khóa xử lý lặp theo chuyến ở Payment Service, nhưng chưa có transactional outbox; cần bổ sung trước khi yêu cầu bảo đảm giao sự kiện trong môi trường production.

Chi tiết module, cấu hình và API: [backend/README.md](backend/README.md).
