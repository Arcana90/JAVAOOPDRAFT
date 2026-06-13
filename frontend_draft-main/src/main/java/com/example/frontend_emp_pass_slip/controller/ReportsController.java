package com.example.frontend_emp_pass_slip.controller;

import backend.passslip.ReportDepartmentSummary;
import backend.passslip.ReportsJdbcRepository;
import backend.passslip.ReportsStats;
import backend.passslip.DailyActivitySummary;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReportsController {

    @FXML private Label totalSlipsLabel;
    @FXML private Label currentlyOutLabel;
    @FXML private Label officialLabel;
    @FXML private Label avgDurationLabel;

    @FXML private TableView<ReportDepartmentSummary> reportsTable;
    @FXML private TableColumn<ReportDepartmentSummary, String> departmentCol;
    @FXML private TableColumn<ReportDepartmentSummary, Integer> totalCol;
    @FXML private TableColumn<ReportDepartmentSummary, Integer> officialCol;
    @FXML private TableColumn<ReportDepartmentSummary, Integer> personalCol;
    @FXML private TableColumn<ReportDepartmentSummary, String> avgDurationCol;

    @FXML private BarChart<String, Number> activityChart;
    @FXML private CategoryAxis dayAxis;
    @FXML private NumberAxis valueAxis;

    private final ReportsJdbcRepository reportsRepository = new ReportsJdbcRepository();

    private final XYChart.Series<String, Number> officialSeries = new XYChart.Series<>();
    private final XYChart.Series<String, Number> personalSeries = new XYChart.Series<>();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupChart();
        loadReportsFromDatabase();
    }

    private void setupTableColumns() {
        departmentCol.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getDepartment()));

        totalCol.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getTotalSlips()));

        officialCol.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getOfficial()));

        personalCol.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getPersonal()));

        avgDurationCol.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getAvgDuration()));
    }

    private void setupChart() {
        officialSeries.setName("Official");
        personalSeries.setName("Personal");

        activityChart.getData().clear();
        activityChart.getData().addAll(List.of(officialSeries, personalSeries));

        dayAxis.setCategories(FXCollections.observableArrayList("Mon", "Tue", "Wed", "Thu", "Fri"));

        valueAxis.setAutoRanging(true);
        valueAxis.setTickUnit(1);
        valueAxis.setMinorTickCount(0);
    }

    private void loadReportsFromDatabase() {
        ReportsStats stats = reportsRepository.getStats();

        totalSlipsLabel.setText(String.valueOf(stats.getTotalSlips()));
        currentlyOutLabel.setText(String.valueOf(stats.getCurrentlyOut()));
        officialLabel.setText(String.valueOf(stats.getOfficial()));
        avgDurationLabel.setText(stats.getAvgDuration());

        reportsTable.setItems(FXCollections.observableArrayList(
                reportsRepository.findDepartmentSummaries()
        ));

        updateChartDataWithDatabaseValues();
    }

    private void updateChartDataWithDatabaseValues() {
        officialSeries.getData().clear();
        personalSeries.getData().clear();

        List<DailyActivitySummary> actualDbData = reportsRepository.findWeeklyDailyActivity();

        Map<String, DailyActivitySummary> dataMap = new HashMap<>();
        for (DailyActivitySummary summary : actualDbData) {
            dataMap.put(summary.getDay(), summary);
        }

        String[] businessDays = {"Mon", "Tue", "Wed", "Thu", "Fri"};

        for (String day : businessDays) {
            int officialCount = 0;
            int personalCount = 0;

            if (dataMap.containsKey(day)) {
                officialCount = dataMap.get(day).getOfficialCount();
                personalCount = dataMap.get(day).getPersonalCount();
            }

            XYChart.Data<String, Number> officialDataPoint = new XYChart.Data<>(day, officialCount);
            XYChart.Data<String, Number> personalDataPoint = new XYChart.Data<>(day, personalCount);

            officialSeries.getData().add(officialDataPoint);
            personalSeries.getData().add(personalDataPoint);

            setupDeferredTooltipBinding(officialDataPoint, day, officialCount, personalCount);
            setupDeferredTooltipBinding(personalDataPoint, day, officialCount, personalCount);
        }
    }

    private void setupDeferredTooltipBinding(XYChart.Data<String, Number> dataPoint, String day, int officialVal, int personalVal) {
        if (dataPoint.getNode() != null) {
            Tooltip.install(dataPoint.getNode(), createActivityTooltip(day, officialVal, personalVal));
        }

        dataPoint.nodeProperty().addListener((observable, oldNode, newNode) -> {
            if (newNode != null) {
                Tooltip.install(newNode, createActivityTooltip(day, officialVal, personalVal));
            }
        });
    }

    /**
     * Factory method to generate a clean, modern customized Tooltip instance.
     */
    private Tooltip createActivityTooltip(String day, int officialVal, int personalVal) {
        VBox layoutBox = new VBox(4);
        layoutBox.getStyleClass().add("chart-hover-popup");

        Label titleLabel = new Label(day);
        titleLabel.getStyleClass().add("chart-hover-title");

        Label officialLabelTxt = new Label("Official : " + officialVal);
        officialLabelTxt.getStyleClass().add("chart-hover-official");

        Label personalLabelTxt = new Label("Personal : " + personalVal);
        personalLabelTxt.getStyleClass().add("chart-hover-personal");

        layoutBox.getChildren().addAll(titleLabel, officialLabelTxt, personalLabelTxt);

        Tooltip tooltip = new Tooltip();
        tooltip.setGraphic(layoutBox);

        // Instant hover timings
        tooltip.setShowDelay(Duration.millis(10));
        tooltip.setHideDelay(Duration.millis(10));
        tooltip.setShowDuration(Duration.INDEFINITE);

        // Clear the default tooltip UI styling layer
        tooltip.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        // CRITICAL FIX: Explicitly links your style.css directly into this separate window layout layer
        try {
            String cssPath = Objects.requireNonNull(getClass().getResource("/com/example/frontend_emp_pass_slip/css/style.css")).toExternalForm();
            tooltip.getScene().getStylesheets().add(cssPath);
        } catch (Exception ignored) {
            // Fallback safety if path configuration shifts locally
        }

        return tooltip;
    }
}