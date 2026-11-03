-- Seed data for IELTS Writing Mate
-- Default password for all sample accounts is: 123456 (hashed with BCrypt)

-- 1. Users (1 Teacher, 2 Students)
INSERT INTO users (id, username, password_hash, full_name, role) VALUES
(1, 'teacher1', '$2a$10$Qz129kM7Z9PIrDGdDNV5X.QqMnYO9so1QdkZrM6PlZG.58dul/Dpu', 'Ms. Sarah Johnson', 'TEACHER'),
(2, 'student1', '$2a$10$Qz129kM7Z9PIrDGdDNV5X.QqMnYO9so1QdkZrM6PlZG.58dul/Dpu', 'Nguyễn Văn Học', 'STUDENT'),
(3, 'student2', '$2a$10$Qz129kM7Z9PIrDGdDNV5X.QqMnYO9so1QdkZrM6PlZG.58dul/Dpu', 'Trần Thị Mai', 'STUDENT');

-- 2. Writing Tasks (2 Task 1, 2 Task 2)
INSERT INTO writing_tasks (id, task_type, prompt, time_limit) VALUES
(1, 'TASK_1', 'The line graph below shows the percentage of households in different income groups having internet access from 2010 to 2020. Summarize the information by selecting and reporting the main features, and make comparisons where relevant.', 20),
(2, 'TASK_1', 'The bar chart illustrates the amount of waste produced by three different companies between 2000 and 2015. Summarize the information by selecting and reporting the main features, and make comparisons where relevant.', 20),
(3, 'TASK_2', 'Some people think that universities should provide graduates with the knowledge and skills needed in the workplace. Others think that the true function of a university should be to give access to knowledge for its own sake, regardless of whether the course is useful to an employer. Discuss both views and give your opinion.', 40),
(4, 'TASK_2', 'In many parts of the world, people are living longer. What problems does this cause for individuals and society? What measures can be taken to tackle these problems?', 40);

-- 3. Sample Essays for student1 (student_id = 2)
-- Essay 1: DRAFT
INSERT INTO essays (id, student_id, task_id, title, content, status, created_at, submitted_at) VALUES
(1, 2, 1, 'Draft: Household Internet Access Analysis', 'The line graph illustrates the proportion of households across various income categories that had access to the internet between 2010 and 2020.

Overall, it is evident that internet accessibility experienced an upward trend across all income brackets throughout the decade. High-income households consistently maintained the highest penetration rates, while low-income groups showed the most accelerated growth towards the end of the period.', 'DRAFT', '2026-10-25 10:00:00', NULL);

-- Essay 2: SUBMITTED
INSERT INTO essays (id, student_id, task_id, title, content, status, created_at, submitted_at) VALUES
(2, 2, 2, 'Report on Industrial Waste Production (2000-2015)', 'The bar chart compares three enterprises in terms of their waste generation over a 15-year period starting from 2000.

Overall, while Company A and Company B managed to reduce their annual waste production significantly, Company C exhibited an opposite trend with steady increases throughout the recorded period.

In 2000, Company A was the largest producer of waste at approximately 12 tonnes. However, this figure dropped consistently to under 8 tonnes by 2015. Similarly, Company B cut down its waste from 8 tonnes to roughly 3 tonnes over the same timeframe.

In contrast, Company C produced only 4 tonnes of waste in 2000, but this amount increased continuously, reaching a peak of around 10 tonnes in 2015, surpassing both other companies.', 'SUBMITTED', '2026-10-26 14:00:00', '2026-10-26 14:35:00');

-- Essay 3: GRADED
INSERT INTO essays (id, student_id, task_id, title, content, status, created_at, submitted_at) VALUES
(3, 2, 3, 'The Primary Function of Higher Education', 'Whether universities should prepare graduates primarily for future employment or concentrate on intellectual advancement for its own sake is a matter of debate. In my view, while preparing students for the labour market is crucial, providing pure academic knowledge remains an indispensable role of higher education.

On the one hand, proponents of career-focused education argue that tertiary institutions must adapt to current market demands. High tuition fees mean students expect their investment to yield tangible returns, namely high-paying employment after graduation. In addition, modern economies rely heavily on specialized workforces such as software engineers, medical professionals, and accountants. If universities fail to equip students with practical skills, graduates may face high unemployment rates, and national economic competitiveness could suffer.

On the other hand, focusing solely on vocation could undermine the fundamental essence of academia. Universities have historically been centres of critical thinking, research, and philosophical discovery. Many groundbreaking innovations originate from theoretical research rather than immediate vocational objectives. Furthermore, fostering critical thinking and intellectual inquiry enables graduates to adapt to rapid changes in any professional domain, rather than becoming obsolete when specific job skills evolve.

In conclusion, although university curricula should undoubtedly incorporate practical skills to facilitate employment, the preservation of broad theoretical knowledge and intellectual exploration is equally vital.', 'GRADED', '2026-10-27 09:00:00', '2026-10-27 09:40:00');

-- Teacher assessment for essay 3
INSERT INTO teacher_assessments (
    essay_id, teacher_id, task_score, coherence_score, lexical_score, grammar_score,
    overall_band, feedback, graded_at
) VALUES (
    3, 1, 7.5, 7.0, 7.5, 7.0, 7.5,
    '[Mở bài] The introduction clearly paraphrases the question and gives a balanced position. It sets up the argument effectively and introduces the key issue of higher education.\n\n[Thân bài] The body paragraphs develop the argument with relevant examples and logical progression. The essay contrasts career-focused education with broader academic goals in a persuasive and coherent way.\n\n[Kết bài] The conclusion restates the main idea and reinforces the balanced view. It is concise but complete.\n\n[Feedback chung] Overall, this is a well-structured and persuasive response with strong organisation and clear argumentation.',
    '2026-10-27 10:00:00'
);
