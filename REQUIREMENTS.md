# IELTS WRITING MATE - Tài liệu yêu cầu phần mềm (Software Requirements)
Phiên bản 1.0 | Ngôn ngữ tài liệu: Tiếng Việt | Thời gian: 06 ngày

## 1. Công nghệ sử dụng
- Ngôn ngữ: Java 21
- Giao diện: Java Swing
- Database: SQLite (file local `ielts_writing_mate.db`)
- Kết nối database: JDBC, dùng `PreparedStatement` cho mọi truy vấn có tham số
- Build & thư viện: Maven
- Kiểm thử: JUnit 5
- Quản lý mã nguồn: Git và GitHub
- Bảo mật mật khẩu: BCrypt hoặc PBKDF2 (không lưu plain text)
- File I/O: Java I/O / NIO (xuất essay ra file TXT, UTF-8)
- Kiến trúc: View → Controller → Service → DAO → Database

### Vai trò người dùng:
- **STUDENT (học sinh):** chọn đề, viết bài, lưu nháp, nộp bài, tự đánh giá bằng checklist, xem điểm và feedback.
- **TEACHER (giáo viên):** xem bài đã nộp, chấm bốn tiêu chí, nhập feedback.

### Luồng nghiệp vụ chính (ưu tiên số 1):
Student chọn đề → viết bài → lưu nháp / nộp bài → làm checklist tự đánh giá → Teacher chấm điểm và phản hồi → Student xem kết quả.

### Ngoài phạm vi (Out of Scope):
- Không tích hợp AI tự động chấm bài hoặc AI sửa bài.
- Không xây dựng web app, mobile app hoặc hệ thống client-server.
- Không có chat, thông báo thời gian thực, quản lý lớp học, biểu đồ nâng cao, ngân hàng đề lớn, thanh toán, đăng ký công khai.
- Không triển khai APT, ASM, Dynamic Proxy hoặc ClassLoader tùy chỉnh.

---

## 2. View – Thiết kế giao diện
Sử dụng hai cửa sổ: `LoginFrame` (JFrame) cho đăng nhập và `MainFrame` (JFrame) chứa menu điều hướng giữa các màn hình bằng `CardLayout`. Menu hiển thị theo role (STUDENT hoặc TEACHER). View không chứa SQL và không chứa business logic; sự kiện người dùng được chuyển cho Controller.

### 2.1. Màn hình đăng nhập – LoginFrame
- Nhập username và password để đăng nhập.
- Điều hướng theo role: STUDENT vào `StudentDashboardPanel`, TEACHER vào `TeacherDashboardPanel`.
- Hiển thị thông báo khi sai thông tin đăng nhập. Không có đăng ký công khai; tài khoản demo tạo sẵn trong DB.
- Đăng xuất (logout) nằm ở menu của `MainFrame`.
- Thành phần: `JTextField` (Username), `JPasswordField` (Password), `JButton` (Login), `JLabel` (Thông báo lỗi).

### 2.2. Màn hình dashboard học sinh – StudentDashboardPanel
- Xem danh sách đề IELTS Writing (Task 1 và Task 2). Chọn một đề để tạo bài viết mới.
- Xem danh sách bài viết của chính mình kèm trạng thái: `DRAFT`, `SUBMITTED`, `GRADED`.
- Mở lại bài nháp để chỉnh sửa; mở bài đã nộp hoặc đã chấm để xem.
- Backup / Restore danh sách essay bằng Java Serialization.
- Thành phần: `JTable` (Danh sách đề), `JTable` (Danh sách bài viết: tiêu đề, loại đề, trạng thái, ngày tạo, ngày nộp), `JButton` (Tạo bài mới, Mở bài, Làm mới, Backup, Restore).

