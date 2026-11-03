package com.ieltsmate.view;

import com.ieltsmate.controller.EssayController;
import com.ieltsmate.entity.ChecklistAnswer;
import com.ieltsmate.entity.ChecklistResponse;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.SelfAssessment;
import com.ieltsmate.entity.TaskType;
import com.ieltsmate.service.ChecklistService;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChecklistPanel extends JPanel {
    private final EssayController controller;
    private final Runnable onBack;
    private final JPanel criteriaPanel = new JPanel();
    private final JProgressBar completionBar = new JProgressBar(0, 100);
    private final JComboBox<String> estimatedBand = new JComboBox<>(createBandOptions());
    private final JTextArea commentArea = new JTextArea(4, 40);
    private final JButton saveButton = new JButton("Lưu tự đánh giá");
    private final JLabel statusLabel = new JLabel(" ");
    private final List<ChecklistRow> rows = new ArrayList<>();
    private Essay essay;
    private boolean busy;

    public ChecklistPanel(EssayController controller, Runnable onBack) {
        this.controller = Objects.requireNonNull(controller);
        this.onBack = Objects.requireNonNull(onBack);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        completionBar.setStringPainted(true);
        completionBar.setString("0%");
        JPanel top = new JPanel(new BorderLayout(8, 8));
        JButton backButton = new JButton("Quay lại bài viết");
        backButton.addActionListener(event -> onBack.run());
        top.add(backButton, BorderLayout.WEST);
        top.add(completionBar, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        criteriaPanel.setLayout(new javax.swing.BoxLayout(criteriaPanel, javax.swing.BoxLayout.Y_AXIS));
        add(new JScrollPane(criteriaPanel), BorderLayout.CENTER);

        JPanel assessment = new JPanel(new BorderLayout(8, 8));
        assessment.setBorder(BorderFactory.createTitledBorder("Tự đánh giá"));
        JPanel bandPanel = new JPanel(new BorderLayout(8, 0));
        bandPanel.add(new JLabel("Band ước tính (0–9):"), BorderLayout.WEST);
        bandPanel.add(estimatedBand, BorderLayout.CENTER);
        assessment.add(bandPanel, BorderLayout.NORTH);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        commentArea.setBorder(BorderFactory.createTitledBorder("Nhận xét"));
        assessment.add(new JScrollPane(commentArea), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(saveButton, BorderLayout.EAST);
        assessment.add(footer, BorderLayout.SOUTH);
        add(assessment, BorderLayout.SOUTH);
        saveButton.addActionListener(event -> save());
    }

    public void loadEssay(Essay essay) {
        this.essay = Objects.requireNonNull(essay);
        busy = true;
        saveButton.setEnabled(false);
        statusLabel.setText("Đang tải checklist...");
        int essayId = Math.toIntExact(essay.getId());

        new SwingWorker<ChecklistData, Void>() {
            @Override
            protected ChecklistData doInBackground() throws Exception {
                TaskType type = controller.getTask(Math.toIntExact(essay.getTaskId()))
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy đề bài."))
                        .getTaskType();
                return new ChecklistData(type, controller.getChecklistTemplate(type),
                        controller.getChecklistResponses(essayId),
                        controller.getSelfAssessment(essayId).orElse(null),
                        controller.getChecklistCompletion(essayId));
            }

            @Override
            protected void done() {
                busy = false;
                saveButton.setEnabled(true);
                try {
                    showChecklist(get());
                    statusLabel.setText(" ");
                } catch (Exception exception) {
                    statusLabel.setText("Không tải được checklist.");
                    showError("Không thể tải checklist", exception);
                }
            }
        }.execute();
    }

    private void showChecklist(ChecklistData data) {
        rows.clear();
        criteriaPanel.removeAll();
        JLabel heading = new JLabel("Section / Tiêu chí                         Yes       Partially       No");
        heading.setBorder(BorderFactory.createEmptyBorder(4, 6, 8, 6));
        criteriaPanel.add(heading);
        Map<String, ChecklistAnswer> savedAnswers = new HashMap<>();
        for (ChecklistResponse response : data.responses()) {
            savedAnswers.put(key(response.getSectionName(), response.getCriterion()), response.getAnswer());
        }

        String currentSection = null;
        for (ChecklistService.ChecklistItem item : data.template()) {
            if (!item.sectionName().equals(currentSection)) {
                currentSection = item.sectionName();
                JLabel section = new JLabel(currentSection);
                section.setFont(section.getFont().deriveFont(java.awt.Font.BOLD));
                section.setBorder(BorderFactory.createEmptyBorder(12, 6, 4, 6));
                criteriaPanel.add(section);
            }
            ChecklistRow row = new ChecklistRow(item);
            ChecklistAnswer answer = savedAnswers.get(key(item.sectionName(), item.criterion()));
            row.select(answer);
            rows.add(row);
            criteriaPanel.add(row.panel());
        }
        completionBar.setValue(data.completionPercent());
        completionBar.setString(data.completionPercent() + "%");
        if (data.selfAssessment() != null) {
            SelfAssessment assessment = data.selfAssessment();
            estimatedBand.setSelectedItem(assessment.getEstimatedBand() == null
                    ? "" : assessment.getEstimatedBand().toString());
            commentArea.setText(assessment.getComment() == null ? "" : assessment.getComment());
        } else {
            estimatedBand.setSelectedItem("");
            commentArea.setText("");
        }
        rows.forEach(row -> row.yes().addActionListener(event -> updateProgress()));
        rows.forEach(row -> row.partially().addActionListener(event -> updateProgress()));
        rows.forEach(row -> row.no().addActionListener(event -> updateProgress()));
        criteriaPanel.revalidate();
        criteriaPanel.repaint();
        updateProgress();
    }

    private void updateProgress() {
        if (rows.isEmpty()) {
            completionBar.setValue(0);
            completionBar.setString("0%");
            return;
        }

        int totalWeight = rows.size() * 100;
        int earnedWeight = 0;
        for (ChecklistRow row : rows) {
            ChecklistAnswer answer = row.answer();
            if (answer == null) {
                continue;
            }
            int weight = switch (answer) {
                case YES -> 100;
                case PARTIALLY -> 50;
                case NO -> 0;
                default -> 0;
            };
            earnedWeight += weight;
        }

        int percent = (int) Math.round(earnedWeight * 100.0 / totalWeight);
        completionBar.setValue(percent);
        completionBar.setString(percent + "%");
    }

    private void save() {
        if (essay == null || busy) {
            return;
        }
        List<ChecklistResponse> responses = rows.stream()
                .filter(ChecklistRow::isAnswered)
                .map(row -> new ChecklistResponse(0, essay.getId(),
                        row.item().sectionName(), row.item().criterion(), row.answer()))
                .toList();
        String selectedBand = (String) estimatedBand.getSelectedItem();
        Double band = selectedBand == null || selectedBand.isBlank()
                ? null : Double.valueOf(selectedBand);
        String comment = commentArea.getText();
        SelfAssessment selfAssessment =
                new SelfAssessment(0, essay.getId(), band, comment);
        int essayId = Math.toIntExact(essay.getId());
        busy = true;
        saveButton.setEnabled(false);
        statusLabel.setText("Đang lưu...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                controller.saveChecklistResponses(essayId, responses);
                controller.saveSelfAssessment(selfAssessment);
                return null;
            }

            @Override
            protected void done() {
                busy = false;
                saveButton.setEnabled(true);
                try {
                    get();
                    statusLabel.setText("Đã lưu checklist và tự đánh giá.");
                    JOptionPane.showMessageDialog(ChecklistPanel.this,
                            "Đã lưu checklist và tự đánh giá.", "IELTS Writing Mate",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    statusLabel.setText("Không thể lưu dữ liệu.");
                    showError("Không thể lưu checklist", exception);
                }
            }
        }.execute();
    }

    private void showError(String action, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(this, action + ": " + cause.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private static String[] createBandOptions() {
        String[] options = new String[20];
        options[0] = "";
        for (int i = 0; i <= 18; i++) {
            options[i + 1] = String.format(java.util.Locale.ROOT, "%.1f", i * 0.5);
        }
        return options;
    }

    private static String key(String section, String criterion) {
        return section + "\u0000" + criterion;
    }

    private record ChecklistData(TaskType type,
                                 List<ChecklistService.ChecklistItem> template,
                                 List<ChecklistResponse> responses,
                                 SelfAssessment selfAssessment,
                                 int completionPercent) {
    }

    private static final class ChecklistRow {
        private final ChecklistService.ChecklistItem item;
        private final JRadioButton yes = new JRadioButton("Yes");
        private final JRadioButton partially = new JRadioButton("Partially");
        private final JRadioButton no = new JRadioButton("No");
        private final JPanel panel = new JPanel(new GridLayout(1, 4, 8, 4));

        private ChecklistRow(ChecklistService.ChecklistItem item) {
            this.item = item;
            ButtonGroup group = new ButtonGroup();
            group.add(yes);
            group.add(partially);
            group.add(no);
            panel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            panel.add(new JLabel(item.criterion()));
            panel.add(yes);
            panel.add(partially);
            panel.add(no);
        }

        private ChecklistService.ChecklistItem item() {
            return item;
        }

        private JRadioButton yes() {
            return yes;
        }

        private JRadioButton partially() {
            return partially;
        }

        private JRadioButton no() {
            return no;
        }

        private JPanel panel() {
            return panel;
        }

        private void select(ChecklistAnswer answer) {
            if (answer == null) {
                return;
            }
            switch (answer) {
                case YES -> yes.setSelected(true);
                case PARTIALLY -> partially.setSelected(true);
                case NO -> no.setSelected(true);
            }
        }

        private boolean isAnswered() {
            return yes.isSelected() || partially.isSelected() || no.isSelected();
        }

        private ChecklistAnswer answer() {
            if (yes.isSelected()) {
                return ChecklistAnswer.YES;
            }
            if (partially.isSelected()) {
                return ChecklistAnswer.PARTIALLY;
            }
            if (no.isSelected()) {
                return ChecklistAnswer.NO;
            }
            return null;
        }
    }
}