# Chila Ticket Manager

Chila là ứng dụng quản lý dự án và công việc theo nhóm, tích hợp thành viên workspace, lập kế hoạch dự án, quy trình Kanban và Scrum, cộng tác, báo cáo và tự động hóa cơ bản. Dự án tham khảo các quy trình phổ biến của Jira nhưng không nhằm thay thế hoặc tương thích API đầy đủ với Jira.

## Mục lục

- [Tính năng](#tính-năng)
- [Công nghệ](#công-nghệ)
- [Kiến trúc](#kiến-trúc)
- [Yêu cầu](#yêu-cầu)
- [Cài đặt và chạy local](#cài-đặt-và-chạy-local)
- [Cấu hình](#cấu-hình)
- [Các trang trong ứng dụng](#các-trang-trong-ứng-dụng)
- [Tổng quan API](#tổng-quan-api)
- [Cơ sở dữ liệu và migration](#cơ-sở-dữ-liệu-và-migration)
- [Kiểm tra](#kiểm-tra)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Giới hạn hiện tại](#giới-hạn-hiện-tại)
- [Tài liệu thiết kế](#tài-liệu-thiết-kế)

## Tính năng

### Tài khoản và bảo mật

- Đăng ký bằng email và mật khẩu, xác thực email bằng OTP 6 chữ số.
- Đổi mật khẩu cần OTP 6 chữ số gửi đến email của tài khoản đang đăng nhập.
- OTP hết hạn sau 10 phút, cho phép tối đa 5 lần nhập và chờ 60 giây trước khi gửi lại. Mã được lưu dưới dạng hash mật khẩu.
- Mật khẩu được hash bằng BCrypt; JWT truy cập được lưu trong cookie HttpOnly có thời hạn 15 phút; các yêu cầu thay đổi dữ liệu được bảo vệ bằng CSRF.
- Cập nhật tên hiển thị, quản lý bảo mật tài khoản và đăng xuất.

### Workspace, dự án và quyền truy cập

- Tạo và chuyển đổi workspace; chỉnh sửa hoặc xóa workspace.
- Vai trò workspace: `OWNER`, `ADMIN`, `MEMBER`. Người tạo workspace trở thành chủ sở hữu.
- Thêm người dùng Chila hiện có, điều chỉnh vai trò phù hợp, xóa thành viên hoặc tự rời workspace.
- Tạo, chỉnh sửa, lưu trữ và khôi phục dự án mà không xóa lịch sử công việc.
- Backend là nơi xác thực quyền truy cập; dữ liệu workspace và dự án được giới hạn theo thành viên và vai trò.

### Quản lý công việc và lập kế hoạch

- Tạo, chỉnh sửa, tìm kiếm, lọc, sắp xếp, phân trang, lưu trữ và khôi phục issue.
- Loại issue: `TASK`, `BUG`, `STORY`; trạng thái: `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`; độ ưu tiên: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
- Mã issue ổn định theo dự án, ví dụ `APP-42`; số thứ tự được cấp tuần tự và thứ tự issue trong các cột trạng thái được lưu lại.
- Bảng Kanban hỗ trợ kéo thả bằng chuột, thao tác bằng bàn phím, kết hợp nhiều bộ lọc và phân trang riêng cho từng cột.
- Product backlog có thứ tự độc lập và cho phép lập kế hoạch sprint.
- Vòng đời sprint gồm tương lai, đang chạy và đã đóng. Khi hoàn thành sprint, issue đã xong được giữ trong lịch sử; issue chưa xong được chuyển rõ ràng về backlog hoặc sprint tương lai khác.
- Epic có tiến độ theo issue; issue cha/con, liên kết issue có hướng, release và kế hoạch theo ngày đến hạn.
- Tùy chỉnh các chuyển trạng thái workflow của dự án và lưu bộ lọc issue riêng tư hoặc chia sẻ trong workspace.

### Cộng tác và theo dõi

- Bình luận issue, lịch sử hoạt động chỉ ghi thêm và thông báo trong ứng dụng khi được giao issue hoặc có bình luận.
- Dashboard dự án với chỉ số luồng công việc; báo cáo sprint gồm ảnh chụp trạng thái khi hoàn thành, burndown và velocity.
- Quy tắc tự động hóa dự án có thể tạo thông báo khi issue chuyển sang trạng thái đã chọn. Lịch sử thực thi được lưu lại.
- Giao diện responsive, trang hồ sơ và cài đặt workspace, cùng hộp thoại xác nhận trong ứng dụng cho các thao tác nguy hiểm.

## Công nghệ

| Khu vực | Công nghệ |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5, Spring Web, Spring Security, Spring Data JPA, Bean Validation, Spring Mail, Actuator |
| Frontend | React 19, TypeScript 6, React Router 7, Vite 8 |
| Cơ sở dữ liệu | PostgreSQL 17, migration SQL bằng Flyway |
| Kiểm thử tích hợp backend | JUnit/Spring Test, PostgreSQL Testcontainers, Docker |

## Kiến trúc

- Frontend là ứng dụng React đơn trang. Khi phát triển, Vite chuyển tiếp các yêu cầu `/api` đến backend.
- Backend cung cấp REST API phiên bản hóa tại `/api/v1` và chịu trách nhiệm xác thực quyền truy cập cũng như quy tắc nghiệp vụ.
- PostgreSQL được truy cập qua Spring Data JPA. Flyway quản lý thay đổi schema; Hibernate chỉ xác thực schema khi khởi động, không tự tạo hoặc cập nhật schema.
- Đăng nhập dùng JWT trong cookie HttpOnly. Frontend gửi giá trị cookie CSRF riêng trong header `X-XSRF-TOKEN` cho các yêu cầu thay đổi dữ liệu.
- Email được gửi qua SMTP có thể cấu hình. Nếu `CHILA_EMAIL_FROM` để trống, Chila dùng `SPRING_MAIL_USERNAME` làm địa chỉ gửi.

## Yêu cầu

- Java 21
- Node.js tương thích Vite 8 (Node.js 20.19+ hoặc 22.12+)
- npm
- Docker Desktop hoặc Docker Engine có Compose để chạy PostgreSQL local và bộ test tích hợp backend
- Tài khoản SMTP để gửi mã xác thực đăng ký và đổi mật khẩu

## Cài đặt và chạy local

Chạy các lệnh sau từ thư mục gốc dự án bằng PowerShell.

1. Tạo file môi trường cho backend và khóa ký JWT riêng:

   ```powershell
   Copy-Item backend/.env.example backend/.env
   ```

   Chỉnh sửa `backend/.env`. Thay `CHILA_JWT_SECRET` bằng khóa Base64 riêng có ít nhất 32 byte. Cấu hình các biến SMTP ở phần dưới. Không commit file `.env` hoặc dùng thông tin mẫu khi triển khai production.

2. Khởi động PostgreSQL local:

   ```powershell
   docker compose up -d postgres
   docker compose ps
   ```

   Compose mặc định dùng PostgreSQL 17 ở cổng `5432`, database `chila` và thông tin mẫu chỉ dành cho phát triển. Nếu cổng `5432` đang được sử dụng, đặt `POSTGRES_PORT=5433` trong `.env` ở thư mục gốc để Compose dùng cổng đó và đặt cùng cổng trong `backend/.env` cho backend.

3. Mở terminal và chạy backend:

   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

   Backend mặc định lắng nghe cổng `8080`. Flyway tự chạy các migration còn thiếu khi khởi động. Kiểm tra `http://localhost:8080/api/v1/health`; phản hồi thành công:

   ```json
   {"status":"UP","application":"Chila Backend"}
   ```

4. Mở terminal khác và chạy frontend:

   ```powershell
   cd frontend
   npm ci
   npm run dev
   ```

   Mở địa chỉ local Vite hiển thị trong terminal, thường là `http://localhost:5173`.

Nếu chạy backend từ thư mục gốc dự án, ứng dụng vẫn tìm cấu hình `backend/.env` cùng với các đường dẫn `.env` hiện hành.

### Dùng Neon hoặc dịch vụ PostgreSQL khác

Đặt `POSTGRESQL_DB_URL` trong `backend/.env` thành URL PostgreSQL hoặc JDBC PostgreSQL. `SPRING_DATASOURCE_URL` được ưu tiên hơn nếu có giá trị. Thông tin đăng nhập trong URL được sử dụng, trừ khi `SPRING_DATASOURCE_USERNAME` hoặc `SPRING_DATASOURCE_PASSWORD` ghi đè. Giữ URL database ở chế độ riêng tư. Khi không đặt URL cloud, backend dùng PostgreSQL local từ Compose.

### Đổi cổng backend

Chạy backend với `SERVER_PORT=8081` (PowerShell: `$env:SERVER_PORT="8081"`) và đặt `CHILA_API_TARGET=http://localhost:8081` trước khi chạy Vite. Nếu API được host riêng, cấu hình `VITE_API_BASE_URL` ở frontend và đặt `CHILA_CORS_ALLOWED_ORIGIN` ở backend thành chính xác origin của frontend.

## Cấu hình

File mẫu được lưu tại [backend/.env.example](../backend/.env.example). File thật `backend/.env` chỉ dùng local và được Git bỏ qua.

| Biến | Mục đích |
| --- | --- |
| `CHILA_JWT_SECRET` | Khóa ký Base64 riêng; cần ít nhất 32 byte dữ liệu khóa. Bắt buộc. |
| `CHILA_JWT_ISSUER` | Issuer JWT; mặc định `chila-local`. |
| `CHILA_AUTH_COOKIE_SECURE` | Bật cookie `Secure` khi dùng HTTPS; mặc định `false` cho HTTP local. |
| `CHILA_CORS_ALLOWED_ORIGIN` | Origin trình duyệt chính xác được backend cho phép; mặc định `http://localhost:5173`. |
| `POSTGRESQL_DB_URL` | URL Neon/PostgreSQL tùy chọn. |
| `SPRING_DATASOURCE_URL` | JDBC URL tường minh; được ưu tiên hơn `POSTGRESQL_DB_URL`. |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Tùy chọn ghi đè thông tin đăng nhập database. |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` | Cấu hình PostgreSQL Compose local và giá trị kết nối dự phòng của backend. |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | Máy chủ và cổng SMTP (mặc định cổng `587`). |
| `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Thông tin xác thực SMTP. |
| `SPRING_MAIL_SMTP_AUTH`, `SPRING_MAIL_STARTTLS` | Bật xác thực SMTP và STARTTLS; mặc định đều là `true`. |
| `CHILA_EMAIL_FROM` | Địa chỉ người gửi; nếu trống, Chila dùng `SPRING_MAIL_USERNAME`. |
| `SERVER_PORT` | Cổng HTTP backend; mặc định `8080`. |
| `CHILA_API_TARGET` | Đích proxy phát triển của Vite; mặc định `http://localhost:8080`. |
| `VITE_API_BASE_URL` | URL gốc frontend dùng khi backend được host riêng. |

Cần cấu hình SMTP hoạt động để gửi email đăng ký và đổi mật khẩu. Nếu không có mail server, yêu cầu gửi OTP sẽ trả lỗi gửi email và không thể hoàn tất các thao tác tài khoản đó.

## Các trang trong ứng dụng

| Đường dẫn | Trang |
| --- | --- |
| `/login`, `/register` | Đăng nhập và tạo tài khoản có xác thực OTP |
| `/workspaces/:workspaceId/overview` | Tổng quan workspace và công việc gần đây |
| `/workspaces/:workspaceId/projects` | Danh sách dự án |
| `/workspaces/:workspaceId/projects/:projectId` | Các tab issue, board, backlog, sprint, epic, release, báo cáo và cài đặt dự án |
| `/workspaces/:workspaceId/issues` | Issue từ các dự án hiển thị trong workspace |
| `/workspaces/:workspaceId/my-issues` | Issue được giao cho người dùng hiện tại |
| `/workspaces/:workspaceId/issues/:issueReference` | Chi tiết issue, thảo luận, hoạt động và quan hệ |
| `/workspaces/:workspaceId/members` | Thành viên và vai trò workspace |
| `/workspaces/:workspaceId/settings` | Cài đặt workspace và quy trình |
| `/workspaces/:workspaceId/profile`, `/profile` | Hồ sơ tài khoản và bảo mật mật khẩu |

Trang issue của workspace ghép dữ liệu từ các API theo từng dự án ở frontend; hiện chưa có một truy vấn server phân trang toàn workspace. Backend vẫn luôn xác thực quyền truy cập, bất kể giao diện đang hiển thị gì.

## Tổng quan API

Tất cả API dùng tiền tố `/api/v1`. Các nhóm tài nguyên chính:

| Khu vực | Một số route tiêu biểu |
| --- | --- |
| Health và xác thực | `GET /health`, `GET /auth/csrf`, `POST /auth/registration-otp`, `/auth/register`, `/auth/login`, `/auth/logout` |
| Người dùng và thông báo | `/users/me`, `/users/me/password-otp`, `/users/me/password`, `/users/me/notifications`, `/notifications/{id}/read` |
| Workspace và thành viên | `/workspaces`, `/workspaces/{workspaceId}/members` |
| Dự án và issue | `/workspaces/{workspaceId}/projects`, `/projects/{projectId}/issues`, `/issues/{issueId}` |
| Lập kế hoạch | `/projects/{projectId}/backlog`, `/projects/{projectId}/sprints`, `/projects/{projectId}/epics`, `/projects/{projectId}/releases` |
| Workflow và bộ lọc | `/projects/{projectId}/workflow`, `/projects/{projectId}/filters` |
| Cộng tác và báo cáo | `/issues/{issueId}/comments`, `/issues/{issueId}/activities`, `/projects/{projectId}/reports` |
| Tự động hóa | `/projects/{projectId}/automation/rules`, `/projects/{projectId}/automation/executions` |

Yêu cầu thay đổi dữ liệu cần CSRF token hợp lệ. Gọi `GET /api/v1/auth/csrf`, giữ cookie `XSRF-TOKEN` và gửi giá trị cookie trong header `X-XSRF-TOKEN`. Đăng ký cần yêu cầu mã qua `/api/v1/auth/registration-otp`, sau đó gửi `verificationCode` cùng request `/api/v1/auth/register`. Người dùng đã đăng nhập yêu cầu mã đổi mật khẩu tại `/api/v1/users/me/password-otp` và gửi `verificationCode`, `newPassword` đến `/api/v1/users/me/password`.

## Cơ sở dữ liệu và migration

PostgreSQL là cơ sở dữ liệu được hỗ trợ. Flyway migration `V1`–`V12` tạo và mở rộng schema người dùng, workspace, dự án, issue, cộng tác, mã issue/thứ tự, sprint, phân cấp/release, workflow/bộ lọc, báo cáo, tự động hóa và xác thực email. Các file migration ở `backend/src/main/resources/db/migration`. Hibernate chạy ở chế độ `ddl-auto: validate`; không chỉnh sửa migration đã áp dụng để thay đổi schema đã triển khai—hãy tạo migration mới.

## Kiểm tra

Chạy bộ test backend từ thư mục `backend` (cần Docker để Testcontainers chạy PostgreSQL):

```powershell
.\mvnw.cmd test
```

Chạy kiểm tra tĩnh frontend từ thư mục `frontend`:

```powershell
npm run lint
npm run build
```

Frontend hiện chưa có runner kiểm thử component React tự động. Các tương tác quan trọng như kéo thả cũng cần được kiểm tra trên trình duyệt.

## Cấu trúc dự án

```text
backend/       REST API Spring Boot, service nghiệp vụ, migration và integration test
frontend/      Ứng dụng React, TypeScript và Vite
docs/          Tài liệu thiết kế theo phase, quyết định kiến trúc và tài liệu đa ngôn ngữ
docker-compose.yml  Dịch vụ PostgreSQL local
README.md      Tổng quan và hướng dẫn tiếng Anh
```

## Giới hạn hiện tại

- Dự án hiện triển khai một số quy trình quản lý dự án; không có đầy đủ tính năng hoặc khả năng tương thích API của Jira.
- Trang issue workspace tổng hợp kết quả API theo dự án ở frontend, chưa có phân trang server thực sự trên toàn workspace.
- Một số danh sách lập kế hoạch và tổng quan bị giới hạn số lượng kết quả. Tiến độ epic dựa vào trạng thái hiện tại; liên kết issue không tự động chặn chuyển trạng thái workflow. Ngày release là thông tin kế hoạch, không phải lịch Gantt.
- Issue chưa có story point hoặc label. Workflow cho phép cấu hình chuyển trạng thái, chưa cho đổi danh sách trạng thái. Bộ lọc đã lưu chưa có tiêu chí label.
- Tự động hóa hiện hỗ trợ gửi thông báo theo thay đổi trạng thái đến reporter hoặc assignee. Chưa có lịch chạy, webhook đi ra hoặc các hành động tích hợp khác.
- Xác thực chưa có refresh token hoặc thu hồi JWT phía server. Đăng xuất xóa cookie trình duyệt; token đã sao chép vẫn có hiệu lực đến khi hết hạn ngắn.

## Tài liệu thiết kế

- [Phase B: database và xác thực](phase-b-design.md)
- [Phase C: workspace và thành viên](phase-c-design.md)
- [Phase D: dự án](phase-d-design.md)
- [Phase E: issue và quản lý công việc](phase-e-design.md)
- [Phase F: cộng tác và kiểm toán](phase-f-design.md)
- [Phase G: trải nghiệm frontend](phase-g-ux.md)
- [Mã issue, thứ tự và Kanban](issue-keys-ranking-kanban.md)
- [Phase 2: backlog và sprint Scrum](phase-2-design.md)
- [Phase 3: epic, phân cấp và release](phase-3-design.md)
- [Phase 4: workflow và bộ lọc đã lưu](phase-4-design.md)
- [Phase 5: báo cáo và dashboard](phase-5-design.md)
- [Phase 6: tự động hóa dự án](phase-6-design.md)
- [English project guide](../README.md)