### 2.3. Màn hình viết bài – WritingPanel
- Hiển thị đề bài đã chọn (`JTextArea` read-only).
- Nhập tiêu đề (`JTextField`) và nội dung essay (`JTextArea` trong `JScrollPane`).
- Lưu nháp (`DRAFT`), có thể mở lại và chỉnh sửa.
- Nộp bài (`SUBMITTED`); sau khi nộp, nội dung chuyển sang chế độ chỉ đọc (read-only).
- Xuất essay ra file TXT (UTF-8, hỗ trợ tiếng Việt).
- Mở `ChecklistPanel` để tự đánh giá.
- Thao tác lưu/nộp chạy nền (`SwingWorker` / `ExecutorService`), không làm treo giao diện.
- Thành phần nút: `JButton` (Lưu nháp, Nộp bài, Xuất TXT, Checklist), `JLabel` (Trạng thái bài).

### 2.4. Màn hình checklist tự đánh giá – ChecklistPanel
- Hiển thị checklist theo từng section của bài:
    - **Task 1:** Introduction, Overview, Body Paragraph 1, Body Paragraph 2.
    - **Task 2:** Introduction, Body Paragraph 1, Body Paragraph 2, Conclusion.
- Mỗi mục chọn một trong ba đáp án: `YES` / `PARTIALLY` / `NO`.
- Lưu câu trả lời theo essay và theo section; mở lại phải tải đúng dữ liệu đã lưu.
- Hiển thị phần trăm hoàn thành checklist (`JProgressBar`).
- Nhập self-assessment: band ước tính (`JComboBox`) và nhận xét ngắn (`JTextArea`). Nút `JButton` (Lưu).

### 2.5. Màn hình dashboard giáo viên – TeacherDashboardPanel
- Xem danh sách bài đã được Student nộp (`SUBMITTED`); có thể lọc xem bài đã chấm (`GRADED`).
- Xem thông tin Student, loại đề, ngày nộp. Mở bài để đọc và chấm.
- Thành phần: `JTable` (Student, tiêu đề, loại đề, ngày nộp, trạng thái), `JComboBox` (Lọc trạng thái), `JButton` (Mở bài, Làm mới).

### 2.6. Màn hình chấm bài – AssessmentPanel
- Hiển thị đề bài và nội dung essay (chỉ đọc).
- Chấm bốn tiêu chí: Task Achievement (Task 1) / Task Response (Task 2), Coherence & Cohesion, Lexical Resource, Grammatical Range & Accuracy.
- Mỗi tiêu chí nhập điểm từ 0 đến 9, bước 0.5 (`JSpinner` hoặc `JComboBox`).
- Tự động tính và hiển thị overall band (`JLabel`). Nhập feedback chung (`JTextArea`).
- Lưu kết quả và chuyển essay sang `GRADED`; được chỉnh sửa bản đánh giá trước khi hoàn tất.
- Danh sách tiêu chí hiển thị được sinh từ annotation `@RubricCriterion` bằng Reflection.
- Thành phần nút: `JButton` (Lưu, Hoàn tất chấm).

### 2.7. Màn hình kết quả – ResultPanel
- Student xem bốn điểm tiêu chí, overall band và feedback của giáo viên sau khi bài ở trạng thái `GRADED`.
- Hiển thị kết quả self-assessment (phần trăm checklist, band ước tính) nếu đã thực hiện.
- Bài chưa được chấm: hiển thị trạng thái hiện tại.

### 2.8. Yêu cầu chung về giao diện
- Mọi thao tác lưu / nộp / chấm phải có thông báo thành công hoặc lỗi (`JOptionPane` hoặc `JLabel`).
- Tác vụ nền dùng `SwingWorker` hoặc `ExecutorService`; cập nhật Swing component đúng trên Event Dispatch Thread (EDT).

---

## 3. Service – Xử lý nghiệp vụ

### 3.1. AuthService
- Methods: `login()`, `logout()`, `getCurrentUser()`
- Nghiệp vụ: Username và password không được để trống. So khớp mật khẩu bằng hash (BCrypt/PBKDF2). Role chỉ gồm `STUDENT` hoặc `TEACHER`.

