package com.attendance.controllers;

import com.attendance.services.AttendanceService;
import com.attendance.models.Attendance;
import javafx.animation.*;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Dashboard Controller — embedded inside MainLayout content area.
 * v2.0: Animated counters, hover scale on cards, activity feed.
 */
public class DashboardController implements Initializable {

    @FXML private Label totalUsersLabel;
    @FXML private Label presentTodayLabel;
    @FXML private Label absentTodayLabel;
    @FXML private Label attendancePercentLabel;
    @FXML private TableView<Attendance> recentTable;
    @FXML private TableColumn<Attendance, String> nameColumn;
    @FXML private TableColumn<Attendance, String> deptColumn;
    @FXML private TableColumn<Attendance, String> timeColumn;
    @FXML private TableColumn<Attendance, String> statusColumn;
    @FXML private ListView<String> activityFeed;

    private AttendanceService attendanceService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        attendanceService = new AttendanceService();
        setupTable();
        refreshDashboard();

        // Add hover animations to stat cards once they're rendered
        javafx.application.Platform.runLater(this::setupStatCardAnimations);
    }

    /** Apply hover scale effect to each stat card */
    private void setupStatCardAnimations() {
        try {
            Node root = totalUsersLabel.getParent().getParent(); // HBox containing cards
            if (root instanceof HBox hbox) {
                for (Node card : hbox.getChildren()) {
                    addCardHoverEffect(card);
                }
            }
        } catch (Exception e) {
            // Silent — not critical
        }
    }

    private void addCardHoverEffect(Node card) {
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.04);
            st.setToY(1.04);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        deptColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        timeColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getFormattedCheckIn()));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Styled status column
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    // Create a styled badge
                    Label badge = new Label(status);
                    badge.setStyle(
                        "PRESENT".equals(status)
                            ? "-fx-text-fill: #00E5A0; -fx-font-weight: bold; -fx-font-size: 12px; " +
                              "-fx-background-color: rgba(0,229,160,0.1); -fx-padding: 3 10; " +
                              "-fx-background-radius: 6;"
                            : "-fx-text-fill: #FF8787; -fx-font-weight: bold; -fx-font-size: 12px; " +
                              "-fx-background-color: rgba(255,107,107,0.1); -fx-padding: 3 10; " +
                              "-fx-background-radius: 6;"
                    );
                    setText(null);
                    setGraphic(badge);
                }
            }
        });
    }

    public void refreshDashboard() {
        int totalUsers = attendanceService.getTotalUsersCount();
        int presentToday = attendanceService.getTodayPresentCount();
        int absentToday = Math.max(0, totalUsers - presentToday);
        double percentage = totalUsers > 0 ? (presentToday * 100.0 / totalUsers) : 0;

        // Animated number counting with smoother easing
        animateNumber(totalUsersLabel, totalUsers);
        animateNumber(presentTodayLabel, presentToday);
        animateNumber(absentTodayLabel, absentToday);

        // Animated percentage
        SimpleIntegerProperty percentProp = new SimpleIntegerProperty(0);
        percentProp.addListener((obs, o, n) -> attendancePercentLabel.setText(n.intValue() + "%"));
        Timeline pctTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(percentProp, 0)),
            new KeyFrame(Duration.millis(900), new KeyValue(percentProp, (int) percentage, Interpolator.EASE_OUT))
        );
        pctTimeline.play();

        // Load today's attendance
        List<Attendance> todayRecords = attendanceService.getTodayAttendance();
        recentTable.setItems(FXCollections.observableArrayList(todayRecords));

        // Populate activity feed with staggered fade-in
        activityFeed.getItems().clear();
        if (todayRecords.isEmpty()) {
            activityFeed.getItems().add("ℹ️  No attendance marked today yet.");
        } else {
            for (Attendance record : todayRecords) {
                String icon = "PRESENT".equals(record.getStatus()) ? "✅" : "🚫";
                activityFeed.getItems().add(
                    icon + "  " + record.getUserName() + " — " + record.getStatus().toLowerCase() +
                    " at " + record.getFormattedCheckIn()
                );
            }
        }
    }

    private void animateNumber(Label label, int targetValue) {
        SimpleIntegerProperty counter = new SimpleIntegerProperty(0);
        counter.addListener((obs, oldVal, newVal) -> label.setText(String.valueOf(newVal.intValue())));

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(counter, 0)),
            new KeyFrame(Duration.millis(900), new KeyValue(counter, targetValue, Interpolator.EASE_OUT))
        );
        timeline.play();
    }

    // Quick action handlers — navigate via parent BorderPane
    @FXML
    private void quickMarkAttendance() { navigateViaMainLayout("attendance"); }

    @FXML
    private void quickRegister() { navigateViaMainLayout("register"); }

    @FXML
    private void quickReports() { navigateViaMainLayout("reports"); }

    @FXML
    private void refreshData() {
        refreshDashboard();
        com.attendance.utils.AlertHelper.showAlert(
            Alert.AlertType.INFORMATION, "Refreshed", "Success", "Dashboard data refreshed!");
    }

    private void navigateViaMainLayout(String target) {
        try {
            Node root = totalUsersLabel.getScene().getRoot();
            if (root instanceof javafx.scene.layout.BorderPane bp) {
                javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) bp.getCenter();
                String fxmlPath = switch (target) {
                    case "attendance" -> "/views/attendance.fxml";
                    case "register" -> "/views/registration.fxml";
                    case "reports" -> "/views/reports.fxml";
                    default -> "/views/dashboard.fxml";
                };

                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
                javafx.scene.Parent content = loader.load();

                // Slide+fade
                content.setOpacity(0);
                content.setTranslateY(18);
                contentArea.getChildren().setAll(content);

                FadeTransition fade = new FadeTransition(Duration.millis(280), content);
                fade.setFromValue(0);
                fade.setToValue(1);

                TranslateTransition slide = new TranslateTransition(Duration.millis(320), content);
                slide.setFromY(18);
                slide.setToY(0);
                slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));

                new ParallelTransition(fade, slide).play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
