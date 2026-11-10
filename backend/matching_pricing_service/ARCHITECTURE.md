# Matching/Pricing: Clean Architecture

```text
api --> application --> domain
infrastructure --> application --> domain
```

`MatchingApplicationService` chọn tài xế khả dụng gần điểm đón trong 10 km. `PricingApplicationService` tính giá backend từ tọa độ và ghi báo giá vào Trip Service. `TripCatalog` và `DriverDirectory` là interface do application sở hữu; HTTP client và Mongo repository hiện thực chúng trong infrastructure. Domain chứa `DriverLocation`, `Coordinates`, `Distance` và các chính sách giá thuần Java.

Không có lớp `UseCase` hoặc cấu trúc `port/adapter`. API DTO và Mongo document không đi vào domain. Hai tài xế demo được khởi tạo ở infrastructure khi MongoDB chưa có vị trí nào.