### 3.2. WritingTaskService
- Methods: `getAllTasks()`, `getTaskById()`, `getTasksByType()`

### 3.3. EssayService
- Methods: `createEssay()`, `saveDraft()`, `submitEssay()`, `getEssayById()`, `getEssaysByStudent()`, `getEssaysByStatus()`
- Nghiệp vụ: Student chỉ xem và sửa được bài của chính mình. Chỉ cho phép sửa khi ở trạng thái `DRAFT`. Khi nộp bài, tiêu đề và nội dung không được trống; ghi nhận `submitted_at`. Quy trình trạng thái: `DRAFT` → `SUBMITTED` → `GRADED` (không chuyển ngược hoặc bỏ bước).

### 3.4. ChecklistService
- Methods: `getChecklistTemplate()`, `saveResponses()`, `getResponsesByEssay()`, `calculateCompletionPercent()`, `saveSelfAssessment()`, `getSelfAssessment()`
- Nghiệp vụ: Task 1 (Introduction, Overview, Body Paragraph 1, Body Paragraph 2). Task 2 (Introduction, Body Paragraph 1, Body Paragraph 2, Conclusion). Đáp án: `YES`, `PARTIALLY`, `NO`. Nội dung các mục checklist lưu trong file `resources`, mỗi section tối thiểu 3 mục.

### 3.5. AssessmentService
- Methods: `gradeEssay()`, `updateAssessment()`, `getAssessmentByEssay()`, `calculateOverallBand()`
- Nghiệp vụ: Chỉ Teacher được chấm bài ở trạng thái `SUBMITTED`. Mỗi tiêu chí từ 0 đến 9, bước 0.5; bắt buộc nhập đủ 4 điểm. Khi hoàn tất, lưu đánh giá và chuyển essay sang `GRADED`.
- Overall band = trung bình bốn tiêu chí, làm tròn theo quy tắc band IELTS đến 0.5 (ví dụ: 6.25 → 6.5; 6.75 → 7.0; 6.125 → 6.0).

### 3.6. RubricService
- Methods: `getRubricCriteria()`, `validateCriteria()`
- Nghiệp vụ: Tiêu chí đánh dấu bằng custom annotation `@RubricCriterion` (`Retention.RUNTIME`, `Target` phù hợp) trên các field của `TeacherAssessment`; dùng Reflection đọc metadata để sinh danh sách tiêu chí cho `AssessmentPanel`.

### 3.7. ExportService và BackupService
- Methods: `exportEssayToTxt()`, `backupEssays()`, `restoreEssays()`
- Nghiệp vụ: Dùng Java I/O / NIO (`Path`, `Files`), đọc ghi UTF-8, không hardcode đường dẫn tuyệt đối. Backup / Restore dùng `ObjectOutputStream` / `ObjectInputStream`; class liên quan `implements Serializable`. Bảo vệ critical section bằng cơ chế đồng bộ (`synchronized` / Lock) phù hợp khi ghi file chung.

---

## 4. Entity – Cấu trúc Database
Database: `ielts_writing_mate.db` (SQLite, đường dẫn tương đối, tự khởi tạo nếu chưa tồn tại). Bật `PRAGMA foreign_keys = ON` mỗi khi mở connection; khóa ngoại dùng `ON DELETE RESTRICT`.

### 4.1. Bảng `users`
- `id`: INTEGER PK AUTOINCREMENT
- `username`: TEXT NOT NULL UNIQUE
- `password_hash`: TEXT NOT NULL
- `full_name`: TEXT NOT NULL
- `role`: TEXT NOT NULL CHECK (role IN ('STUDENT', 'TEACHER'))
- Java Entity: `User` (lớp cha), `Student`, `Teacher` (kế thừa `User`) | Enum: `Role`

### 4.2. Bảng `writing_tasks`
- `id`: INTEGER PK AUTOINCREMENT
- `task_type`: TEXT NOT NULL CHECK (task_type IN ('TASK_1', 'TASK_2'))
- `prompt`: TEXT NOT NULL
- `time_limit`: INTEGER
- Java Entity: `WritingTask` | Enum: `TaskType`

