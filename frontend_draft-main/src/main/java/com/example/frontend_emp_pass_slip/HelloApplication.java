package com.example.frontend_emp_pass_slip;

import backend.db.ConnectionPoolManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

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
            // Pool was never initialized, so nothing to shut down.
        }
    }

    public static void main(String[] args) {
        launch();
    }
}