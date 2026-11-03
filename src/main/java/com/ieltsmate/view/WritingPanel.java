package com.ieltsmate.view;

import com.ieltsmate.controller.EssayController;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.EssayStatus;
import com.ieltsmate.entity.TaskType;
import com.ieltsmate.entity.WritingTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class WritingPanel extends JPanel {
    private final EssayController controller;
    private final Consumer<Essay> openChecklist;
    private final Runnable onBack;
    private final JTextArea promptArea = new JTextArea();
    private final JTextField titleField = new JTextField();
    private final JTextArea contentArea = new JTextArea();
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel wordCountLabel = new JLabel("0 từ");
    private final JLabel timerLabel = new JLabel("20:00");
    private final JButton saveButton = new JButton("Lưu nháp");
    private final JButton submitButton = new JButton("Nộp bài");
    private final JButton exportButton = new JButton("Xuất TXT");
    private final JButton checklistButton = new JButton("Checklist");
    private final JButton backButton = new JButton("Quay lại");
    private final Timer writingTimer;
    private Essay essay;
    private long timerEssayId = -1;
    private int remainingSeconds;
    private int defaultTimerSeconds = 20 * 60;
    private final Map<Long, Integer> remainingSecondsByEssay = new HashMap<>();
    private boolean busy;

    public WritingPanel(EssayController controller, Consumer<Essay> openChecklist, Runnable onBack) {
        this.controller = Objects.requireNonNull(controller);
        this.openChecklist = Objects.requireNonNull(openChecklist);
        this.onBack = Objects.requireNonNull(onBack);
        writingTimer = new Timer(1_000, event -> tickTimer());
        writingTimer.setCoalesce(true);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        promptArea.setEditable(false);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setRows(5);
        promptArea.setBorder(BorderFactory.createTitledBorder("Đề bài"));
        add(new JScrollPane(promptArea), BorderLayout.NORTH);

        JPanel editor = new JPanel(new BorderLayout(6, 8));
        titleField.setBorder(BorderFactory.createTitledBorder("Tiêu đề bài viết"));
        editor.add(titleField, BorderLayout.NORTH);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setFont(contentArea.getFont().deriveFont(15f));
        contentArea.setBorder(BorderFactory.createTitledBorder("Nội dung essay"));
        editor.add(new JScrollPane(contentArea), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(statusLabel, BorderLayout.WEST);
        JPanel indicators = new JPanel();
        indicators.add(timerLabel);
        indicators.add(wordCountLabel);
        footer.add(indicators, BorderLayout.EAST);
        editor.add(footer, BorderLayout.SOUTH);
        add(editor, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        backButton.addActionListener(event -> onBack.run());
        saveButton.addActionListener(event -> saveDraft());
        submitButton.addActionListener(event -> submitEssay());
        exportButton.addActionListener(event -> exportEssay());
        checklistButton.addActionListener(event -> showChecklist());
        actions.add(backButton);
        actions.add(saveButton);
        actions.add(submitButton);
        actions.add(exportButton);
        actions.add(checklistButton);
        add(actions, BorderLayout.SOUTH);
        contentArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateWordCount();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateWordCount();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateWordCount();
            }
        });
        setPreferredSize(new Dimension(900, 600));
    }

    public void openEssay(Essay essay, WritingTask task) {
        this.essay = Objects.requireNonNull(essay);
        Objects.requireNonNull(task, "task must not be null");
        promptArea.setText(task.getPrompt() == null ? "" : task.getPrompt());
        titleField.setText(essay.getTitle() == null ? "" : essay.getTitle());
        contentArea.setText(essay.getContent() == null ? "" : essay.getContent());
        statusLabel.setText("Trạng thái: " + essay.getStatus());
        long essayId = essay.getId();
        if (timerEssayId != essayId) {
            timerEssayId = essayId;
            defaultTimerSeconds = timerDurationSeconds(task);
            remainingSeconds = remainingSecondsByEssay.getOrDefault(essayId, defaultTimerSeconds);
        }
        applyTimerState();
        updateWordCount();
    }

    public void resumeTimerIfDraft() {
        if (essay == null) {
            return;
        }
        if (essay.getStatus() != EssayStatus.DRAFT) {
            stopTimer();
            timerLabel.setVisible(false);
            return;
        }
        if (essay.getId() != timerEssayId) {
            timerEssayId = essay.getId();
            remainingSeconds = remainingSecondsByEssay.getOrDefault(essay.getId(), defaultTimerSeconds);
        }
        applyTimerState();
        writingTimer.start();
    }

    public void stopTimer() {
        writingTimer.stop();
    }

    private void saveDraft() {
        if (essay == null || busy) {
            return;
        }
        Essay current = essay;
        String title = titleField.getText();
        String content = contentArea.getText();
        runEssayAction("Đang lưu nháp...", () ->
                controller.saveDraft(Math.toIntExact(current.getId()), title, content),
                "Đã lưu bản nháp.");
    }

    private void submitEssay() {
        if (essay == null || busy) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn nộp bài? Sau khi nộp, bài sẽ chỉ đọc.",
                "Xác nhận nộp bài", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        Essay current = essay;
        String title = titleField.getText();
        String content = contentArea.getText();
        runEssayAction("Đang nộp bài...", () ->
                controller.submitEssay(Math.toIntExact(current.getId()), title, content),
                "Đã nộp bài thành công.");
    }

    private void runEssayAction(String workingMessage, EssayOperation operation, String successMessage) {
        setBusy(true, workingMessage);
        new SwingWorker<Essay, Void>() {
            @Override
            protected Essay doInBackground() throws Exception {
                return operation.run();
            }

            @Override
            protected void done() {
                setBusy(false, " ");
                try {
                    essay = get();
                    statusLabel.setText("Trạng thái: " + essay.getStatus());
                    setEditable(essay.getStatus() == EssayStatus.DRAFT);
                    if (essay.getStatus() != EssayStatus.DRAFT) {
                        stopTimer();
                    }
                    showMessage(successMessage);
                } catch (Exception exception) {
                    showError("Không thể lưu bài viết", exception);
                }
            }
        }.execute();
    }

    private void exportEssay() {
        if (essay == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Xuất bài viết thành TXT");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        if (!path.getFileName().toString().toLowerCase().endsWith(".txt")) {
            path = path.resolveSibling(path.getFileName() + ".txt");
        }
        Path outputPath = path;
        Essay export = new Essay(essay.getId(), essay.getStudentId(), essay.getTaskId(),
                titleField.getText(), contentArea.getText(), essay.getStatus(),
                essay.getCreatedAt(), essay.getSubmittedAt());
        setBusy(true, "Đang xuất file...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                controller.exportEssay(export, outputPath);
                return null;
            }

            @Override
            protected void done() {
                setBusy(false, " ");
                try {
                    get();
                    showMessage("Đã xuất file TXT UTF-8.");
                } catch (Exception exception) {
                    showError("Không thể xuất bài viết", exception);
                }
            }
        }.execute();
    }

    private void showChecklist() {
        if (essay == null) {
            return;
        }
        if (essay.getStatus() != EssayStatus.DRAFT) {
            openChecklist.accept(essay);
            return;
        }
        Essay current = essay;
        String title = titleField.getText();
        String content = contentArea.getText();
        setBusy(true, "Đang lưu trước khi mở checklist...");
        new SwingWorker<Essay, Void>() {
            @Override
            protected Essay doInBackground() throws Exception {
                return controller.saveDraft(Math.toIntExact(current.getId()),
                        title, content);
            }

            @Override
            protected void done() {
                setBusy(false, " ");
                try {
                    essay = get();
                    openChecklist.accept(essay);
                } catch (Exception exception) {
                    showError("Không thể lưu bài viết trước khi mở checklist", exception);
                }
            }
        }.execute();
    }

    private void setEditable(boolean editable) {
        titleField.setEditable(editable);
        contentArea.setEditable(editable);
        saveButton.setEnabled(editable);
        submitButton.setEnabled(editable);
        checklistButton.setEnabled(essay != null);
        exportButton.setEnabled(essay != null);
    }

    private void setBusy(boolean busy, String message) {
        this.busy = busy;
        statusLabel.setText(message);
        if (busy) {
            titleField.setEnabled(false);
            contentArea.setEnabled(false);
            backButton.setEnabled(false);
            saveButton.setEnabled(false);
            submitButton.setEnabled(false);
            exportButton.setEnabled(false);
            checklistButton.setEnabled(false);
        } else if (essay != null) {
            titleField.setEnabled(true);
            contentArea.setEnabled(true);
            backButton.setEnabled(true);
            setEditable(essay.getStatus() == EssayStatus.DRAFT);
        }
    }

    private void updateWordCount() {
        String content = contentArea.getText().strip();
        int words = content.isEmpty() ? 0 : content.split("\\s+").length;
        wordCountLabel.setText(words + " từ");
    }

    private void tickTimer() {
        if (remainingSeconds <= 0) {
            stopTimer();
            updateTimerLabel();
            return;
        }
        remainingSeconds--;
        if (essay != null) {
            remainingSecondsByEssay.put(essay.getId(), remainingSeconds);
        }
        updateTimerLabel();
        if (remainingSeconds == 0) {
            stopTimer();
            timerLabel.setText("00:00");
            JOptionPane.showMessageDialog(this,
                    "Đã hết thời gian làm bài. Bạn vẫn có thể tiếp tục viết.",
                    "Hết giờ", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateTimerLabel() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        timerLabel.setText(String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds));
    }

    private void applyTimerState() {
        boolean draftMode = essay != null && essay.getStatus() == EssayStatus.DRAFT;
        timerLabel.setVisible(draftMode);
        if (!draftMode) {
            stopTimer();
            return;
        }
        if (remainingSeconds <= 0) {
            remainingSeconds = remainingSecondsByEssay.getOrDefault(essay.getId(), defaultTimerSeconds);
        }
        remainingSecondsByEssay.put(essay.getId(), remainingSeconds);
        updateTimerLabel();
        writingTimer.start();
    }

    private static int timerDurationSeconds(WritingTask task) {
        int minutes = task.getTimeLimit() == null || task.getTimeLimit() <= 0
                ? (task.getTaskType() == TaskType.TASK_2 ? 40 : 20)
                : task.getTimeLimit();
        return Math.multiplyExact(minutes, 60);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "IELTS Writing Mate",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String action, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(this, action + ": " + cause.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface EssayOperation {
        Essay run() throws Exception;
    }

    @Override
    public void removeNotify() {
        stopTimer();
        super.removeNotify();
    }
}