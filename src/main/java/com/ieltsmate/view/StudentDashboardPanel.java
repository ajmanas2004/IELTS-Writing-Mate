package com.ieltsmate.view;

import com.ieltsmate.controller.EssayController;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.TaskType;
import com.ieltsmate.entity.WritingTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class StudentDashboardPanel extends JPanel {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EssayController controller;
    private final BiConsumer<Essay, WritingTask> openEssay;
    private final DefaultTableModel taskModel = readOnlyModel("ID", "Loại đề", "Giới hạn (phút)", "Đề bài");
    private final DefaultTableModel essayModel =
            readOnlyModel("ID", "Tiêu đề", "Loại đề", "Trạng thái", "Ngày tạo", "Ngày nộp");
    private final JTable taskTable = new JTable(taskModel);
    private final JTable essayTable = new JTable(essayModel);
    private final JLabel statusLabel = new JLabel(" ");
    private boolean loading;

    public StudentDashboardPanel(EssayController controller,
                                 BiConsumer<Essay, WritingTask> openEssay) {
        this.controller = Objects.requireNonNull(controller);
        this.openEssay = Objects.requireNonNull(openEssay);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel lists = new JPanel(new GridLayout(2, 1, 0, 12));
        lists.add(tableSection("Đề IELTS Writing", taskTable));
        lists.add(tableSection("Bài viết của bạn", essayTable));
        add(lists, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        JButton createButton = new JButton("Tạo bài mới");
        createButton.addActionListener(event -> createEssay());
        JButton openButton = new JButton("Mở bài");
        openButton.addActionListener(event -> openSelectedEssay());
        JButton refreshButton = new JButton("Làm mới");
        refreshButton.addActionListener(event -> refresh());
        JButton backupButton = new JButton("Backup");
        backupButton.addActionListener(event -> backup());
        JButton restoreButton = new JButton("Restore");
        restoreButton.addActionListener(event -> restore());
        actions.add(createButton);
        actions.add(openButton);
        actions.add(refreshButton);
        actions.add(backupButton);
        actions.add(restoreButton);
        actions.add(statusLabel);
        add(actions, BorderLayout.SOUTH);

        taskTable.setAutoCreateRowSorter(true);
        essayTable.setAutoCreateRowSorter(true);
        refresh();
    }

    public void refresh() {
        if (loading) {
            return;
        }
        loading = true;
        statusLabel.setText("Đang tải dữ liệu...");
        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws SQLException {
                return new DashboardData(controller.getTasks(), controller.getMyEssays());
            }

            @Override
            protected void done() {
                loading = false;
                try {
                    DashboardData data = get();
                    replaceRows(taskModel, data.tasks().stream().map(task -> new Object[]{
                            task.getId(), task.getTaskType(), task.getTimeLimit(), task.getPrompt()
                    }).toList());
                    replaceRows(essayModel, data.essays().stream().map(StudentDashboardPanel::essayRow).toList());
                    statusLabel.setText(data.essays().size() + " bài viết");
                } catch (Exception exception) {
                    showError("Không thể tải dữ liệu", exception);
                    statusLabel.setText("Lỗi tải dữ liệu");
                }
            }
        }.execute();
    }

    private void createEssay() {
        int row = taskTable.getSelectedRow();
        if (row < 0) {
            showMessage("Hãy chọn một đề bài trước.");
            return;
        }
        int modelRow = taskTable.convertRowIndexToModel(row);
        int taskId = ((Number) taskModel.getValueAt(modelRow, 0)).intValue();
        statusLabel.setText("Đang tạo bài viết...");
        new SwingWorker<EssayAndTask, Void>() {
            @Override
            protected EssayAndTask doInBackground() throws Exception {
                Essay essay = controller.createEssay(taskId);
                WritingTask task = controller.getTask(taskId)
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy đề vừa chọn."));
                return new EssayAndTask(essay, task);
            }

            @Override
            protected void done() {
                try {
                    EssayAndTask result = get();
                    statusLabel.setText("Đã tạo bài nháp.");
                    refresh();
                    openEssay.accept(result.essay(), result.task());
                } catch (Exception exception) {
                    showError("Không thể tạo bài viết", exception);
                    statusLabel.setText("Lỗi tạo bài");
                }
            }
        }.execute();
    }

    private void openSelectedEssay() {
        int row = essayTable.getSelectedRow();
        if (row < 0) {
            showMessage("Hãy chọn một bài viết trước.");
            return;
        }
        int modelRow = essayTable.convertRowIndexToModel(row);
        int essayId = ((Number) essayModel.getValueAt(modelRow, 0)).intValue();
        statusLabel.setText("Đang mở bài viết...");
        new SwingWorker<EssayAndTask, Void>() {
            @Override
            protected EssayAndTask doInBackground() throws Exception {
                Essay essay = controller.getEssay(essayId)
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy bài viết."));
                WritingTask task = controller.getTask(Math.toIntExact(essay.getTaskId()))
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy đề bài."));
                essay.setTaskType(task.getTaskType());
                return new EssayAndTask(essay, task);
            }

            @Override
            protected void done() {
                try {
                    EssayAndTask result = get();
                    openEssay.accept(result.essay(), result.task());
                } catch (Exception exception) {
                    showError("Không thể mở bài viết", exception);
                }
            }
        }.execute();
    }

    private void backup() {
        Path path = choosePath(true);
        if (path == null) {
            return;
        }
        setBusy(true, "Đang sao lưu...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                controller.backupEssays(controller.getMyEssays(), path);
                return null;
            }

            @Override
            protected void done() {
                setBusy(false, " ");
                try {
                    get();
                    showMessage("Đã sao lưu danh sách bài viết.");
                } catch (Exception exception) {
                    showError("Không thể sao lưu bài viết", exception);
                }
            }
        }.execute();
    }

    private void restore() {
        Path path = choosePath(false);
        if (path == null) {
            return;
        }
        int confirmation = JOptionPane.showConfirmDialog(this,
                "Khôi phục sẽ thêm hoặc cập nhật các bài trong tệp sao lưu. Tiếp tục?",
                "Xác nhận khôi phục", JOptionPane.YES_NO_OPTION);
        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }
        setBusy(true, "Đang khôi phục...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<Essay> restored = controller.restoreEssays(path);
                for (Essay essay : restored) {
                    if (essay.getStudentId() != controller.getCurrentUser().getId()) {
                        throw new SecurityException("Tệp sao lưu chứa bài viết của tài khoản khác.");
                    }
                }
                controller.persistRestoredEssays(restored);
                return null;
            }

            @Override
            protected void done() {
                setBusy(false, " ");
                try {
                    get();
                    showMessage("Đã khôi phục bài viết.");
                    refresh();
                } catch (Exception exception) {
                    showError("Không thể khôi phục bài viết", exception);
                }
            }
        }.execute();
    }

    private Path choosePath(boolean save) {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle(save ? "Sao lưu bài viết" : "Khôi phục bài viết");
        int result = save ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
        if (result != javax.swing.JFileChooser.APPROVE_OPTION) {
            return null;
        }
        Path path = chooser.getSelectedFile().toPath();
        if (save && !path.getFileName().toString().toLowerCase().endsWith(".ser")) {
            path = path.resolveSibling(path.getFileName() + ".ser");
        }
        return path;
    }

    private void setBusy(boolean busy, String message) {
        statusLabel.setText(message);
        taskTable.setEnabled(!busy);
        essayTable.setEnabled(!busy);
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

    private static JPanel tableSection(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private static Object[] essayRow(Essay essay) {
        return new Object[]{
                essay.getId(),
                essay.getTitle(),
                essay.getTaskType() == null ? "" : essay.getTaskType(),
                essay.getStatus(),
                essay.getCreatedAt() == null ? "" : essay.getCreatedAt().format(DATE_FORMAT),
                essay.getSubmittedAt() == null ? "" : essay.getSubmittedAt().format(DATE_FORMAT)
        };
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static void replaceRows(DefaultTableModel model, List<Object[]> rows) {
        model.setRowCount(0);
        rows.forEach(model::addRow);
    }

    private record DashboardData(List<WritingTask> tasks, List<Essay> essays) {
    }

    private record EssayAndTask(Essay essay, WritingTask task) {
    }
}
