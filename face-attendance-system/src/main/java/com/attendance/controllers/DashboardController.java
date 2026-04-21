package com.attendance.controllers;

import com.attendance.services.AttendanceService;
import com.attendance.models.Attendance;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Dashboard Controller - Main screen of the application
 */
public class DashboardController implements Initializable {

    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label presentTodayLabel;
    @FXML
    private Label absentTodayLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private TableView<Attendance> recentTable;
    @FXML
    private TableColumn<Attendance, String> nameColumn;
    @FXML
    private TableColumn<Attendance, String> deptColumn;
    @FXML
    private TableColumn<Attendance, String> timeColumn;
    @FXML
    private TableColumn<Attendance, String> statusColumn;

    private AttendanceService attendanceService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        attendanceService = new AttendanceService();
        setupTable();
        refreshDashboard();

        // Set current date
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        deptColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        timeColumn.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFormattedCheckIn()));
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

    public void refreshDashboard() {
        int totalUsers = attendanceService.getTotalUsersCount();
        int presentToday = attendanceService.getTodayPresentCount();
        int absentToday = Math.max(0, totalUsers - presentToday);

        totalUsersLabel.setText(String.valueOf(totalUsers));
        presentTodayLabel.setText(String.valueOf(presentToday));
        absentTodayLabel.setText(String.valueOf(absentToday));

        // Load today's attendance
        List<Attendance> todayRecords = attendanceService.getTodayAttendance();
        recentTable.setItems(FXCollections.observableArrayList(todayRecords));
    }

    @FXML
    private void openRegistration() {
        loadView("/views/registration.fxml", "👤 Register New User");
    }

    @FXML
    private void openAttendance() {
        loadView("/views/attendance.fxml", "📷 Mark Attendance");
    }

    @FXML
    private void openReports() {
        loadView("/views/reports.fxml", "📊 Attendance Reports");
    }

    @FXML
    private void refreshData() {
        refreshDashboard();
        showAlert(Alert.AlertType.INFORMATION, "Refreshed", "Dashboard data refreshed!");
    }

    private void loadView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = new Stage();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            stage.setTitle(title);
            stage.setScene(scene);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.showAndWait();

            // Refresh after closing
            refreshDashboard();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
