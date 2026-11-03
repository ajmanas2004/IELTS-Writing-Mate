      package com.ieltsmate.view;

import com.ieltsmate.annotation.RubricCriterion;
import com.ieltsmate.controller.AssessmentController;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.EssayStatus;
import com.ieltsmate.entity.RubricEvidence;
import com.ieltsmate.entity.TaskType;
import com.ieltsmate.entity.TeacherAssessment;
import com.ieltsmate.entity.WritingTask;
import com.ieltsmate.service.RubricService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AssessmentPanel extends JPanel {
    private static final String TAG_OPEN = "[Mở bài]";
    private static final String TAG_BODY = "[Thân bài]";
    private static final String TAG_CONCLUSION = "[Kết bài]";
    private static final String TAG_FEEDBACK = "[Feedback chung]";
    private static final Color PRIMARY = uiColor("Component.focusColor", new Color(0x2563EB));
    private static final Color CARD_BORDER = uiColor("Component.borderColor", new Color(0xDCE3ED));
    private static final Color MUTED = uiColor("Label.disabledForeground", new Color(0x64748B));

    private final AssessmentController assessmentController;
    private final Essay essay;
    private final WritingTask task;
    private final Runnable onComplete;
    private final TeacherAssessment assessment;
    private final Map<String, CheckboxGroup> criterionCheckboxes = new HashMap<>();
    private final Map<String, JComboBox<Double>> scoreCombos = new HashMap<>();
    private final Map<String, String> criterionNames = new HashMap<>();

    private final JTextArea introCommentArea = new JTextArea(3, 50);
    private final JTextArea bodyCommentArea = new JTextArea(4, 50);
    private final JTextArea conclusionCommentArea = new JTextArea(3, 50);
    private final JTextArea feedbackArea = new JTextArea(3, 50);

    private final JLabel overallBand = new JLabel("Overall band · chọn điểm 4 tiêu chí");
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton saveButton = new JButton("Lưu đánh giá");
    private final JButton completeButton = new JButton("Hoàn tất chấm");
    private boolean evidenceLoaded;

    public AssessmentPanel(AssessmentController assessmentController,
                           Essay essay,
                           WritingTask task,
                           TeacherAssessment existingAssessment,
                           Runnable onComplete) {
        this.assessmentController = Objects.requireNonNull(assessmentController);
        this.essay = Objects.requireNonNull(essay);
        this.task = Objects.requireNonNull(task);
        this.onComplete = Objects.requireNonNull(onComplete);
        if (essay.getStatus() != EssayStatus.SUBMITTED && essay.getStatus() != EssayStatus.GRADED) {
            throw new IllegalArgumentException("Only submitted or graded essays can be assessed");
        }
        assessment = existingAssessment == null
                ? new TeacherAssessment(0, essay.getId(), 0, 0, 0, 0, 0, 0, "", null)
                : existingAssessment;
        buildUi();
        loadEvidence();
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, 0));

        JPanel page = new JPanel(new GridBagLayout());
        page.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        int row = 0;

        // 1. Header
        addPageRow(page, buildHeader(), row++, 0);

        // 2. Reading pane (Prompt & Student Essay)
        JPanel readingPane = new JPanel(new GridLayout(1, 2, 12, 0));
        readingPane.add(buildReadingCard("ĐỀ BÀI", task.getPrompt()));
        readingPane.add(buildReadingCard("BÀI VIẾT · " + safeTitle(), essay.getContent()));
        addPageRow(page, readingPane, row++, 12);

        // 3. Checklist nhanh gọn ở phần trên
        JPanel checklistSection = new JPanel(new BorderLayout(8, 8));
        checklistSection.setBorder(BorderFactory.createTitledBorder("Teacher Rubric Checklist"));
        checklistSection.add(buildChecklist(), BorderLayout.CENTER);
        addPageRow(page, checklistSection, row++, 12);

        // 4. 3 Box riêng biệt theo chiều dọc (Introduction, Body Paragraphs, Conclusion)
        JPanel structureSection = buildStructureBoxes();
        addPageRow(page, structureSection, row++, 12);

        // 5. Thanh nhập điểm 4 tiêu chí IELTS
        JPanel scoringSection = buildScoringSection();
        addPageRow(page, scoringSection, row++, 12);

        // 6. Feedback tổng quan
        JPanel feedbackSection = buildFeedbackSection();
        addPageRow(page, feedbackSection, row++, 12);

        // JScrollPane cho toàn bộ phần nội dung tiêu chí và các box chấm bài
        JScrollPane pageScroll = new JScrollPane(page,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pageScroll.setWheelScrollingEnabled(true);
        pageScroll.getVerticalScrollBar().setUnitIncrement(20);
        pageScroll.getViewport().setViewPosition(new java.awt.Point(0, 0));
        pageScroll.setBorder(BorderFactory.createEmptyBorder());
        add(pageScroll, BorderLayout.CENTER);

        // Panel chức năng cố định ở miền SOUTH
        add(buildActionBar(), BorderLayout.SOUTH);

        saveButton.addActionListener(event -> saveAssessment());
        completeButton.addActionListener(event -> finishGrading());
        styleButton(saveButton, false);
        styleButton(completeButton, true);
        completeButton.setEnabled(essay.getStatus() == EssayStatus.SUBMITTED);
        updateOverallBand();
    }

    private static void addPageRow(JPanel page, java.awt.Component component,
                                   int row, int topInset) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(topInset, 0, 0, 0);
        page.add(component, constraints);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 4));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(task.getTaskType() == TaskType.TASK_1
                ? "Academic Writing · Task 1" : "Academic Writing · Task 2");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 22f));
        JLabel subtitle = new JLabel("Chấm bài " + safeTitle()
                + "  ·  " + (essay.getStatus() == EssayStatus.GRADED ? "Đã chấm" : "Chờ chấm"));
        subtitle.setForeground(MUTED);
        titles.add(title);
        titles.add(Box.createVerticalStrut(4));
        titles.add(subtitle);
        header.add(titles, BorderLayout.WEST);
        return header;
    }

    private JPanel buildReadingCard(String heading, String text) {
        JPanel card = new JPanel(new BorderLayout(4, 6));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel label = new JLabel(heading);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        label.setForeground(MUTED);
        JTextArea area = new JTextArea(text == null ? "" : text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(new Insets(8, 8, 8, 8));
        card.add(label, BorderLayout.NORTH);
        JScrollPane textScroll = new JScrollPane(area);
        textScroll.setBorder(BorderFactory.createEmptyBorder());
        textScroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(textScroll, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(500, 155));
        card.setMinimumSize(new Dimension(0, 130));
        return card;
    }

    private JPanel buildChecklist() {
        RubricService rubricService = new RubricService();
        if (!rubricService.validateCriteria()) {
            throw new IllegalStateException("Rubric criteria are missing or invalid");
        }

        for (RubricService.RubricField rubricField : rubricService.getRubricCriteria()) {
            RubricService.CriterionGuidance guidance =
                    rubricService.getGuidance(task.getTaskType(), rubricField.fieldName());
            criterionNames.put(rubricField.fieldName(), guidance.displayName());

            JCheckBox openingBox = new JCheckBox();
            JCheckBox bodyBox = new JCheckBox();
            JCheckBox conclusionBox = new JCheckBox();

            openingBox.addActionListener(event -> updateOverallBand());
            bodyBox.addActionListener(event -> updateOverallBand());
            conclusionBox.addActionListener(event -> updateOverallBand());

            criterionCheckboxes.put(rubricField.fieldName(),
                    new CheckboxGroup(openingBox, bodyBox, conclusionBox));
        }

        JPanel page = new JPanel(new BorderLayout(14, 10));
        page.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel instruction = new JLabel("Đánh dấu các tiêu chí đạt được tại từng phần của bài viết.");
        instruction.setForeground(MUTED);
        page.add(instruction, BorderLayout.NORTH);

        JPanel sections = new JPanel(new GridLayout(1, 3, 14, 0));
        sections.add(buildChecklistSectionCard("1  ·  INTRODUCTION", "opening",
                task.getTaskType() == TaskType.TASK_1
                        ? "Diễn đạt lại đề bài và xác định biểu đồ/đối tượng."
                        : "Diễn đạt lại chủ đề và nêu lập trường/định hướng trả lời."));
        sections.add(buildChecklistSectionCard("2  ·  BODY PARAGRAPHS", "body",
                "Phát triển thông tin hoặc luận điểm, liên kết mạch lạc và dùng ngôn ngữ chính xác."));
        sections.add(buildChecklistSectionCard(
                task.getTaskType() == TaskType.TASK_1
                        ? "3  ·  OVERVIEW / CONCLUSION"
                        : "3  ·  CONCLUSION",
                "conclusion",
                task.getTaskType() == TaskType.TASK_1
                        ? "Chọn và tóm tắt xu hướng/đặc điểm nổi bật."
                        : "Tổng hợp câu trả lời nhất quán, không đưa luận điểm mới."));
        page.add(sections, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildChecklistSectionCard(String heading, String section, String description) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel title = new JLabel("<html><b>" + heading + "</b></html>");
        title.setForeground(PRIMARY);
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel detail = new JLabel("<html><div style='width:220px'>" + escapeHtml(description) + "</div></html>");
        detail.setForeground(MUTED);
        detail.setAlignmentX(LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(detail);
        card.add(Box.createVerticalStrut(10));

        for (String criterionKey : List.of("taskScore", "coherenceScore", "lexicalScore", "grammarScore")) {
            CheckboxGroup group = criterionCheckboxes.get(criterionKey);
            if (group != null) {
                JCheckBox checkBox = switch (section) {
                    case "opening" -> group.opening();
                    case "body" -> group.body();
                    case "conclusion" -> group.conclusion();
                    default -> throw new IllegalArgumentException("Unknown rubric section");
                };
                checkBox.setText(criterionNames.get(criterionKey));
                checkBox.setAlignmentX(LEFT_ALIGNMENT);
                card.add(checkBox);
                card.add(Box.createVerticalStrut(6));
            }
        }
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel buildStructureBoxes() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Nhận xét theo cấu trúc bài viết"));

        panel.add(buildCommentBox("1. Introduction (Mở bài)",
                task.getTaskType() == TaskType.TASK_1
                        ? "Nhận xét về cách Mở bài (paraphrase đề bài, giới thiệu biểu đồ/đối tượng...)"
                        : "Nhận xét về cách Mở bài (paraphrase đề bài, thesis statement, lập trường...)",
                introCommentArea, 85));
        panel.add(Box.createVerticalStrut(10));

        panel.add(buildCommentBox("2. Body Paragraphs (Thân bài)",
                "Nhận xét các đoạn Thân bài (sự phát triển ý, dữ liệu/luận điểm, từ vựng, ngữ pháp, độ mạch lạc...)",
                bodyCommentArea, 105));
        panel.add(Box.createVerticalStrut(10));

        panel.add(buildCommentBox(
                task.getTaskType() == TaskType.TASK_1
                        ? "3. Overview / Conclusion (Tổng quan / Kết bài)"
                        : "3. Conclusion (Kết bài)",
                task.getTaskType() == TaskType.TASK_1
                        ? "Nhận xét đoạn Overview (tóm tắt xu hướng chính, các điểm nổi bật...)"
                        : "Nhận xét đoạn Kết bài (khẳng định lại lập trường, tóm tắt các ý chính...)",
                conclusionCommentArea, 85));

        return panel;
    }

    private JPanel buildCommentBox(String titleText, String descriptionText, JTextArea area, int height) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(titleText);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        title.setForeground(PRIMARY);
        JLabel desc = new JLabel(descriptionText);
        desc.setForeground(MUTED);
        header.add(title);
        header.add(Box.createVerticalStrut(2));
        header.add(desc);

        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(new Insets(6, 8, 6, 8));
        area.getDocument().addDocumentListener(new DocumentChangeListener(this::updateOverallBand));

        JScrollPane areaScroll = new JScrollPane(area);
        areaScroll.setPreferredSize(new Dimension(600, height));
        areaScroll.setMinimumSize(new Dimension(300, 60));

        card.add(header, BorderLayout.NORTH);
        card.add(areaScroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFeedbackSection() {
        JPanel section = new JPanel(new BorderLayout(8, 8));
        section.setBorder(BorderFactory.createTitledBorder("Feedback chung"));
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        feedbackArea.setMargin(new Insets(8, 10, 8, 10));
        feedbackArea.setText(assessment.getFeedback() == null ? "" : assessment.getFeedback());
        JScrollPane feedbackScroll = new JScrollPane(feedbackArea);
        feedbackScroll.setPreferredSize(new Dimension(700, 110));
        feedbackScroll.setMinimumSize(new Dimension(250, 80));
        section.add(feedbackScroll, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildActionBar() {
        JPanel actionBar = new JPanel(new BorderLayout(12, 0));
        actionBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JPanel summary = new JPanel();
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        overallBand.setFont(overallBand.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        overallBand.setForeground(PRIMARY);
        summary.add(overallBand);
        summary.add(Box.createVerticalStrut(4));
        summary.add(statusLabel);
        actionBar.add(summary, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(saveButton);
        actions.add(completeButton);
        actionBar.add(actions, BorderLayout.EAST);
        return actionBar;
    }

    private JPanel buildScoringSection() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Điểm 4 tiêu chí IELTS"));

        JPanel scoreGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        scoreGrid.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        RubricService rubricService = new RubricService();
        for (RubricService.RubricField rubricField : rubricService.getRubricCriteria()) {
            Field field;
            try {
                field = TeacherAssessment.class.getDeclaredField(rubricField.fieldName());
            } catch (NoSuchFieldException exception) {
                throw new IllegalStateException("Rubric score field is missing", exception);
            }
            RubricCriterion metadata = rubricField.annotation();
            RubricService.CriterionGuidance guidance =
                    rubricService.getGuidance(task.getTaskType(), rubricField.fieldName());

            Double[] values = createScoreValues(metadata);
            JComboBox<Double> combo = new JComboBox<>(values);
            combo.setPreferredSize(new Dimension(100, 34));

            double currentVal = getAssessmentScore(field.getName());
            combo.setSelectedItem(currentVal);
            combo.addActionListener(event -> updateOverallBand());
            scoreCombos.put(rubricField.fieldName(), combo);

            JPanel item = new JPanel();
            item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
            JLabel label = new JLabel(guidance.displayName());
            label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 12f));
            label.setAlignmentX(LEFT_ALIGNMENT);
            combo.setAlignmentX(LEFT_ALIGNMENT);

            item.add(label);
            item.add(Box.createVerticalStrut(4));
            item.add(combo);
            scoreGrid.add(item);
        }

        panel.add(scoreGrid, BorderLayout.CENTER);
        return panel;
    }

    private double getAssessmentScore(String fieldName) {
        return switch (fieldName) {
            case "taskScore" -> assessment.getTaskScore();
            case "coherenceScore" -> assessment.getCoherenceScore();
            case "lexicalScore" -> assessment.getLexicalScore();
            case "grammarScore" -> assessment.getGrammarScore();
            default -> 0.0;
        };
    }

    private void loadEvidence() {
        setBusy(true, "Đang tải rubric...");
        new SwingWorker<List<RubricEvidence>, Void>() {
            @Override
            protected List<RubricEvidence> doInBackground() throws Exception {
                return assessmentController.getRubricEvidence(essay.getId());
            }

            @Override
            protected void done() {
                try {
                    List<RubricEvidence> evidenceList = get();
                    for (RubricEvidence evidence : evidenceList) {
                        CheckboxGroup group = criterionCheckboxes.get(evidence.criterionKey());
                        if (group != null) {
                            group.opening().setSelected(evidence.openingCovered());
                            group.body().setSelected(evidence.bodyCovered());
                            group.conclusion().setSelected(evidence.conclusionCovered());
                        }
                    }
                    parseCommentsFromEvidence(evidenceList);
                    evidenceLoaded = true;
                    setBusy(false, "Sẵn sàng chấm");
                    updateOverallBand();
                } catch (Exception exception) {
                    setBusy(false, "Không tải được rubric");
                    showError("Không thể tải rubric evidence", exception);
                }
            }
        }.execute();
    }

    private void parseCommentsFromEvidence(List<RubricEvidence> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (RubricEvidence ev : list) {
            String text = ev.evidence();
            if (text == null || text.isBlank()) {
                continue;
            }
            if (text.contains(TAG_OPEN) || text.contains(TAG_BODY) || text.contains(TAG_CONCLUSION)) {
                if (introCommentArea.getText().isBlank() && text.contains(TAG_OPEN)) {
                    introCommentArea.setText(extractPart(text, TAG_OPEN, TAG_BODY, TAG_CONCLUSION));
                }
                if (bodyCommentArea.getText().isBlank() && text.contains(TAG_BODY)) {
                    bodyCommentArea.setText(extractPart(text, TAG_BODY, TAG_CONCLUSION, null));
                }
                if (conclusionCommentArea.getText().isBlank() && text.contains(TAG_CONCLUSION)) {
                    conclusionCommentArea.setText(extractPart(text, TAG_CONCLUSION, null, null));
                }
            }
        }
    }

    private String extractPart(String full, String startTag, String endTag1, String endTag2) {
        int start = full.indexOf(startTag);
        if (start == -1) return "";
        start += startTag.length();
        int end = full.length();
        if (endTag1 != null && full.indexOf(endTag1, start) != -1) {
            end = Math.min(end, full.indexOf(endTag1, start));
        }
        if (endTag2 != null && full.indexOf(endTag2, start) != -1) {
            end = Math.min(end, full.indexOf(endTag2, start));
        }
        return full.substring(start, end).strip();
    }

    private void saveAssessment() {
        TeacherAssessment values;
        List<RubricEvidence> evidence;
        try {
            evidence = readEvidence();
            values = readAssessment();
        } catch (RuntimeException exception) {
            showError("Hoàn thiện checklist và đánh giá", exception);
            return;
        }
        boolean alreadyGraded = essay.getStatus() == EssayStatus.GRADED;
        runInBackground("Đang lưu đánh giá...", () -> {
            assessmentController.saveRubricEvidence(essay.getId(), evidence);
            return alreadyGraded
                    ? assessmentController.updateAssessment(values)
                    : assessmentController.saveAssessmentDraft(values);
        }, false);
    }

    private void finishGrading() {
        if (essay.getStatus() != EssayStatus.SUBMITTED) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this,
                "Hoàn tất chấm và chuyển trạng thái bài viết sang GRADED?",
                "Xác nhận hoàn tất", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        TeacherAssessment values;
        List<RubricEvidence> evidence;
        try {
            evidence = readEvidence();
            values = readAssessment();
        } catch (RuntimeException exception) {
            showError("Hoàn thiện checklist và đánh giá", exception);
            return;
        }
        runInBackground("Đang hoàn tất chấm...", () -> {
            assessmentController.saveRubricEvidence(essay.getId(), evidence);
            return assessmentController.gradeEssay(values);
        }, true);
    }

    private void runInBackground(String workingText,
                                 AssessmentOperation operation,
                                 boolean completed) {
        setBusy(true, workingText);
        new SwingWorker<TeacherAssessment, Void>() {
            @Override
            protected TeacherAssessment doInBackground() throws Exception {
                return operation.run();
            }

            @Override
            protected void done() {
                setBusy(false, " ");
                try {
                    copyAssessment(get());
                    statusLabel.setText(completed ? "Đã hoàn tất chấm." : "Đã lưu đánh giá.");
                    if (completed) {
                        JOptionPane.showMessageDialog(AssessmentPanel.this,
                                "Đã lưu evidence, rubric và hoàn tất chấm bài.",
                                "IELTS Writing Mate", JOptionPane.INFORMATION_MESSAGE);
                        onComplete.run();
                    } else {
                        JOptionPane.showMessageDialog(AssessmentPanel.this,
                                "Đã lưu assessment cùng evidence và rationale.",
                                "IELTS Writing Mate", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception exception) {
                    showError("Không thể lưu đánh giá", exception);
                    statusLabel.setText("Lưu đánh giá thất bại.");
                }
            }
        }.execute();
    }

    private TeacherAssessment readAssessment() {
        double taskScore = (Double) scoreCombos.get("taskScore").getSelectedItem();
        double coherenceScore = (Double) scoreCombos.get("coherenceScore").getSelectedItem();
        double lexicalScore = (Double) scoreCombos.get("lexicalScore").getSelectedItem();
        double grammarScore = (Double) scoreCombos.get("grammarScore").getSelectedItem();

        assessment.setTaskScore(taskScore);
        assessment.setCoherenceScore(coherenceScore);
        assessment.setLexicalScore(lexicalScore);
        assessment.setGrammarScore(grammarScore);
        assessment.setEssayId(essay.getId());

        String fb = feedbackArea.getText().strip();
        if (fb.isBlank()) {
            List<String> secFb = new ArrayList<>();
            if (!introCommentArea.getText().isBlank()) secFb.add("[Mở bài] " + introCommentArea.getText().strip());
            if (!bodyCommentArea.getText().isBlank()) secFb.add("[Thân bài] " + bodyCommentArea.getText().strip());
            if (!conclusionCommentArea.getText().isBlank()) secFb.add("[Kết bài] " + conclusionCommentArea.getText().strip());
            fb = String.join("\n\n", secFb);
        }
        assessment.setFeedback(fb);
        assessment.setGradedAt(LocalDateTime.now());
        assessment.setOverallBand(assessmentController.calculateOverallBand(
                taskScore, coherenceScore, lexicalScore, grammarScore));
        return assessment;
    }

    private List<RubricEvidence> readEvidence() {
        if (!evidenceLoaded) {
            throw new IllegalStateException("Rubric evidence is still loading");
        }
        List<RubricEvidence> evidenceList = new ArrayList<>();
        String introText = introCommentArea.getText().strip();
        String bodyText = bodyCommentArea.getText().strip();
        String conclusionText = conclusionCommentArea.getText().strip();

        for (String criterionKey : List.of("taskScore", "coherenceScore", "lexicalScore", "grammarScore")) {
            CheckboxGroup group = criterionCheckboxes.get(criterionKey);
            boolean op = group != null && group.opening().isSelected();
            boolean bo = group != null && group.body().isSelected();
            boolean co = group != null && group.conclusion().isSelected();

            StringBuilder evSb = new StringBuilder();
            if (!introText.isBlank()) evSb.append(TAG_OPEN).append(" ").append(introText).append(" ");
            if (!bodyText.isBlank()) evSb.append(TAG_BODY).append(" ").append(bodyText).append(" ");
            if (!conclusionText.isBlank()) evSb.append(TAG_CONCLUSION).append(" ").append(conclusionText).append(" ");

            String ev = evSb.toString().strip();
            if (ev.isBlank()) {
                ev = "";
            }

            String rat = !feedbackArea.getText().isBlank()
                    ? feedbackArea.getText().strip()
                    : !ev.isBlank() ? ev : "";

            evidenceList.add(new RubricEvidence(criterionKey, op, bo, co, ev, rat));
        }
        return List.copyOf(evidenceList);
    }

    private void updateOverallBand() {
        if (!evidenceLoaded || !isEvidenceComplete()) {
            overallBand.setText("Overall band · tích chọn checklist để hoàn tất");
            return;
        }
        try {
            TeacherAssessment values = readAssessment();
            overallBand.setText(String.format(Locale.ROOT,
                    "Overall band  %.1f", values.getOverallBand()));
        } catch (RuntimeException exception) {
            overallBand.setText("Overall band · kiểm tra lại điểm");
        }
    }

    private boolean isEvidenceComplete() {
        return true;
    }

    private void copyAssessment(TeacherAssessment saved) {
        assessment.setId(saved.getId());
        assessment.setTeacherId(saved.getTeacherId());
        assessment.setTaskScore(saved.getTaskScore());
        assessment.setCoherenceScore(saved.getCoherenceScore());
        assessment.setLexicalScore(saved.getLexicalScore());
        assessment.setGrammarScore(saved.getGrammarScore());
        assessment.setOverallBand(saved.getOverallBand());
        assessment.setFeedback(saved.getFeedback());
        assessment.setGradedAt(saved.getGradedAt());
    }

    private void setBusy(boolean busy, String message) {
        statusLabel.setText(message);
        saveButton.setEnabled(!busy && evidenceLoaded);
        completeButton.setEnabled(!busy && evidenceLoaded
                && essay.getStatus() == EssayStatus.SUBMITTED);
        criterionCheckboxes.values().forEach(group -> {
            group.opening().setEnabled(!busy);
            group.body().setEnabled(!busy);
            group.conclusion().setEnabled(!busy);
        });
        scoreCombos.values().forEach(combo -> combo.setEnabled(!busy));
        introCommentArea.setEnabled(!busy);
        bodyCommentArea.setEnabled(!busy);
        conclusionCommentArea.setEnabled(!busy);
        feedbackArea.setEnabled(!busy);
    }

    private void showError(String action, Exception exception) {
        Throwable cause = exception.getCause() instanceof InvocationTargetException invocation
                ? invocation.getCause() : exception.getCause();
        if (cause == null) {
            cause = exception;
        }
        JOptionPane.showMessageDialog(this, action + ": " + cause.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private String safeTitle() {
        return essay.getTitle() == null || essay.getTitle().isBlank()
                ? "Essay #" + essay.getId() : essay.getTitle();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static Color uiColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color == null ? fallback : color;
    }

    private static void styleButton(JButton button, boolean primary) {
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setMargin(new Insets(9, 16, 9, 16));
        if (primary) {
            button.setBackground(PRIMARY);
            button.setForeground(Color.WHITE);
            button.setFont(button.getFont().deriveFont(java.awt.Font.BOLD));
            button.setToolTipText("Lưu rubric và chuyển bài sang trạng thái đã chấm");
        }
    }

    private static Double[] createScoreValues(RubricCriterion criterion) {
        int count = (int) Math.round((criterion.maxScore() - criterion.minScore()) / criterion.step());
        Double[] values = new Double[count + 1];
        for (int index = 0; index <= count; index++) {
            values[index] = criterion.minScore() + index * criterion.step();
        }
        return values;
    }

    private static ScoreInput createScoreInput(Field field, JComboBox<Double> comboBox) {
        try {
            String suffix = Character.toUpperCase(field.getName().charAt(0))
                    + field.getName().substring(1);
            Method getter = TeacherAssessment.class.getMethod("get" + suffix);
            Method setter = TeacherAssessment.class.getMethod("set" + suffix, double.class);
            return new ScoreInput(field.getName(), getter, setter, comboBox);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot bind rubric score: " + field.getName(), exception);
        }
    }

    @FunctionalInterface
    private interface AssessmentOperation {
        TeacherAssessment run() throws SQLException;
    }

    private record ScoreInput(String fieldName, Method getter, Method setter,
                              JComboBox<Double> comboBox) {
        private double get(TeacherAssessment assessment) {
            try {
                return ((Number) getter.invoke(assessment)).doubleValue();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot access rubric field: " + fieldName, exception);
            }
        }

        private void set(TeacherAssessment assessment, double value) {
            try {
                setter.invoke(assessment, value);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot update rubric field: " + fieldName, exception);
            }
        }
    }

    private record CheckboxGroup(JCheckBox opening, JCheckBox body, JCheckBox conclusion) {
    }

    private record CriterionInput(String fieldName, JCheckBox opening, JCheckBox body,
                                  JCheckBox conclusion, JTextArea evidence,
                                  JTextArea rationale, JComboBox<Double> score) {
    }

    private static final class DocumentChangeListener implements javax.swing.event.DocumentListener {
        private final Runnable onChange;

        private DocumentChangeListener(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent event) {
            onChange.run();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent event) {
            onChange.run();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent event) {
            onChange.run();
        }
    }
}
