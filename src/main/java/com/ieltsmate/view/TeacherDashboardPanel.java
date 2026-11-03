package com.ieltsmate.view;

import com.ieltsmate.controller.AssessmentController;
import com.ieltsmate.controller.EssayController;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.EssayStatus;
import com.ieltsmate.entity.WritingTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

public class TeacherDashboardPanel extends JPanel {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final EssayController essayController;
    private final AssessmentController assessmentController;
    private final BiConsumer<Essay, WritingTask> onOpenEssay;
    private final JComboBox<EssayStatus> statusFilter =
            new JComboBox<>(new EssayStatus[]{EssayStatus.SUBMITTED, EssayStatus.GRADED});
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Student", "Tiêu đề", "Loại đề", "Ngày nộp", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable essaysTable = new JTable(model);
    private final JLabel statusLabel = new JLabel(" ");

    public TeacherDashboardPanel(EssayController essayController,
                                AssessmentController assessmentController,
                                BiConsumer<Essay, WritingTask> onOpenEssay) {
        this.essayController = essayController;
        this.assessmentController = assessmentController;
        this.onOpenEssay = onOpenEssay;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        JPanel filter = new JPanel();
        filter.add(new JLabel("Lọc trạng thái:"));
        filter.add(statusFilter);
        JButton refreshButton = new JButton("Làm mới");
        refreshButton.addActionListener(event -> refresh());
        JButton openButton = new JButton("Mở bài");
        openButton.addActionListener(event -> openSelected());
        filter.add(refreshButton);
        filter.add(openButton);
        top.add(filter, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        essaysTable.setAutoCreateRowSorter(true);
        essaysTable.setRowHeight(36);
        essaysTable.setFillsViewportHeight(true);
        essaysTable.getColumnModel().getColumn(0).setMinWidth(0);
        essaysTable.getColumnModel().getColumn(0).setMaxWidth(0);
        essaysTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        add(new JScrollPane(essaysTable), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        statusFilter.addActionListener(event -> refresh());
        refresh();
    }

    public void refresh() {
        EssayStatus selected = (EssayStatus) statusFilter.getSelectedItem();
        if (selected == null) {
            return;
        }
        statusLabel.setText("Đang tải bài viết...");
        new SwingWorker<List<Essay>, Void>() {
            @Override
            protected List<Essay> doInBackground() throws SQLException {
                return essayController.getEssays(selected);
            }

            @Override
            protected void done() {
                try {
                    List<Essay> essays = get();
                    model.setRowCount(0);
                    for (Essay essay : essays) {
                        model.addRow(new Object[]{
                                essay.getId(),
                                essay.getStudentName() == null
                                        ? "Student #" + essay.getStudentId() : essay.getStudentName(),
                                essay.getTitle(),
                                essay.getTaskType() == null ? "-" : essay.getTaskType().name(),
                                essay.getSubmittedAt() == null ? "-" : essay.getSubmittedAt().format(DATE_FORMAT),
                                essay.getStatus()
                        });
                    }
                    statusLabel.setText("Đã tải " + essays.size() + " bài viết.");
                } catch (Exception exception) {
                    statusLabel.setText("Không thể tải bài viết.");
                    showError("Không thể tải danh sách bài viết", exception);
                }
            }
        }.execute();
    }

    private void openSelected() {
        int selectedRow = essaysTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một bài viết để mở.",
                    "Chưa có bài được chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int viewRow = essaysTable.convertRowIndexToModel(selectedRow);
        Object rawId = model.getValueAt(viewRow, 0);
        if (rawId == null) {
            JOptionPane.showMessageDialog(this, "Bài viết không hợp lệ để mở.",
                    "Lỗi dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        long essayId = Long.parseLong(rawId.toString());
        new SwingWorker<Essay, Void>() {
            @Override
            protected Essay doInBackground() throws Exception {
                return essayController.getEssay(Math.toIntExact(essayId))
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy bài viết"));
            }

            @Override
            protected void done() {
                try {
                    Essay essay = get();
                    WritingTask task = essayController.getTask(Math.toIntExact(essay.getTaskId()))
                            .orElseThrow(() -> new IllegalStateException("Không tìm thấy đề bài."));
                    onOpenEssay.accept(essay, task);
                } catch (Exception exception) {
                    showError("Không thể mở bài viết", exception);
                }
            }
        }.execute();
    }

    private void showError(String action, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(this, action + ": " + cause.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
