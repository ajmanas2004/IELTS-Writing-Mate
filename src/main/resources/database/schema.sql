-- Database Schema for IELTS Writing Mate
-- SQLite foreign keys must be enabled per connection: PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('STUDENT', 'TEACHER'))
);

CREATE TABLE IF NOT EXISTS writing_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_type TEXT NOT NULL CHECK (task_type IN ('TASK_1', 'TASK_2')),
    prompt TEXT NOT NULL,
    time_limit INTEGER
);

CREATE TABLE IF NOT EXISTS essays (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id INTEGER NOT NULL,
    task_id INTEGER NOT NULL,
    title TEXT,
    content TEXT,
    status TEXT NOT NULL CHECK (status IN ('DRAFT', 'SUBMITTED', 'GRADED')),
    created_at DATETIME NOT NULL,
    submitted_at DATETIME,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (task_id) REFERENCES writing_tasks(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS checklist_responses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    essay_id INTEGER NOT NULL,
    section_name TEXT NOT NULL,
    criterion TEXT NOT NULL,
    answer TEXT NOT NULL CHECK (answer IN ('YES', 'PARTIALLY', 'NO')),
    FOREIGN KEY (essay_id) REFERENCES essays(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS self_assessments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    essay_id INTEGER NOT NULL UNIQUE,
    estimated_band REAL,
    comment TEXT,
    FOREIGN KEY (essay_id) REFERENCES essays(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS teacher_assessments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    essay_id INTEGER NOT NULL UNIQUE,
    teacher_id INTEGER NOT NULL,
    task_score REAL NOT NULL,
    coherence_score REAL NOT NULL,
    lexical_score REAL NOT NULL,
    grammar_score REAL NOT NULL,
    overall_band REAL NOT NULL,
    feedback TEXT,
    graded_at DATETIME NOT NULL,
    FOREIGN KEY (essay_id) REFERENCES essays(id) ON DELETE RESTRICT,
    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS assessment_rubric_evidence (
    essay_id INTEGER NOT NULL,
    criterion_key TEXT NOT NULL,
    opening_covered INTEGER NOT NULL DEFAULT 0,
    body_covered INTEGER NOT NULL DEFAULT 0,
    conclusion_covered INTEGER NOT NULL DEFAULT 0,
    evidence TEXT NOT NULL,
    rationale TEXT NOT NULL,
    PRIMARY KEY (essay_id, criterion_key),
    FOREIGN KEY (essay_id) REFERENCES essays(id) ON DELETE RESTRICT
);
