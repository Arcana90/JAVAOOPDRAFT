package com.example.frontend_emp_pass_slip.controller;

import backend.app.AppSettingsManager;
import backend.app.SettingsRepository;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.LocalDateTime;
import java.util.Map;

public class SettingsController {
    @FXML private Label lastLoginLabel;
    @FXML private ComboBox<String> timeFormatComboBox;
    @FXML private ComboBox<String> dateFormatComboBox;
    @FXML private TextField autoLogoutField;
    @FXML private Label statusLabel;

    private final SettingsRepository settingsRepository = new SettingsRepository();

    @FXML
    private void initialize() {
        // Initialize choices inside UI dropdown boxes
        timeFormatComboBox.getItems().addAll("12h", "24h");
        dateFormatComboBox.getItems().addAll("YYYY-MM-DD", "DD/MM/YYYY", "MM/DD/YYYY");

        // Format system label initialization dynamically
        lastLoginLabel.setText(
                AppSettingsManager.getInstance().formatDateTime(LocalDateTime.now())
        );

        loadSettings();
    }

    private void loadSettings() {
        Map<String, String> settings = settingsRepository.loadSettings();

        timeFormatComboBox.setValue(settings.getOrDefault("time_format", "24h"));
        dateFormatComboBox.setValue(settings.getOrDefault("date_format", "YYYY-MM-DD"));
        autoLogoutField.setText(settings.getOrDefault("auto_logout_minutes", "30"));
    }

    @FXML
    private void saveSettings() {
        String timeFormat = timeFormatComboBox.getValue();
        String dateFormat = dateFormatComboBox.getValue();
        String autoLogout = autoLogoutField.getText().trim();

        if (timeFormat == null || dateFormat == null || autoLogout.isBlank()) {
            showStatus("Please complete all settings fields.", "#b00020");
            return;
        }

        try {
            int minutes = Integer.parseInt(autoLogout);
            if (minutes <= 0) {
                showStatus("Auto-logout timer must be greater than 0.", "#b00020");
                return;
            }
        } catch (NumberFormatException e) {
            showStatus("Auto-logout timer must be a number.", "#b00020");
            return;
        }

        boolean saved = settingsRepository.saveSettings(timeFormat, dateFormat, autoLogout);

        if (saved) {
            // SYNC ALL SYSTEM MODULES IMMEDIATELY
            AppSettingsManager.getInstance().refreshSettings();

            // Re-render dashboard text timestamp layout to match the update choice configuration
            lastLoginLabel.setText(AppSettingsManager.getInstance().formatDateTime(LocalDateTime.now()));
            showStatus("Settings saved and updated successfully across the system.", "#0b6b2b");
        } else {
            showStatus("Failed to save settings. Check console.", "#b00020");
        }
    }

    private void showStatus(String message, String hexColor) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + hexColor + ";");
    }
    @FXML
    private void showUnavailable() {
        showStatus("This action is not connected yet.", "#555555");
    }
}