package com.ieltsmate.view;

import com.ieltsmate.controller.AssessmentController;
import com.ieltsmate.controller.EssayController;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.SelfAssessment;
import com.ieltsmate.entity.TeacherAssessment;
import com.ieltsmate.entity.WritingTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Objects;

public class ResultPanel extends JPanel {
    private final EssayController essayController;
    private final AssessmentController assessmentController;
    private final Runnable onBack;
    private final JLabel statusLabel = new JLabel(" ");
    private final JPanel scorePanel = new JPanel(new GridLayout(0, 2, 8, 8));
    private final JTextArea essayContent = new JTextArea();
    private final JTextArea feedback = new JTextArea();
    private final JTextArea selfComment = new JTextArea();
    private final JLabel checklistProgress = new JLabel("Checklist: —");
    private Essay essay;

    public ResultPanel(EssayController essayController,
                       AssessmentController assessmentController,
                       Runnable onBack) {
        this.essayController = Objects.requireNonNull(essayController);
        this.assessmentController = Objects.requireNonNull(assessmentController);
        this.onBack = Objects.requireNonNull(onBack);
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JButton back = new JButton("Quay lại danh sách");
        back.addActionListener(event -> onBack.run());
        JPanel header = new JPanel(new BorderLayout());
        header.add(back, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        scorePanel.setBorder(BorderFactory.createTitledBorder("Kết quả chấm"));
        essayContent.setEditable(false);
        essayContent.setLineWrap(true);
        essayContent.setWrapStyleWord(true);
        essayContent.setBorder(BorderFactory.createTitledBorder("Bài viết"));

        feedback.setEditable(false);
        feedback.setLineWrap(true);
        feedback.setWrapStyleWord(true);
        feedback.setBorder(BorderFactory.createTitledBorder("Feedback của giáo viên"));
        selfComment.setEditable(false);
        selfComment.setLineWrap(true);
        selfComment.setWrapStyleWord(true);
        selfComment.setBorder(BorderFactory.createTitledBorder("Nhận xét tự đánh giá"));

        JPanel details = new JPanel(new BorderLayout(8, 8));
        details.add(scorePanel, BorderLayout.NORTH);
        JPanel comments = new JPanel(new GridLayout(2, 1, 0, 8));
        comments.add(new JScrollPane(feedback));
        comments.add(new JScrollPane(selfComment));
        details.add(comments, BorderLayout.CENTER);
        details.add(checklistProgress, BorderLayout.SOUTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 10, 0));
        body.add(new JScrollPane(essayContent));
        body.add(details);
        add(body, BorderLayout.CENTER);
    }

    public void loadEssay(Essay essay) {
        this.essay = Objects.requireNonNull(essay);
        statusLabel.setText("Đang tải kết quả...");
        int essayId = Math.toIntExact(essay.getId());
        new SwingWorker<ResultData, Void>() {
            @Override
            protected ResultData doInBackground() throws Exception {
                TeacherAssessment assessment = assessmentController.getAssessment(essayId).orElse(null);
                SelfAssessment selfAssessment = essayController.getSelfAssessment(essayId).orElse(null);
                int completion = essayController.getChecklistCompletion(essayId);
                WritingTask task = essayController.getTask(Math.toIntExact(essay.getTaskId()))
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy đề bài."));
                return new ResultData(assessment, selfAssessment, completion, task);
            }

            @Override
            protected void done() {
                try {
                    showResult(get());
                } catch (Exception exception) {
                    showError("Không thể tải kết quả", exception);
                    statusLabel.setText("Không tải được kết quả.");
                }
            }
        }.execute();
    }

    private void showResult(ResultData data) {
        essayContent.setText(essay.getContent() == null ? "" : essay.getContent());
        essayContent.setCaretPosition(0);
        scorePanel.removeAll();
        TeacherAssessment assessment = data.assessment();
        if (assessment == null) {
            statusLabel.setText("Trạng thái bài: " + essay.getStatus() + " — chưa có điểm.");
            addScore("Task Achievement", "—");
            addScore("Coherence & Cohesion", "—");
            addScore("Lexical Resource", "—");
            addScore("Grammatical Range & Accuracy", "—");
            addScore("Overall band", "—");
            feedback.setText("Chưa có feedback.");
        } else {
            statusLabel.setText("Kết quả chấm bài: " + essay.getTitle());
            addScore(data.task().getTaskType() == com.ieltsmate.entity.TaskType.TASK_1
                    ? "Task Achievement" : "Task Response", band(assessment.getTaskScore()));
            addScore("Coherence & Cohesion", band(assessment.getCoherenceScore()));
            addScore("Lexical Resource", band(assessment.getLexicalScore()));
            addScore("Grammatical Range & Accuracy", band(assessment.getGrammarScore()));
            addScore("Overall band", band(assessment.getOverallBand()));
            feedback.setText(formatTeacherFeedback(assessment.getFeedback()));
        }
        if (data.selfAssessment() == null) {
            selfComment.setText("Chưa có tự đánh giá.");
        } else {
            SelfAssessment self = data.selfAssessment();
            String band = self.getEstimatedBand() == null ? "Chưa nhập"
                    : band(self.getEstimatedBand());
            selfComment.setText("Band ước tính: " + band + "\n\n"
                    + (self.getComment() == null ? "" : self.getComment()));
        }
        checklistProgress.setText("Checklist hoàn thành: " + data.completion() + "%");
        scorePanel.revalidate();
        scorePanel.repaint();
    }

    private static String formatTeacherFeedback(String rawFeedback) {
        if (rawFeedback == null || rawFeedback.isBlank()) {
            return "Không có feedback.";
        }

        StringBuilder builder = new StringBuilder();
        String intro = extractTagValue(rawFeedback, "[Mở bài]", "[Thân bài]");
        String body = extractTagValue(rawFeedback, "[Thân bài]", "[Kết bài]");
        String conclusion = extractTagValue(rawFeedback, "[Kết bài]", "[Feedback chung]");
        String general = extractTagValue(rawFeedback, "[Feedback chung]", null);

        if (!intro.isBlank()) {
            builder.append("Mở bài:\n").append(intro);
        }
        if (!body.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append("Thân bài:\n").append(body);
        }
        if (!conclusion.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append("Kết bài:\n").append(conclusion);
        }
        if (!general.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append("Feedback chung:\n").append(general);
        }

        if (builder.isEmpty()) {
            return rawFeedback.trim();
        }
        return builder.toString();
    }

    private static String extractTagValue(String full, String tag, String nextTag) {
        int start = full.indexOf(tag);
        if (start == -1) {
            return "";
        }
        start += tag.length();
        int end = full.length();
        if (nextTag != null) {
            int nextIndex = full.indexOf(nextTag, start);
            if (nextIndex != -1) {
                end = Math.min(end, nextIndex);
            }
        }
        return full.substring(start, end).trim();
    }

    private void addScore(String criterion, String score) {
        scorePanel.add(new JLabel(criterion));
        scorePanel.add(new JLabel(score));
    }

    private void showError(String action, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(this, action + ": " + cause.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private static String band(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private record ResultData(TeacherAssessment assessment, SelfAssessment selfAssessment,
                              int completion, WritingTask task) {
    }
}