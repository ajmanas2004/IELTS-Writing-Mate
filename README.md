  # IELTS Writing Mate

Ứng dụng desktop Java Swing với giao diện FlatLaf Light/Dark, hỗ trợ học sinh luyện IELTS Writing, tự đánh giá bằng checklist và giáo viên chấm theo bốn tiêu chí IELTS. Dữ liệu được lưu cục bộ bằng SQLite.

## Yêu cầu

- JDK 21
- Apache Maven 3.8 trở lên
- Windows, macOS hoặc Linux có môi trường desktop để chạy giao diện Swing

## Build và chạy

Mở terminal tại thư mục chứa `pom.xml`:

```shell
mvn clean package
java -jar target/ielts-writing-mate-1.0-SNAPSHOT.jar
```

Maven biên dịch với Java release 21, chạy unit test JUnit 5 và đóng gói dependency vào executable JAR. SQLite được khởi tạo và seed dữ liệu demo khi ứng dụng mở lần đầu. File `ielts_writing_mate.db` được tạo trong thư mục làm việc hiện tại; hãy chạy ứng dụng từ thư mục mong muốn để chọn vị trí lưu database.

Giao diện dùng FlatLaf Light mặc định. Để chạy với giao diện tối:

```shell
java -Dieltsmate.theme=dark -jar target/ielts-writing-mate-1.0-SNAPSHOT.jar
```

## Tài khoản demo

Mật khẩu mặc định cho cả ba tài khoản là `123456`.

| Vai trò | Tên đăng nhập | Mật khẩu |
|---|---|---|
| Giáo viên | `teacher1` | `123456` |
| Học sinh | `student1` | `123456` |
| Học sinh | `student2` | `123456` |

Các tài khoản chỉ được tạo khi bảng `users` chưa có dữ liệu. Mật khẩu trong seed được lưu dưới dạng BCrypt hash.

## Chức năng theo vai trò

- **Học sinh:** chọn Task 1/Task 2, tạo và lưu nháp essay, nộp bài, xuất TXT UTF-8, hoàn thành checklist, lưu band tự ước tính/nhận xét, xem kết quả đã chấm, backup và restore essay.
- **Giáo viên:** lọc các bài `SUBMITTED`/`GRADED`, mở bài, chấm bốn tiêu chí theo bước 0.5, xem overall band tính tự động, lưu đánh giá hoặc hoàn tất chấm.
- Rubric chấm tách Task Achievement (Academic Task 1) và Task Response (Task 2), cùng Coherence & Cohesion, Lexical Resource và Grammatical Range & Accuracy. Teacher Rubric Checklist được nhóm theo Introduction, Body paragraphs và Conclusion/Overview; mỗi tiêu chí có band anchors tóm lược 5–8, Evidence và Rationale bắt buộc. Overall Band chỉ hiện sau khi cả bốn tiêu chí có score, indicator được chọn, evidence và rationale. Evidence/rationale được lưu theo essay và tiêu chí.
- Thao tác đọc/ghi dữ liệu và file trên giao diện chạy nền; kết quả được cập nhật trên Swing Event Dispatch Thread.

## Đối chiếu tiêu chí nghiệm thu

| AC | Tiêu chí | Thành phần đáp ứng |
|---|---|---|
| AC01 | Đăng nhập và điều hướng theo role | `LoginFrame`, `AuthController`, `AuthService`, `MainFrame` |
| AC02 | Học sinh xem đề Task 1 và Task 2 | `StudentDashboardPanel`, `WritingTaskService`, `WritingTaskDAO` |
| AC03 | Tạo essay mới | `StudentDashboardPanel`, `EssayController`, `EssayService` |
| AC04 | Lưu và mở lại bản nháp | `WritingPanel`, `EssayService`, `EssayDAO` |
| AC05 | Essay đã nộp không còn sửa được | `EssayService`, `WritingPanel` |
| AC06 | Checklist lưu theo essay/section | `ChecklistPanel`, `ChecklistService`, `ChecklistResponseDAO` |
| AC07 | Lưu self-assessment | `ChecklistPanel`, `ChecklistService`, `SelfAssessmentDAO` |
| AC08 | Giáo viên xem bài đã nộp | `TeacherDashboardPanel`, `EssayService` |
| AC09 | Giáo viên chấm bốn tiêu chí, đối chiếu indicator/descriptor và lưu evidence/rationale | `AssessmentPanel`, `RubricService`, `RubricEvidenceDAO`, `AssessmentService` |
| AC10 | Tính overall band theo quy tắc IELTS | `AssessmentService.calculateOverallBand()` |
| AC11 | Học sinh xem band và feedback | `ResultPanel`, `AssessmentService` |
| AC12 | Xuất essay TXT UTF-8 | `WritingPanel`, `ExportService`, `FileManager` |
| AC13 | Backup/restore essay bằng serialization | `StudentDashboardPanel`, `BackupService`, `FileManager` |
| AC14 | Dữ liệu tồn tại qua các lần mở ứng dụng | SQLite, `DatabaseConnection`, các DAO |

