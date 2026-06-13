package backend.employee;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class EmployeeController implements Initializable {

    @FXML private TableView<EmployeeDTO> employeeTable;
    @FXML private TableColumn<EmployeeDTO, String> idCol;
    @FXML private TableColumn<EmployeeDTO, String> nameCol;
    @FXML private TableColumn<EmployeeDTO, String> emailCol;
    @FXML private TableColumn<EmployeeDTO, String> departmentCol;
    @FXML private TableColumn<EmployeeDTO, String> positionCol;

    @FXML private TextField searchField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField positionField;
    @FXML private ComboBox<String> departmentComboBox;

    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;

    @FXML private Label totalEmployeesLabel;
    @FXML private Label activeSlipsLabel;
    @FXML private Label departmentsCountLabel;

    private final EmployeeService employeeService = new EmployeeService();
    private final ObservableList<EmployeeDTO> employeeList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadEmployeeData();
        setupDepartmentComboBox();
        setupSelectionListener();
    }

    private void setupTableColumns() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        departmentCol.setCellValueFactory(new PropertyValueFactory<>("department"));
        positionCol.setCellValueFactory(new PropertyValueFactory<>("position"));
    }

    private void loadEmployeeData() {
        employeeList.clear();
        employeeList.addAll(employeeService.getAllEmployees());
        employeeTable.setItems(employeeList);
        updateDashboardCards();
    }

    private void setupDepartmentComboBox() {
        if (departmentComboBox != null) {
            departmentComboBox.setItems(FXCollections.observableArrayList(
                    "IT Department", "HR Department", "Finance", "Procurement", "Legal", "Operations"
            ));
        }
    }

    private void setupSelectionListener() {
        employeeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                nameField.setText(newSelection.getFullName());
                emailField.setText(newSelection.getEmail());
                positionField.setText(newSelection.getPosition());
                departmentComboBox.setValue(newSelection.getDepartment());
            }
        });
    }

    private void updateDashboardCards() {
        if (totalEmployeesLabel != null) {
            totalEmployeesLabel.setText(String.valueOf(employeeList.size()));
        }
    }

    @FXML
    private void handleAddEmployee() {
        // Add employee logic execution
    }

    @FXML
    private void handleUpdateEmployee() {
        // Update employee logic execution
    }

    @FXML
    private void handleDeleteEmployee() {
        // Delete employee logic execution
    }

    @FXML
    private void handleClearFields() {
        nameField.clear();
        emailField.clear();
        positionField.clear();
        departmentComboBox.setValue(null);
        employeeTable.getSelectionModel().clearSelection();
    }
}