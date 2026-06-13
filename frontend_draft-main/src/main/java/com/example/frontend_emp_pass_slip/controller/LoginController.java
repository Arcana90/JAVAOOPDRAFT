package com.example.frontend_emp_pass_slip.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    private void login() throws IOException {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (!"admin".equalsIgnoreCase(username) || !"admin".equals(password)) {
            statusLabel.setText("Use admin / admin for the demo account.");
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/frontend_emp_pass_slip/view/MainLayout.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 768);
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(scene);
        stage.centerOnScreen();
    }
}
