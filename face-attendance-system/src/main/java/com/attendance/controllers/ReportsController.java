package com.attendance.controllers;

import com.attendance.models.Attendance;
import com.attendance.services.AttendanceService;
import com.attendance.services.ReportService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Reports Controller - View and export attendance reports
 */
public class ReportsController implements Initializable {

    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private ComboBox<String> filterCombo;
    @FXML
    private TableView<Attendance> reportTable;
    @FXML
    private TableColumn<Attendance, String> dateColumn;
    @FXML
    private TableColumn<Attendance, String> nameColumn;
    @FXML
    private TableColumn<Attendance, String> deptColumn;
    @FXML
    private TableColumn<Attendance, String> checkInColumn;
    @FXML
    private TableColumn<Attendance, String> checkOutColumn;
    @FXML
    private TableColumn<Attendance, String> statusColumn;
    @FXML
    private Label totalRecordsLabel;

    private AttendanceService attendanceService;
    private ReportService reportService;
    private List<Attendance> currentRecords;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        attendanceService = new AttendanceService();
        reportService = new ReportService();

        setupTable();
        setupFilters();
        loadData();
    }

    private void setupTable() {
        dateColumn.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFormattedDate()));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        deptColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        checkInColumn.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFormattedCheckIn()));
        checkOutColumn.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFormattedCheckOut()));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Style status column
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("PRESENT".equals(status)) {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupFilters() {
        // Date pickers default to this week
        startDatePicker.setValue(LocalDate.now().minusDays(7));
        endDatePicker.setValue(LocalDate.now());

        // Filter options
        filterCombo.getItems().addAll("All", "Today", "This Week", "This Month", "Custom");
        filterCombo.setValue("This Week");

        filterCombo.setOnAction(e -> {
            switch (filterCombo.getValue()) {
                case "Today" -> {
                    startDatePicker.setValue(LocalDate.now());
                    endDatePicker.setValue(LocalDate.now());
                }
                case "This Week" -> {
                    startDatePicker.setValue(LocalDate.now().minusDays(7));
                    endDatePicker.setValue(LocalDate.now());
                }
                case "This Month" -> {
                    startDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
                    endDatePicker.setValue(LocalDate.now());
                }
                case "All" -> {
                    startDatePicker.setValue(LocalDate.of(2020, 1, 1));
                    endDatePicker.setValue(LocalDate.now());
                }
            }
            loadData();
        });
    }

    private void loadData() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            return;
        }

        currentRecords = attendanceService.getAttendanceByDateRange(start, end);
        reportTable.setItems(FXCollections.observableArrayList(currentRecords));
        totalRecordsLabel.setText("Total Records: " + currentRecords.size());
    }

    @FXML
    private void applyFilter() {
        loadData();
    }

    @FXML
    private void exportPDF() {
        if (currentRecords == null || currentRecords.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No records to export!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.setInitialFileName("attendance_report.pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fileChooser.showSaveDialog(reportTable.getScene().getWindow());
        if (file != null) {
            boolean success = reportService.generatePDFReport(
                    currentRecords,
                    file.getAbsolutePath(),
                    startDatePicker.getValue(),
                    endDatePicker.getValue());

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "📄 Report exported to: " + file.getName());
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to export report.");
            }
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) reportTable.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
