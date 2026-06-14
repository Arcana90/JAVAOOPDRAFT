package backend.app; // Change this to match your folder structure

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class SessionManager {
    private Timer timer;
    private final Stage stage; // CRITICAL FIX: Track the Stage, not the Scene
    private final Runnable logoutAction;
    private final int timeoutMinutes;

    private Alert warningDialog;
    private boolean isWarningShowing = false;

    // Constructor now requires the Stage
    public SessionManager(Stage stage, int timeoutMinutes, Runnable logoutAction) {
        this.stage = stage;
        this.timeoutMinutes = timeoutMinutes;
        this.logoutAction = logoutAction;

        setupActivityMonitors();
        startTimer();
    }

    private void setupActivityMonitors() {
        // Event filters on the Stage intercept every single click/type globally
        stage.addEventFilter(MouseEvent.ANY, e -> resetTimer());
        stage.addEventFilter(KeyEvent.ANY, e -> resetTimer());
    }

    public void resetTimer() {
        // Don't let background mouse movements reset the timer IF the warning is already on screen.
        if (isWarningShowing) return;

        if (timer != null) timer.cancel();
        startTimer();
    }

    private void startTimer() {
        timer = new Timer(true); // Run as a daemon thread

        long timeoutMillis = timeoutMinutes * 60 * 1000L;
        long warningMillis = timeoutMillis - 30000L; // Show warning 30 seconds BEFORE timeout

        // Safety check: if they set the timer to 1 minute, show warning at 15 seconds
        if (warningMillis <= 0) warningMillis = 15000L;

        // 1. Schedule the Warning Pop-up
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> showWarningDialog());
            }
        }, warningMillis);

        // 2. Schedule the Actual Logout
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> executeLogout());
            }
        }, timeoutMillis);
    }

    private void showWarningDialog() {
        isWarningShowing = true;

        warningDialog = new Alert(Alert.AlertType.WARNING);
        warningDialog.setTitle("Session Inactive");
        warningDialog.setHeaderText("Are you still there?");
        warningDialog.setContentText("Your session has been inactive and will expire in 30 seconds.");

        ButtonType imStillHereBtn = new ButtonType("I'm Still Here");
        warningDialog.getButtonTypes().setAll(imStillHereBtn);

        // Wait for the user to click the button
        Optional<ButtonType> result = warningDialog.showAndWait();
        if (result.isPresent() && result.get() == imStillHereBtn) {
            isWarningShowing = false;
            resetTimer(); // Restart the clock!
        }
    }

    private void executeLogout() {
        if (warningDialog != null && warningDialog.isShowing()) {
            warningDialog.close();
        }

        if (timer != null) timer.cancel();

        logoutAction.run();
    }

    public void stopTimer() {
        if (timer != null) timer.cancel();
    }
}