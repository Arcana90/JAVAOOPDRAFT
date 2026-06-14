package com.example.frontend_emp_pass_slip.controller;

import backend.app.HeaderStatsRepository;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MainLayoutController {

    @FXML private StackPane contentArea;
    @FXML private ToggleButton dashboardButton;

    @FXML private Label employeesOutLabel;
    @FXML private Label currentDateLabel;

    private final HeaderStatsRepository headerStatsRepository = new HeaderStatsRepository();

    @FXML
    private void initialize() {
        dashboardButton.setSelected(true);
        updateHeaderStats();
        loadDashboard();
    }

    @FXML
    private void loadDashboard() {
        loadView("Dashboard.fxml");
        updateHeaderStats();
    }

    @FXML
    private void loadEmployeeMgmt() {
        loadView("EmployeeManagement.fxml");
    }

    @FXML
    private void loadPassSlip() {
        loadView("PassSlipIssuance.fxml");
        updateHeaderStats();
    }

    @FXML
    private void loadMonitoring() {
        loadView("Monitoring.fxml");
        updateHeaderStats();
    }

    @FXML
    private void loadReports() {
        loadView("Reports.fxml");
    }

    @FXML
    private void loadSettings() {
        loadView("Settings.fxml");
    }

    private void updateHeaderStats() {
        int employeesOut = headerStatsRepository.countEmployeesOut();
        employeesOutLabel.setText(String.valueOf(employeesOut));

        currentDateLabel.setText(
                LocalDate.now().format(DateTimeFormatter.ofPattern("M/d/yyyy"))
        );
    }

    private void loadView(String fxmlFileName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/frontend_emp_pass_slip/view/" + fxmlFileName)
            );

            Parent view = loader.load();
            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading: " + fxmlFileName);
        }
    }

    @FXML
    private void logout() throws IOException {
        // 1. STOP THE TIMER! This ensures it doesn't keep running in the background.
        backend.app.SessionManager.getInstance().stopTimer();

        // 2. Load the Login screen
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/frontend_emp_pass_slip/view/Login.fxml")
        );

        Scene scene = new Scene(loader.load(), 1280, 768);
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.setScene(scene);
        stage.centerOnScreen();
    }
}