### 4.3. Bảng `essays`
- `id`: INTEGER PK AUTOINCREMENT
- `student_id`: INTEGER NOT NULL FK → `users(id)` ON DELETE RESTRICT
- `task_id`: INTEGER NOT NULL FK → `writing_tasks(id)` ON DELETE RESTRICT
- `title`: TEXT
- `content`: TEXT
- `status`: TEXT NOT NULL CHECK (status IN ('DRAFT', 'SUBMITTED', 'GRADED'))
- `created_at`: DATETIME NOT NULL
- `submitted_at`: DATETIME NULL
- Java Entity: `Essay` | Enum: `EssayStatus`

### 4.4. Bảng `checklist_responses`
- `id`: INTEGER PK AUTOINCREMENT
- `essay_id`: INTEGER NOT NULL FK → `essays(id)` ON DELETE RESTRICT
- `section_name`: TEXT NOT NULL
- `criterion`: TEXT NOT NULL
- `answer`: TEXT NOT NULL CHECK (answer IN ('YES', 'PARTIALLY', 'NO'))
- Java Entity: `ChecklistResponse` | Enum: `ChecklistAnswer`

### 4.5. Bảng `self_assessments`
- `id`: INTEGER PK AUTOINCREMENT
- `essay_id`: INTEGER NOT NULL UNIQUE FK → `essays(id)` ON DELETE RESTRICT
- `estimated_band`: REAL NULL
- `comment`: TEXT
- Java Entity: `SelfAssessment`

### 4.6. Bảng `teacher_assessments`
- `id`: INTEGER PK AUTOINCREMENT
- `essay_id`: INTEGER NOT NULL UNIQUE FK → `essays(id)` ON DELETE RESTRICT
- `teacher_id`: INTEGER NOT NULL FK → `users(id)` ON DELETE RESTRICT
- `task_score`: REAL NOT NULL (0–9, bước 0.5)
- `coherence_score`: REAL NOT NULL (0–9, bước 0.5)
- `lexical_score`: REAL NOT NULL (0–9, bước 0.5)
- `grammar_score`: REAL NOT NULL (0–9, bước 0.5)
- `overall_band`: REAL NOT NULL
- `feedback`: TEXT
- `graded_at`: DATETIME NOT NULL
- Java Entity: `TeacherAssessment`

---

## 5. Cấu trúc Source Code
- `com.ieltsmate.Main`
- `com.ieltsmate.view`: `LoginFrame`, `MainFrame`, `StudentDashboardPanel`, `TeacherDashboardPanel`, `WritingPanel`, `ChecklistPanel`, `AssessmentPanel`, `ResultPanel`
- `com.ieltsmate.controller`: `AuthController`, `EssayController`, `AssessmentController`
- `com.ieltsmate.service`: `AuthService`, `WritingTaskService`, `EssayService`, `ChecklistService`, `AssessmentService`, `RubricService`, `ExportService`, `BackupService`
- `com.ieltsmate.dao`: `BaseDAO<T>`, `UserDAO`, `WritingTaskDAO`, `EssayDAO`, `ChecklistResponseDAO`, `SelfAssessmentDAO`, `TeacherAssessmentDAO`
- `com.ieltsmate.entity`: `User`, `Student`, `Teacher`, `WritingTask`, `Essay`, `ChecklistResponse`, `SelfAssessment`, `TeacherAssessment`, `Role`, `TaskType`, `EssayStatus`, `ChecklistAnswer`
- `com.ieltsmate.annotation`: `RubricCriterion`
- `com.ieltsmate.util`: `FileManager`, `PasswordUtil`, `ThreadManager`
- `com.ieltsmate.config`: `DatabaseConnection`
- `src/main/resources/`: `database/schema.sql`, `database/seed.sql`, checklist templates
- `src/test/java/com/ieltsmate/`: `AuthServiceTest`, `AssessmentServiceTest`, `EssayServiceTest`

