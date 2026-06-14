package com.example.frontend_emp_pass_slip.controller;

import backend.passslip.MonitoringJdbcRepository;
import backend.passslip.PassSlipMonitoringRecord;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import backend.app.AppSettingsManager;
public class MonitoringController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label countLabel;

    @FXML private TableView<PassSlipMonitoringRecord> monitoringTable;
    @FXML private TableColumn<PassSlipMonitoringRecord, String> slipNoColumn;
    @FXML private TableColumn<PassSlipMonitoringRecord, String> employeeIdColumn;
    @FXML private TableColumn<PassSlipMonitoringRecord, String> departmentColumn;
    @FXML private TableColumn<PassSlipMonitoringRecord, String> dateColumn;
    @FXML private TableColumn<PassSlipMonitoringRecord, String> nameColumn;
    @FXML private TableColumn<PassSlipMonitoringRecord, String> timeOutColumn;
    @FXML private TableColumn<PassSlipMonitoringRecord, String> timeInColumn;
    @FXML private TableColumn<PassSlipMonitoringRecord, String> durationColumn;
    @FXML private TableColumn<PassSlipMonitoringRecord, String> typeColumn;
    @FXML private TableColumn<PassSlipMonitoringRecord, String> statusColumn;

    private final MonitoringJdbcRepository monitoringRepository = new MonitoringJdbcRepository();
    private final ObservableList<PassSlipMonitoringRecord> records = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupColumns();
        setupFilters();
        loadRecordsFromDatabase();

        monitoringTable.setOnMouseClicked(event -> {
            PassSlipMonitoringRecord selected = monitoringTable.getSelectionModel().getSelectedItem();

            if (event.getClickCount() == 2 && selected != null && "Out".equalsIgnoreCase(selected.getStatus())) {
                showTimeInDialog(selected);
            }
        });
    }

    private void setupColumns() {
        slipNoColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getSlipNo()));
        employeeIdColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getEmployeeId()));
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        departmentColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getDepartment()));
        durationColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getDuration()));
        typeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getType()));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatus()));

        // --- APPLY GLOBAL FORMATTING TO DATES AND TIMES ---
        dateColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(AppSettingsManager.getInstance().formatDateString(data.getValue().getDate())));

        timeOutColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(AppSettingsManager.getInstance().formatTimeString(data.getValue().getTimeOut())));

        timeInColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(AppSettingsManager.getInstance().formatTimeString(data.getValue().getTimeIn())));
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(
                "All Status", "Out", "Returned", "Pending", "Cancelled"
        ));

        statusFilter.getSelectionModel().selectFirst();

        FilteredList<PassSlipMonitoringRecord> filtered =
                new FilteredList<>(records, record -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                applyFilters(filtered)
        );

        statusFilter.valueProperty().addListener((observable, oldValue, newValue) ->
                applyFilters(filtered)
        );

        filtered.addListener((javafx.collections.ListChangeListener<PassSlipMonitoringRecord>) change ->
                updateCount(filtered.size())
        );

        monitoringTable.setItems(filtered);
    }

    private void loadRecordsFromDatabase() {
        records.setAll(monitoringRepository.findAll());
        updateCount(records.size());
    }

    private void applyFilters(FilteredList<PassSlipMonitoringRecord> filtered) {
        String query = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        String status = statusFilter.getValue() == null
                ? "All Status"
                : statusFilter.getValue();

        filtered.setPredicate(record -> {
            boolean matchesQuery = query.isEmpty()
                    || record.getName().toLowerCase().contains(query)
                    || record.getSlipNo().toLowerCase().contains(query)
                    || record.getEmployeeId().toLowerCase().contains(query)
                    || record.getDepartment().toLowerCase().contains(query);

            boolean matchesStatus =
                    "All Status".equals(status) || record.getStatus().equalsIgnoreCase(status);

            return matchesQuery && matchesStatus;
        });
    }

    private void updateCount(int count) {
        countLabel.setText("Showing " + count + " of " + records.size() + " records");
    }

    private void showTimeInDialog(PassSlipMonitoringRecord record) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Record Employee Time-In");
        dialog.setHeaderText("Record Employee Time-In: " + record.getName());

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(12);
        form.setPadding(new Insets(8, 10, 4, 10));

        ComboBox<String> remarks = new ComboBox<>(FXCollections.observableArrayList(
                "Returned on time", "Returned late", "For review"
        ));
        remarks.getSelectionModel().selectFirst();

        // Format the Time Out string for the dialog display!
        String formattedTimeOut = AppSettingsManager.getInstance().formatTimeString(record.getTimeOut());

        form.addRow(0, new Label("Slip No:"), new Label(record.getSlipNo()));
        form.addRow(1, new Label("Employee Name:"), new Label(record.getName()));
        form.addRow(2, new Label("Department:"), new Label(record.getDepartment()));
        form.addRow(3, new Label("Time Out:"), new Label(formattedTimeOut));
        form.addRow(4, new Label("Remarks:"), remarks);

        dialog.getDialogPane().setContent(form);

        ButtonType confirmButtonType = new ButtonType("Confirm Time-In", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == confirmButtonType) {
                boolean updated = monitoringRepository.markAsReturned(record.getPassSlipId());

                if (updated) {
                    loadRecordsFromDatabase();
                    showInfo("Time-In Recorded", "Employee has been marked as returned.");
                } else {
                    showError("Update Failed", "Could not mark this pass slip as returned.");
                }
            }
        });
    }

    @FXML
    private void exportCsv() {
        showInfo("Export", "CSV export is not implemented yet.");
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}