## Kiến trúc

```mermaid
flowchart LR
    V["Swing View<br/>LoginFrame · MainFrame<br/>Student/Teacher Dashboard<br/>Writing · Checklist · Assessment · Result"]
    C["Controller<br/>AuthController · EssayController<br/>AssessmentController"]
    S["Service<br/>Auth · Essay · Checklist<br/>Assessment · Rubric · Export · Backup"]
    D["DAO<br/>BaseDAO&lt;T&gt; · UserDAO · EssayDAO<br/>WritingTaskDAO · Assessment DAOs"]
    DB[("SQLite<br/>ielts_writing_mate.db")]
    U["Utilities<br/>PasswordUtil · FileManager · ThreadManager"]

    V --> C --> S --> D --> DB
    S --> U
    C --> S
```

## ERD

```mermaid
erDiagram
    USERS {
        INTEGER id PK
        TEXT username UK
        TEXT password_hash
        TEXT full_name
        TEXT role
    }
    WRITING_TASKS {
        INTEGER id PK
        TEXT task_type
        TEXT prompt
        INTEGER time_limit
    }
    ESSAYS {
        INTEGER id PK
        INTEGER student_id FK
        INTEGER task_id FK
        TEXT title
        TEXT content
        TEXT status
        DATETIME created_at
        DATETIME submitted_at
    }
    CHECKLIST_RESPONSES {
        INTEGER id PK
        INTEGER essay_id FK
        TEXT section_name
        TEXT criterion
        TEXT answer
    }
    SELF_ASSESSMENTS {
        INTEGER id PK
        INTEGER essay_id FK
        REAL estimated_band
        TEXT comment
    }
    TEACHER_ASSESSMENTS {
        INTEGER id PK
        INTEGER essay_id FK
        INTEGER teacher_id FK
        REAL task_score
        REAL coherence_score
        REAL lexical_score
        REAL grammar_score
        REAL overall_band
        TEXT feedback
        DATETIME graded_at
    }
    ASSESSMENT_RUBRIC_EVIDENCE {
        INTEGER essay_id FK
        TEXT criterion_key
        INTEGER opening_covered
        INTEGER body_covered
        INTEGER conclusion_covered
        TEXT evidence
        TEXT rationale
    }

    USERS ||--o{ ESSAYS : writes
    WRITING_TASKS ||--o{ ESSAYS : contains
    ESSAYS ||--o{ CHECKLIST_RESPONSES : has
    ESSAYS ||--|| SELF_ASSESSMENTS : has
    ESSAYS ||--|| TEACHER_ASSESSMENTS : graded_by
    ESSAYS ||--o{ ASSESSMENT_RUBRIC_EVIDENCE : includes
```

## UML Class

```mermaid
classDiagram
    class User {
        <<abstract-like base entity>>
        -long id
        -String username
        -String passwordHash
        -String fullName
        -Role role
    }
    class Student
    class Teacher
    class Essay
    class WritingTask
    class ChecklistResponse
    class SelfAssessment
    class TeacherAssessment {
        -double taskScore
        -double coherenceScore
        -double lexicalScore
        -double grammarScore
        -double overallBand
    }
    class RubricCriterion {
        <<annotation>>
        +String name()
        +String task1Name()
        +String description()
        +double minScore()
        +double maxScore()
        +double step()
    }
    class BaseDAO~T~ {
        <<generic>>
        +findById(int)
        +findAll()
        +save(T)
        +update(T)
        +delete(int)
    }
    class AuthController
    class EssayController
    class AssessmentController
    class AuthService
    class EssayService
    class AssessmentService
    class ChecklistService
    class RubricService
    class ExportService
    class BackupService

    User <|-- Student
    User <|-- Teacher
    TeacherAssessment ..> RubricCriterion : annotates four score fields
    BaseDAO <|-- UserDAO
    BaseDAO <|-- EssayDAO
    BaseDAO <|-- WritingTaskDAO
    BaseDAO <|-- ChecklistResponseDAO
    BaseDAO <|-- SelfAssessmentDAO
    BaseDAO <|-- TeacherAssessmentDAO
    AuthController --> AuthService
    EssayController --> EssayService
    EssayController --> ChecklistService
    AssessmentController --> AssessmentService
    AssessmentService --> TeacherAssessment
    RubricService ..> RubricCriterion : reads via reflection
    Essay "1" --> "1" WritingTask : task
    Essay "1" --> "*" ChecklistResponse : responses
    Essay "1" --> "0..1" SelfAssessment
    Essay "1" --> "0..1" TeacherAssessment
```

## Kiểm thử

Chạy toàn bộ test:

```shell
mvn test
```

Các test JUnit 5 bao phủ xác thực tài khoản, làm tròn band và rubric annotation, vòng đời essay, quyền sở hữu essay, xuất TXT và backup/restore.