package com.ieltsmate.view;

import com.ieltsmate.controller.AssessmentController;
import com.ieltsmate.controller.AuthController;
import com.ieltsmate.controller.EssayController;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.WritingTask;
import com.ieltsmate.entity.Role;
import com.ieltsmate.entity.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.Objects;

public class MainFrame extends JFrame {
    private static final String DASHBOARD_CARD = "dashboard";
    private static final String WRITING_CARD = "writing";
    private static final String CHECKLIST_CARD = "checklist";
    private static final String RESULT_CARD = "result";
    private static final String ASSESSMENT_CARD = "assessment";

    private final AuthController authController;
    private final EssayController essayController;
    private final AssessmentController assessmentController;
    private final Runnable onLogout;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final JLabel userLabel = new JLabel();
    private final StudentDashboardPanel studentDashboard;
    private final WritingPanel writingPanel;
    private final ChecklistPanel checklistPanel;
    private final ResultPanel resultPanel;
    private TeacherDashboardPanel teacherDashboardPanel;

    public MainFrame(AuthController authController, EssayController essayController,
                     AssessmentController assessmentController, Runnable onLogout) {
        super("IELTS Writing Mate");
        this.authController = Objects.requireNonNull(authController);
        this.essayController = Objects.requireNonNull(essayController);
        this.assessmentController = Objects.requireNonNull(assessmentController);
        this.onLogout = Objects.requireNonNull(onLogout);
        User user = authController.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("A user must be logged in"));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                if (writingPanel != null) {
                    writingPanel.stopTimer();
                }
            }

            @Override
            public void windowClosed(java.awt.event.WindowEvent event) {
                if (writingPanel != null) {
                    writingPanel.stopTimer();
                }
            }
        });
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);
        studentDashboard = user.getRole() == Role.STUDENT
                ? new StudentDashboardPanel(essayController, this::openWriting)
                : null;
        writingPanel = user.getRole() == Role.STUDENT
                ? new WritingPanel(essayController, this::openChecklist, this::showDashboard)
                : null;
        checklistPanel = user.getRole() == Role.STUDENT
                ? new ChecklistPanel(essayController, this::showWriting)
                : null;
        resultPanel = user.getRole() == Role.STUDENT
                ? new ResultPanel(essayController, assessmentController, this::showDashboard)
                : null;
        teacherDashboardPanel = user.getRole() == Role.STUDENT
                ? null
                : new TeacherDashboardPanel(essayController, assessmentController, this::openAssessment);
        buildUi(user, assessmentController);
    }

    private void buildUi(User user, AssessmentController assessmentController) {
        JPanel root = new JPanel(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        userLabel.setText(user.getFullName() + "  ·  " + roleName(user.getRole()));
        JButton dashboardButton = new JButton("Trang chủ");
        dashboardButton.addActionListener(event -> showDashboard());
        JButton logoutButton = new JButton("Đăng xuất");
        logoutButton.addActionListener(event -> logout());

        JPanel navigation = new JPanel();
        navigation.add(dashboardButton);
        navigation.add(logoutButton);
        header.add(userLabel, BorderLayout.CENTER);
        header.add(navigation, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        if (user.getRole() == Role.STUDENT) {
            cards.add(studentDashboard, DASHBOARD_CARD);
            cards.add(writingPanel, WRITING_CARD);
            cards.add(checklistPanel, CHECKLIST_CARD);
            cards.add(resultPanel, RESULT_CARD);
        } else {
            cards.add(teacherDashboardPanel, DASHBOARD_CARD);
            cards.add(new JPanel(), ASSESSMENT_CARD);
        }
        root.add(cards, BorderLayout.CENTER);
        setContentPane(root);
        pack();
    }

    private void openWriting(Essay essay, WritingTask task) {
        if (essay.getStatus() == com.ieltsmate.entity.EssayStatus.GRADED) {
            resultPanel.loadEssay(essay);
            cardLayout.show(cards, RESULT_CARD);
            return;
        }
        writingPanel.openEssay(essay, task);
        if (essay.getStatus() == com.ieltsmate.entity.EssayStatus.DRAFT) {
            writingPanel.resumeTimerIfDraft();
        } else {
            writingPanel.stopTimer();
        }
        cardLayout.show(cards, WRITING_CARD);
    }

    private void openChecklist(Essay essay) {
        writingPanel.stopTimer();
        checklistPanel.loadEssay(essay);
        cardLayout.show(cards, CHECKLIST_CARD);
    }

    private void showWriting() {
        if (writingPanel != null) {
            writingPanel.resumeTimerIfDraft();
        }
        cardLayout.show(cards, WRITING_CARD);
    }

    private void showDashboard() {
        if (writingPanel != null) {
            writingPanel.stopTimer();
        }
        if (studentDashboard != null) {
            studentDashboard.refresh();
        }
        cardLayout.show(cards, DASHBOARD_CARD);
    }

    private void logout() {
        authController.logout();
        dispose();
        SwingUtilities.invokeLater(onLogout);
    }

    private static String roleName(Role role) {
        return role == Role.STUDENT ? "Học sinh" : "Giáo viên";
    }

    private void openAssessment(Essay essay, WritingTask task) {
        if (essay == null || task == null) {
            return;
        }
        com.ieltsmate.entity.TeacherAssessment existingAssessment = null;
        try {
            existingAssessment = assessmentController.getAssessment(Math.toIntExact(essay.getId())).orElse(null);
        } catch (Exception exception) {
            existingAssessment = null;
            JOptionPane.showMessageDialog(this,
                    "Không thể tải bài chấm hiện có: " + exception.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
        AssessmentPanel assessmentPanel = new AssessmentPanel(
                assessmentController,
                essay,
                task,
                existingAssessment,
                () -> {
                    cardLayout.show(cards, DASHBOARD_CARD);
                    if (teacherDashboardPanel != null) {
                        teacherDashboardPanel.refresh();
                    }
                }
        );
        cards.add(assessmentPanel, ASSESSMENT_CARD);
        cardLayout.show(cards, ASSESSMENT_CARD);
    }
}