## 6. Non-functional Requirements – Yêu cầu phi chức năng

### 6.1. Performance
- Ứng dụng khởi động trong thời gian hợp lý.
- Các thao tác đọc và ghi database không làm treo giao diện.
- Tác vụ nền sử dụng `SwingWorker` hoặc `ExecutorService`.

### 6.2. Security
- Không lưu mật khẩu dưới dạng plain text.
- Sử dụng BCrypt hoặc PBKDF2 để xác thực mật khẩu.
- Student chỉ được truy cập và chỉnh sửa bài viết của chính mình.
- Teacher chỉ được thực hiện chức năng chấm bài.
- Sử dụng `PreparedStatement` để hạn chế SQL Injection.

### 6.3. Reliability
- Database được tự động khởi tạo nếu chưa tồn tại.
- Dữ liệu phải được lưu lại sau khi đóng và mở ứng dụng.
- Các thao tác ghi dữ liệu cần xử lý lỗi và thông báo phù hợp.
- Không cho phép chuyển trạng thái essay sai quy trình.

### 6.4. Usability
- Giao diện đơn giản, dễ sử dụng.
- Có thông báo khi lưu, nộp hoặc chấm bài thành công/thất bại.
- Các trường bắt buộc phải được kiểm tra trước khi lưu.
- Các chức năng được hiển thị phù hợp với từng role.

---

## 7. Acceptance Criteria & Testing – Tiêu chí nghiệm thu và kiểm thử

### 7.1. Functional Acceptance Criteria
- AC01: Người dùng đăng nhập đúng thông tin và được điều hướng theo role.
- AC02: Student xem được danh sách đề IELTS Writing Task 1 và Task 2.
- AC03: Student tạo được essay mới.
- AC04: Student lưu và mở lại được bài nháp.
- AC05: Student không thể chỉnh sửa essay sau khi nộp.
- AC06: Student thực hiện và lưu checklist theo từng section.
- AC07: Student lưu được self-assessment.
- AC08: Teacher xem được danh sách essay đã nộp.
- AC09: Teacher chấm được bốn tiêu chí IELTS.
- AC10: Hệ thống tính overall band chính xác.
- AC11: Student xem được điểm và feedback sau khi Teacher hoàn tất chấm.
- AC12: Student xuất được essay thành file TXT UTF-8.
- AC13: Hệ thống hỗ trợ backup và restore essay.
- AC14: Dữ liệu vẫn tồn tại sau khi đóng và mở lại ứng dụng.

### 7.2. Technical Acceptance Criteria
- Sử dụng Java OOP và kế thừa phù hợp.
- Có kiến trúc `View → Controller → Service → DAO`.
- Có Generic `BaseDAO<T>`.
- Có custom annotation `@RubricCriterion` và sử dụng Reflection.
- Có Java Serialization cho backup/restore.
- Có xử lý tác vụ nền bằng `SwingWorker` hoặc `ExecutorService`.
- Có unit test bằng JUnit 5.

### 7.3. Testing
- Kiểm tra đăng nhập đúng và sai thông tin.
- Kiểm tra validation dữ liệu đầu vào.
- Kiểm tra lưu, sửa và nộp essay.
- Kiểm tra quyền truy cập theo role.
- Kiểm tra tính đúng đắn của overall band.
- Kiểm tra dữ liệu sau khi đóng và mở ứng dụng.
- Kiểm tra backup/restore và xuất TXT.

## 8. Dữ liệu demo tối thiểu
- Teacher account: 1
- Student account: 2
- Writing Task 1: 2 đề mẫu
- Writing Task 2: 2 đề mẫu
- Essay mẫu: 3 bài ở các trạng thái khác nhau (DRAFT, SUBMITTED, GRADED)
- Checklist: Có dữ liệu cho cả Task 1 và Task 2