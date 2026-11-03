package com.ieltsmate;

import com.ieltsmate.config.DatabaseConnection;
import com.ieltsmate.controller.AssessmentController;
import com.ieltsmate.controller.AuthController;
import com.ieltsmate.controller.EssayController;
import com.ieltsmate.dao.ChecklistResponseDAO;
import com.ieltsmate.dao.EssayDAO;
import com.ieltsmate.dao.RubricEvidenceDAO;
import com.ieltsmate.dao.SelfAssessmentDAO;
import com.ieltsmate.dao.TeacherAssessmentDAO;
import com.ieltsmate.dao.UserDAO;
import com.ieltsmate.dao.WritingTaskDAO;
import com.ieltsmate.service.AssessmentService;
import com.ieltsmate.service.AuthService;
import com.ieltsmate.service.BackupService;
import com.ieltsmate.service.ChecklistService;
import com.ieltsmate.service.EssayService;
import com.ieltsmate.service.ExportService;
import com.ieltsmate.service.WritingTaskService;
import com.ieltsmate.util.UITheme;
import com.ieltsmate.view.LoginFrame;
import com.ieltsmate.view.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("IELTS Writing Mate requires a graphical desktop");
        }
        try {
            UITheme.install();
            Connection connection = DatabaseConnection.open();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    connection.close();
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "Could not close the database connection", exception);
                }
            }, "database-shutdown"));

            UserDAO userDAO = new UserDAO(connection);
            WritingTaskDAO writingTaskDAO = new WritingTaskDAO(connection);
            EssayDAO essayDAO = new EssayDAO(connection);
            AuthService authService = new AuthService(userDAO);
            AuthController authController = new AuthController(authService);
            EssayService essayService = new EssayService(essayDAO, writingTaskDAO, authService);
            WritingTaskService writingTaskService = new WritingTaskService(writingTaskDAO);
            ChecklistService checklistService = new ChecklistService(
                    new ChecklistResponseDAO(connection), new SelfAssessmentDAO(connection),
                    essayDAO, writingTaskDAO, authService);
            EssayController essayController = new EssayController(
                    essayService, writingTaskService, checklistService, new ExportService(),
                    new BackupService(), authService);
            AssessmentController assessmentController = new AssessmentController(
                    new AssessmentService(essayDAO, new TeacherAssessmentDAO(connection),
                            new RubricEvidenceDAO(connection), authService));

            SwingUtilities.invokeLater(() ->
                    showLogin(authController, essayController, assessmentController));
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Application startup failed", exception);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null, "Không thể khởi động ứng dụng: " + exception.getMessage(),
                    "Lỗi khởi động", JOptionPane.ERROR_MESSAGE));
        }
    }

    private static void showLogin(AuthController authController,
                                  EssayController essayController,
                                  AssessmentController assessmentController) {
        LoginFrame loginFrame = new LoginFrame(authController, user -> {
            MainFrame mainFrame = new MainFrame(authController, essayController,
                    assessmentController, () -> showLogin(authController, essayController, assessmentController));
            mainFrame.setVisible(true);
        });
        loginFrame.setVisible(true);
    }
}
