package com.example.frontend_emp_pass_slip.controller;

import backend.employee.Employee;
import backend.employee.EmployeeRepository;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import backend.employee.Supervisor;
import backend.employee.SupervisorRepository;
import javafx.util.StringConverter;

public class EmployeeManagementController {

    @FXML private TextField searchField;
    @FXML private Label statusLabel;

    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, String> employeeIdColumn;
    @FXML private TableColumn<Employee, String> nameColumn;
    @FXML private TableColumn<Employee, String> departmentColumn;
    @FXML private TableColumn<Employee, String> positionColumn;
    @FXML private TableColumn<Employee, String> supervisorColumn;
    @FXML private TableColumn<Employee, String> contactNumberColumn;
    @FXML private TableColumn<Employee, String> emailColumn;
    @FXML private TableColumn<Employee, String> statusColumn;

    private final EmployeeRepository employeeRepository = new EmployeeRepository();
    private final ObservableList<Employee> employees = FXCollections.observableArrayList();
    private final SupervisorRepository supervisorRepository = new SupervisorRepository();

    @FXML
    private void initialize() {
        setupTableColumns();
        loadEmployeesFromDatabase();
        setupSearch();
    }

    private void setupTableColumns() {
        employeeIdColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getEmployeeId()));

        nameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getFullName()));

        departmentColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getDepartment()));

        positionColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getPosition()));

        supervisorColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getSupervisorName()));

        contactNumberColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getContactNumber()));

        emailColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getEmail()));

        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getStatus()));
    }

    private void loadEmployeesFromDatabase() {
        employees.setAll(employeeRepository.findAll());
        employeeTable.setItems(employees);
        statusLabel.setText(employees.size() + " employees on record");
    }

    private void setupSearch() {
        FilteredList<Employee> filtered = new FilteredList<>(employees, employee -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase();

            filtered.setPredicate(employee ->
                    query.isEmpty()
                            || employee.getEmployeeId().toLowerCase().contains(query)
                            || employee.getFullName().toLowerCase().contains(query)
                            || employee.getDepartment().toLowerCase().contains(query)
                            || employee.getPosition().toLowerCase().contains(query)
                            || employee.getSupervisorName().toLowerCase().contains(query)
                            || employee.getEmail().toLowerCase().contains(query)
                            || employee.getStatus().toLowerCase().contains(query)
            );

            statusLabel.setText(filtered.size() + " employees shown");
        });

        employeeTable.setItems(filtered);
    }

    @FXML
    private void addEmployee() {
        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle("Add Employee");
        dialog.setHeaderText("Enter employee information");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField employeeIdField = new TextField();
        employeeIdField.setPromptText("EMP-003");

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First name");

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last name");

        TextField departmentField = new TextField();
        departmentField.setPromptText("Department");

        TextField positionField = new TextField();
        positionField.setPromptText("Position");

        TextField emailField = new TextField();
        emailField.setPromptText("email@example.com");

        TextField contactNumberField = new TextField();
        contactNumberField.setPromptText("09xxxxxxxxx");

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Active", "Inactive");
        statusBox.setValue("Active");

        ComboBox<Supervisor> supervisorBox = new ComboBox<>();
        supervisorBox.setItems(FXCollections.observableArrayList(
                supervisorRepository.findActiveSupervisors()
        ));

        supervisorBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Supervisor supervisor) {
                if (supervisor == null) {
                    return "";
                }
                return supervisor.getFullName();
            }

            @Override
            public Supervisor fromString(String string) {
                return null;
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Employee ID:"), 0, 0);
        grid.add(employeeIdField, 1, 0);

        grid.add(new Label("First Name:"), 0, 1);
        grid.add(firstNameField, 1, 1);

        grid.add(new Label("Last Name:"), 0, 2);
        grid.add(lastNameField, 1, 2);

        grid.add(new Label("Department:"), 0, 3);
        grid.add(departmentField, 1, 3);

        grid.add(new Label("Position:"), 0, 4);
        grid.add(positionField, 1, 4);

        grid.add(new Label("Email:"), 0, 5);
        grid.add(emailField, 1, 5);

        grid.add(new Label("Contact No.:"), 0, 6);
        grid.add(contactNumberField, 1, 6);

        grid.add(new Label("Supervisor:"), 0, 7);
        grid.add(supervisorBox, 1, 7);

        grid.add(new Label("Status:"), 0, 8);
        grid.add(statusBox, 1, 8);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Supervisor selectedSupervisor = supervisorBox.getValue();

                return new Employee(
                        employeeIdField.getText().trim(),
                        firstNameField.getText().trim(),
                        lastNameField.getText().trim(),
                        departmentField.getText().trim(),
                        positionField.getText().trim(),
                        emailField.getText().trim(),
                        contactNumberField.getText().trim(),
                        statusBox.getValue(),
                        selectedSupervisor == null ? null : selectedSupervisor.getSupervisorId(),
                        selectedSupervisor == null ? "" : selectedSupervisor.getFullName()
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(employee -> {
            if (isEmployeeInputValid(employee)) {
                EmployeeRepository.AddEmployeeResult result = employeeRepository.addEmployee(employee);

                switch (result) {
                    case SUCCESS -> {
                        loadEmployeesFromDatabase();
                        showInfo("Employee Added", "Employee was added successfully.");
                        statusLabel.setText("Employee added successfully.");
                        statusLabel.setStyle("-fx-text-fill: #008000;");
                    }

                    case DUPLICATE -> {
                        showError(
                                "Duplicate Employee",
                                "An employee with the same Employee ID or email already exists."
                        );
                        statusLabel.setText("Duplicate Employee ID or email.");
                        statusLabel.setStyle("-fx-text-fill: #cc0000;");
                    }

                    case FAILED -> {
                        showError(
                                "Database Error",
                                "Failed to add employee. Please check the console for details."
                        );
                        statusLabel.setText("Failed to add employee.");
                        statusLabel.setStyle("-fx-text-fill: #cc0000;");
                    }
                }

            } else {
                statusLabel.setText("Employee ID, first name, last name, department, and position are required.");
                statusLabel.setStyle("-fx-text-fill: #cc0000;");
            }
        });
    }

    private boolean isEmployeeInputValid(Employee employee) {
        return employee.getEmployeeId() != null && !employee.getEmployeeId().isBlank()
                && employee.getFirstName() != null && !employee.getFirstName().isBlank()
                && employee.getLastName() != null && !employee.getLastName().isBlank()
                && employee.getDepartment() != null && !employee.getDepartment().isBlank()
                && employee.getPosition() != null && !employee.getPosition().isBlank();
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

    @FXML
    private void deactivateSelectedEmployee() {
        Employee selectedEmployee = employeeTable.getSelectionModel().getSelectedItem();

        if (selectedEmployee == null) {
            showError("No Employee Selected", "Please select an employee from the table first.");
            return;
        }

        if ("Inactive".equalsIgnoreCase(selectedEmployee.getStatus())) {
            showInfo("Already Inactive", "This employee is already inactive.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Deactivate Employee");
        confirmation.setHeaderText(null);
        confirmation.setContentText(
                "Are you sure you want to deactivate " + selectedEmployee.getFullName() + "?"
        );

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean updated = employeeRepository.deactivateEmployee(selectedEmployee.getEmployeeId());

                if (updated) {
                    loadEmployeesFromDatabase();
                    statusLabel.setText("Employee deactivated successfully.");
                    statusLabel.setStyle("-fx-text-fill: #008000;");
                    showInfo("Employee Deactivated", "Employee status was changed to Inactive.");
                } else {
                    statusLabel.setText("Failed to deactivate employee.");
                    statusLabel.setStyle("-fx-text-fill: #cc0000;");
                    showError("Database Error", "Failed to deactivate employee. Please check the console.");
                }
            }
        });
    }
}