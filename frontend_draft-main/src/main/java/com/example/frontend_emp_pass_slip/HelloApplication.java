package com.example.frontend_emp_pass_slip;

import backend.db.ConnectionPoolManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import backend.app.SessionManager;
import backend.app.AppSettingsManager;
import java.io.IOException;

public class HelloApplication extends Application {

    private SessionManager sessionManager;

    @Override
    public void start(Stage stage) throws IOException {
        initializeDatabase();

        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("/com/example/frontend_emp_pass_slip/view/Login.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(), 1280, 768);

        stage.setTitle("Pass Slip System");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.show();

        // CRITICAL FIX: Pass the STAGE, not the scene!
        startAutoLogout(stage);
    }

    private void initializeDatabase() {
        String url = System.getenv("SUPABASE_DB_URL");
        String user = System.getenv("SUPABASE_DB_USER");
        String password = System.getenv("SUPABASE_DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new IllegalStateException("Missing Supabase environment variables.");
        }

        ConnectionPoolManager.initialize(url, user, password, 3);
        System.out.println("Database pool initialized successfully.");
    }

    @Override
    public void stop() {
        try {
            ConnectionPoolManager.getInstance().shutdown();
            System.out.println("Database pool shut down.");
        } catch (IllegalStateException e) {
            // Pool was never initialized
        }

        if (sessionManager != null) {
            sessionManager.stopTimer();
        }
    }

    // CRITICAL FIX: Method signature now accepts Stage
    private void startAutoLogout(Stage stage) {
        int minutes = AppSettingsManager.getInstance().getAutoLogoutTimer();

        Runnable doLogout = () -> {
            System.out.println("Auto-logout triggered! Returning to Login.");

            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(
                            HelloApplication.class.getResource("/com/example/frontend_emp_pass_slip/view/Login.fxml")
                    );

                    Scene loginScene = new Scene(loader.load(), 1280, 768);

                    // We already have the stage, so we can just use it directly!
                    stage.setScene(loginScene);
                    stage.centerOnScreen();

                    AppSettingsManager.getInstance().stopTracker();

                    // Optional: If you want the timer to start again immediately on the login screen:
                    // startAutoLogout(stage);

                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Auto-logout failed to load Login.fxml view config.");
                }
            });
        };

        // Pass the stage into the new SessionManager
        sessionManager = new SessionManager(stage, minutes, doLogout);
    }

    public static void main(String[] args) {
        launch();
    }
}