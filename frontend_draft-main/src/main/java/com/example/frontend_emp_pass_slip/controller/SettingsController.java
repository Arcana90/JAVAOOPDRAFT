package com.example.frontend_emp_pass_slip.controller;

import backend.app.SettingsRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class SettingsController {
    @FXML private Label lastLoginLabel;
    @FXML private TextField timeFormatField;
    @FXML private TextField dateFormatField;
    @FXML private TextField autoLogoutField;
    @FXML private Label statusLabel;

    private final SettingsRepository settingsRepository = new SettingsRepository();

    @FXML
    private void initialize() {
        lastLoginLabel.setText(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        loadSettings();
    }

    private void loadSettings() {
        Map<String, String> settings = settingsRepository.loadSettings();

        timeFormatField.setText(settings.getOrDefault("time_format", "24h"));
        dateFormatField.setText(settings.getOrDefault("date_format", "YYYY-MM-DD"));
        autoLogoutField.setText(settings.getOrDefault("auto_logout_minutes", "30"));
    }

    @FXML
    private void saveSettings() {
        String timeFormat = timeFormatField.getText().trim();
        String dateFormat = dateFormatField.getText().trim();
        String autoLogout = autoLogoutField.getText().trim();

        if (timeFormat.isBlank() || dateFormat.isBlank() || autoLogout.isBlank()) {
            statusLabel.setText("Please complete all settings fields.");
            statusLabel.setStyle("-fx-text-fill: #b00020;");
            return;
        }

        try {
            int minutes = Integer.parseInt(autoLogout);

            if (minutes <= 0) {
                statusLabel.setText("Auto-logout timer must be greater than 0.");
                statusLabel.setStyle("-fx-text-fill: #b00020;");
                return;
            }

        } catch (NumberFormatException e) {
            statusLabel.setText("Auto-logout timer must be a number.");
            statusLabel.setStyle("-fx-text-fill: #b00020;");
            return;
        }

        boolean saved = settingsRepository.saveSettings(timeFormat, dateFormat, autoLogout);

        if (saved) {
            statusLabel.setText("Settings saved to database.");
            statusLabel.setStyle("-fx-text-fill: #0b6b2b;");
        } else {
            statusLabel.setText("Failed to save settings. Check console.");
            statusLabel.setStyle("-fx-text-fill: #b00020;");
        }
    }

    @FXML
    private void showUnavailable() {
        statusLabel.setText("This action is not connected yet.");
        statusLabel.setStyle("-fx-text-fill: #555555;");
    }
}