package com.ieltsmate.view;

import com.ieltsmate.controller.AuthController;
import com.ieltsmate.entity.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class LoginFrame extends JFrame {
    private final AuthController authController;
    private final Consumer<User> onLogin;
    private final JTextField usernameField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JButton loginButton = new JButton("Đăng nhập");
    private final JLabel messageLabel = new JLabel(" ");

    public LoginFrame(AuthController authController, Consumer<User> onLogin) {
        super("IELTS Writing Mate - Đăng nhập");
        this.authController = Objects.requireNonNull(authController);
        this.onLogin = Objects.requireNonNull(onLogin);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(440, 390));
        setLocationRelativeTo(null);
        buildUi();
    }

    private void buildUi() {
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(28, 36, 24, 36));

        JLabel title = new JLabel("IELTS Writing Mate", JLabel.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        content.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 4, 8, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        form.add(new JLabel("Tên đăng nhập"), constraints);
        constraints.gridx = 1;
        form.add(usernameField, constraints);
        constraints.gridx = 0;
        constraints.gridy++;
        form.add(new JLabel("Mật khẩu"), constraints);
        constraints.gridx = 1;
        form.add(passwordField, constraints);
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 2;
        form.add(loginButton, constraints);
        constraints.gridy++;
        messageLabel.setForeground(new java.awt.Color(170, 35, 35));
        form.add(messageLabel, constraints);
        constraints.gridy++;
        JLabel demo = new JLabel(
                "<html><center>Tài khoản demo (mật khẩu: <b>123456</b>)<br>"
                        + "Học sinh: student1, student2 &nbsp; | &nbsp; Giáo viên: teacher1</center></html>",
                JLabel.CENTER);
        demo.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        form.add(demo, constraints);
        content.add(form, BorderLayout.CENTER);

        loginButton.addActionListener(event -> login());
        passwordField.addActionListener(event -> login());
        setContentPane(content);
        getRootPane().setDefaultButton(loginButton);
        pack();
        setMinimumSize(new Dimension(460, 360));
    }

    private void login() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        loginButton.setEnabled(false);
        messageLabel.setText("Đang xác thực...");

        new SwingWorker<Optional<User>, Void>() {
            @Override
            protected Optional<User> doInBackground() throws SQLException {
                return authController.login(username, password);
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                try {
                    Optional<User> user = get();
                    if (user.isEmpty()) {
                        messageLabel.setText("Tên đăng nhập hoặc mật khẩu không đúng.");
                        passwordField.setText("");
                        passwordField.requestFocusInWindow();
                        return;
                    }
                    dispose();
                    onLogin.accept(user.get());
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    messageLabel.setText("Không thể đăng nhập.");
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Đăng nhập thất bại: " + cause.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
