# Campus Hub Base

## Mini Project Session 13: Xác nhận đồ thất lạc

Nhóm mở rộng dự án nền bằng luồng xác nhận đồ thất lạc. Người nhặt được đồ đăng nhập, mở bài trong chuyên mục **Đồ thất lạc**, gửi đặc điểm nhận dạng riêng. Chủ bài xem yêu cầu, xác nhận hoặc từ chối. Khi xác nhận, bài tự chuyển sang trạng thái `RESOLVED`. API giới hạn quyền xem mô tả, ngăn gửi trùng, không cho chủ bài tự xác nhận và khóa bản ghi bài khi duyệt để tránh hai yêu cầu được duyệt đồng thời.

API mới:

| Phương thức | Đường dẫn | Quyền |
| --- | --- | --- |
| `GET` | `/api/posts/{id}/claims` | Chủ bài xem tất cả; người gửi chỉ xem yêu cầu của mình |
| `POST` | `/api/posts/{id}/claims` | Người đăng nhập không phải chủ bài |
| `PATCH` | `/api/posts/{id}/claims/{claimId}` | Chỉ chủ bài |

Lược đồ mới nằm trong `backend/src/main/resources/db/migration/V2__create_lost_item_claims.sql`. Kiểm thử luồng nằm trong `LostItemClaimIntegrationTest`.

### Chạy bằng Docker Compose

1. Sao chép `.env.example` thành `.env` và đặt hai mật khẩu DB khác nhau.
2. Chạy `docker compose up -d --build`.
3. Sau khi DNS và cổng 443 sẵn sàng, mở `https://103-72-57-81.sslip.io`.

Ứng dụng không tạo sẵn tài khoản hay mật khẩu mẫu. Người dùng đầu tiên tự đăng ký trên giao diện. Caddy tự cấp và gia hạn TLS cho tên miền chứa IP bằng thử thách TLS trên cổng 443; cổng 80 của máy chủ có thể tiếp tục phục vụ ứng dụng hiện có. Cần mở cổng 443, xác minh DNS trỏ đúng IP và bảo đảm cổng này chưa bị dịch vụ khác sử dụng. **HTTP Basic chỉ được dùng qua HTTPS khi đưa lên Internet**. Không commit `.env`.

Để thử luồng: tạo tài khoản A và B; A đăng bài chuyên mục Đồ thất lạc; B mở bài và gửi mô tả; A đăng nhập lại và duyệt. Các thông báo lỗi được hiển thị ngay trong khối xác nhận.

### Kiểm thử

```powershell
cd backend
./gradlew.bat test
cd ../frontend
npm ci
npm run lint
npm run build
```

Phần dưới đây là README gốc của dự án nền, giữ lại để đối chiếu chức năng ban đầu.

Campus Hub là dự án nền cho Mini Project Session 13. Ứng dụng cung cấp một không gian chung để sinh viên đăng sự kiện, tìm đồ thất lạc, hỏi đáp, trao đổi đồ dùng và tìm nhóm.

Dự án được giữ ở mức nhỏ để dễ đọc và sửa đổi, nhưng đã có đủ luồng giao diện, API, xác thực, migration và kiểm thử.

## Chức năng có sẵn

- Xem, tìm kiếm và lọc bài đăng.
- Đăng ký, đăng nhập bằng HTTP Basic.
- Tạo bài đăng theo chuyên mục.
- Bình luận và bày tỏ quan tâm.
- Chủ bài đăng có thể đánh dấu bài đã hoàn tất.
- Quản trị viên có quyền quản lý trạng thái bài đăng qua API.
- Health check tại `/actuator/health`.

Phiên bản Mini Project đã loại bỏ tài khoản mẫu và dữ liệu mẫu. Tạo hai tài khoản qua giao diện để trình diễn luồng xác nhận đồ thất lạc.

## Kiến trúc

```text
React frontend
    │ /api
    ▼
Spring Boot monolith
    │ JDBC
    ▼
MySQL
```

Backend gồm năm bảng chính: `users`, `categories`, `posts`, `comments` và `reactions`. Frontend không sử dụng thư viện giao diện hay thư viện quản lý trạng thái để sinh viên dễ đọc mã nguồn.

## Chạy khi phát triển

Tạo database MySQL tên `campus_hub`, sau đó đặt các biến môi trường nếu thông tin kết nối khác giá trị mặc định:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/campus_hub"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-password"
```

Chạy backend:

```powershell
cd backend
./gradlew.bat bootRun
```

Mở terminal khác để chạy frontend:

```powershell
cd frontend
npm install
npm run dev
```

Frontend phát triển chạy tại `http://localhost:5173` và chuyển tiếp `/api` tới backend tại cổng `8080`.

## API chính

| Phương thức | Đường dẫn | Mục đích | Xác thực |
|---|---|---|---|
| `POST` | `/api/auth/register` | Tạo tài khoản | Không |
| `GET` | `/api/auth/me` | Lấy người dùng hiện tại | Có |
| `GET` | `/api/categories` | Danh sách chuyên mục | Không |
| `GET` | `/api/posts` | Tìm và lọc bài đăng | Không |
| `GET` | `/api/posts/{id}` | Chi tiết bài đăng | Không |
| `POST` | `/api/posts` | Tạo bài đăng | Có |
| `PUT` | `/api/posts/{id}` | Sửa bài của mình | Có |
| `PATCH` | `/api/posts/{id}/status` | Đổi trạng thái | Có |
| `DELETE` | `/api/posts/{id}` | Xóa bài của mình | Có |
| `POST` | `/api/posts/{id}/comments` | Thêm bình luận | Có |
| `POST` | `/api/posts/{id}/reaction` | Bật hoặc tắt quan tâm | Có |

Các API cần xác thực sử dụng header HTTP Basic. Trong môi trường công khai, hệ thống phải được đặt sau HTTPS.

## Kiểm tra trước khi bàn giao

```powershell
cd backend
./gradlew.bat test

cd ../frontend
npm ci
npm run lint
npm run build
```

## Phạm vi cố ý để mở

Dự án nền chưa có tải tệp, thông báo thời gian thực, email, phân trang trên giao diện, lịch sử thay đổi, dashboard và quy trình kiểm duyệt hoàn chỉnh. Nhóm có thể phát triển các hướng này, sửa sâu dự án nền hoặc thay thế toàn bộ công nghệ nếu phù hợp với ý tưởng của mình.

Dự án không cung cấp sẵn Dockerfile, Docker Compose, cấu hình web server hoặc quy trình CI/CD. Phân tích hệ thống và lựa chọn toàn bộ phương án xây dựng, đóng gói, triển khai là công việc của từng nhóm trong Mini Project.
