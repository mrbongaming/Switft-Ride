# Matching & Pricing - Clean Architecture

Module này dùng Clean Architecture thuần và thống nhất với các service còn lại trong dự án.
Không tổ chức source code theo mô hình Hexagonal (`port`/`adapter`).

## Quy tắc phụ thuộc

```text
api --------------------> application ----> domain
infrastructure ---------> application ----> domain
infrastructure ---------------------------> domain
```

Mọi phụ thuộc phải hướng vào trong:

- `domain`: entity, value object và quy tắc tính giá cốt lõi; không phụ thuộc Spring,
  MongoDB, RabbitMQ hoặc HTTP.
- `application`: use case và dịch vụ điều phối nghiệp vụ; chỉ dùng domain và các
  abstraction do application sở hữu khi cần truy cập dữ liệu hoặc dịch vụ ngoài.
- `api`: REST controller, request/response DTO và mapper; chuyển dữ liệu HTTP sang
  input của application.
- `infrastructure`: MongoDB repository, HTTP client, RabbitMQ listener và cấu hình
  Spring; hiện thực các abstraction mà application cần.

Không truyền Mongo document hoặc HTTP DTO vào domain. Việc chuyển đổi dữ liệu phải
được thực hiện tại biên `api` hoặc `infrastructure`.

## Thứ tự triển khai

1. Hoàn thiện domain model và các chính sách tính giá.
2. Định nghĩa input/output của từng use case trong `application`.
3. Cài đặt application service và unit test bằng repository/client giả trong test.
4. Cài đặt MongoDB repository, Trip Service client và routing client trong
   `infrastructure`.
5. Cài đặt REST controller trong `api`.
6. Cài đặt xử lý event có idempotency rồi mới bật RabbitMQ consumer.

