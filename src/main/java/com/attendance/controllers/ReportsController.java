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

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Reports Controller — View and export attendance reports.
 * Now embedded inside the main layout with search and Excel export.
 */
public class ReportsController implements Initializable {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> filterCombo;
    @FXML private TextField searchField;
    @FXML private TableView<Attendance> reportTable;
    @FXML private TableColumn<Attendance, String> dateColumn;
    @FXML private TableColumn<Attendance, String> nameColumn;
    @FXML private TableColumn<Attendance, String> deptColumn;
    @FXML private TableColumn<Attendance, String> checkInColumn;
    @FXML private TableColumn<Attendance, String> checkOutColumn;
    @FXML private TableColumn<Attendance, String> statusColumn;
    @FXML private Label totalRecordsLabel;

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

        // Live search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearch(newVal));
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
                        setStyle("-fx-text-fill: #00C896; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #FF6B6B; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupFilters() {
        startDatePicker.setValue(LocalDate.now().minusDays(7));
        endDatePicker.setValue(LocalDate.now());

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
        if (start == null || end == null) return;

        currentRecords = attendanceService.getAttendanceByDateRange(start, end);
        reportTable.setItems(FXCollections.observableArrayList(currentRecords));
        totalRecordsLabel.setText("Total Records: " + currentRecords.size());
    }

    private void applySearch(String query) {
        if (currentRecords == null) return;
        if (query == null || query.isEmpty()) {
            reportTable.setItems(FXCollections.observableArrayList(currentRecords));
            totalRecordsLabel.setText("Total Records: " + currentRecords.size());
            return;
        }
        String lower = query.toLowerCase();
        List<Attendance> filtered = currentRecords.stream()
                .filter(r -> (r.getUserName() != null && r.getUserName().toLowerCase().contains(lower))
                        || (r.getDepartment() != null && r.getDepartment().toLowerCase().contains(lower)))
                .toList();
        reportTable.setItems(FXCollections.observableArrayList(filtered));
        totalRecordsLabel.setText("Showing: " + filtered.size() + " of " + currentRecords.size());
    }

    @FXML
    private void applyFilter() {
        loadData();
    }

    @FXML
    private void exportPDF() {
        if (currentRecords == null || currentRecords.isEmpty()) {
            com.attendance.utils.AlertHelper.showAlert(Alert.AlertType.WARNING, "No Data", "Empty Report", "No records to export!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.setInitialFileName("attendance_report.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fileChooser.showSaveDialog(reportTable.getScene().getWindow());
        if (file != null) {
            boolean success = reportService.generatePDFReport(currentRecords, file.getAbsolutePath(),
                    startDatePicker.getValue(), endDatePicker.getValue());
            if (success) {
                com.attendance.utils.AlertHelper.showAlert(Alert.AlertType.INFORMATION, "Success", "Export Complete",
                        "📄 Report exported to: " + file.getName());
            } else {
                com.attendance.utils.AlertHelper.showAlert(Alert.AlertType.ERROR, "Error", "Export Failed", "Failed to export report.");
            }
        }
    }

    @FXML
    private void exportExcel() {
        if (currentRecords == null || currentRecords.isEmpty()) {
            com.attendance.utils.AlertHelper.showAlert(Alert.AlertType.WARNING, "No Data", "Empty Report", "No records to export!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel Report");
        fileChooser.setInitialFileName("attendance_report.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

        File file = fileChooser.showSaveDialog(reportTable.getScene().getWindow());
        if (file != null) {
            boolean success = reportService.generateExcelReport(currentRecords, file.getAbsolutePath(),
                    startDatePicker.getValue(), endDatePicker.getValue());
            if (success) {
                com.attendance.utils.AlertHelper.showAlert(Alert.AlertType.INFORMATION, "Success", "Export Complete",
                        "📊 Excel exported to: " + file.getName());
            } else {
                com.attendance.utils.AlertHelper.showAlert(Alert.AlertType.ERROR, "Error", "Export Failed", "Failed to export Excel.");
            }
        }
    }

    @FXML
    private void markAbsent() {
        // Build a custom dialog to pick student + date
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Mark Absent");
        dialog.setHeaderText("🚫 Manually Mark Student as Absent");
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);

        // Dialog content
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(15);
        content.setStyle("-fx-padding: 20;");

        // Student picker
        Label studentLabel = new Label("Select Student:");
        studentLabel.setStyle("-fx-text-fill: #C0D0E0; -fx-font-weight: bold;");
        ComboBox<String> studentCombo = new ComboBox<>();
        studentCombo.setMaxWidth(Double.MAX_VALUE);
        studentCombo.setPromptText("Choose a student...");
        studentCombo.getStyleClass().add("modern-combo");

        // Load all users
        List<com.attendance.models.User> allUsers = attendanceService.getAllUsers();
        for (com.attendance.models.User user : allUsers) {
            studentCombo.getItems().add(user.getId() + " — " + user.getName() + " (" + user.getDepartment() + ")");
        }

        // Date picker
        Label dateLabel = new Label("Date:");
        dateLabel.setStyle("-fx-text-fill: #C0D0E0; -fx-font-weight: bold;");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.getStyleClass().add("modern-date");

        content.getChildren().addAll(studentLabel, studentCombo, dateLabel, datePicker);

        // Style the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-dialog");
        dialogPane.setMinWidth(380);

        // Style OK button
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setText("✅ Mark Absent");
        okButton.getStyleClass().addAll("action-btn", "danger-btn");

        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Cancel");
        cancelButton.getStyleClass().addAll("action-btn", "secondary-btn");

        // Handle result
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String selected = studentCombo.getValue();
                LocalDate selectedDate = datePicker.getValue();

                if (selected == null || selectedDate == null) {
                    com.attendance.utils.AlertHelper.showAlert(Alert.AlertType.WARNING, "Missing Info",
                            "Incomplete Selection", "Please select both a student and a date.");
                    return;
                }

                // Parse user ID from selection string "ID — Name (Dept)"
                int userId = Integer.parseInt(selected.split(" — ")[0].trim());
                String userName = selected.split(" — ")[1].split(" \\(")[0].trim();

                boolean success = attendanceService.markAbsent(userId, userName, selectedDate);
                if (success) {
                    com.attendance.utils.AlertHelper.showAlert(Alert.AlertType.INFORMATION, "Marked Absent",
                            "Success", "🚫 " + userName + " marked ABSENT for " + selectedDate);
                    loadData(); // Refresh table
                } else {
                    com.attendance.utils.AlertHelper.showAlert(Alert.AlertType.WARNING, "Already Exists",
                            "Duplicate Record", userName + " already has an attendance record for " + selectedDate);
                }
            }
        });
    }
}
