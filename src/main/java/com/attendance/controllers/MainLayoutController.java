package com.attendance.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Main Layout Controller — manages sidebar navigation, header clock, and content switching.
 * v2.0: View caching, slide+fade transitions, hover animations.
 */
public class MainLayoutController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private StackPane contentArea;
    @FXML private Label clockLabel;
    @FXML private Label pageTitle;
    @FXML private Label brandLabel;
    @FXML private Label brandSubLabel;

    @FXML private Button navDashboard;
    @FXML private Button navAttendance;
    @FXML private Button navStudents;
    @FXML private Button navReports;
    @FXML private Button navRegister;
    @FXML private Button sidebarToggle;

    private Button activeNavButton;
    private boolean sidebarCollapsed = false;
    private String currentView = "";

    // View cache for instant switching
    private final Map<String, Parent> viewCache = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupClock();
        showDashboard();

        // Preload views in background for instant switching
        javafx.application.Platform.runLater(() -> {
            preloadView("/views/reports.fxml");
            preloadView("/views/students.fxml");
        });

        // Setup keyboard shortcuts when scene is ready
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                setupKeyboardShortcuts();
            }
        });

        // Add subtle hover scale to nav buttons
        addHoverAnimation(navDashboard);
        addHoverAnimation(navAttendance);
        addHoverAnimation(navStudents);
        addHoverAnimation(navReports);
        addHoverAnimation(navRegister);
    }

    private void setupClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalDateTime now = LocalDateTime.now();
            clockLabel.setText(now.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  │  hh:mm:ss a")));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void setupKeyboardShortcuts() {
        rootPane.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN), this::showDashboard);
        rootPane.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN), this::showAttendance);
        rootPane.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN), this::showReports);
        rootPane.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::showRegistration);
    }

    /** Subtle hover scale effect on nav buttons */
    private void addHoverAnimation(Button btn) {
        if (btn == null) return;
        btn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
        });
        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    // ==================== View Preloading ====================

    private void preloadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            viewCache.put(fxmlPath, view);
        } catch (Exception e) {
            // Ignore — will load on demand
        }
    }

    // ==================== Navigation ====================

    @FXML
    private void showDashboard() {
        // Dashboard: always reload for fresh data
        loadContent("/views/dashboard.fxml", "Dashboard", navDashboard, false);
    }

    @FXML
    private void showAttendance() {
        // Attendance: always reload (camera needs fresh init)
        loadContent("/views/attendance.fxml", "Mark Attendance", navAttendance, false);
    }

    @FXML
    private void showStudents() {
        loadContent("/views/students.fxml", "Manage Students", navStudents, false);
    }

    @FXML
    private void showReports() {
        loadContent("/views/reports.fxml", "Attendance Reports", navReports, false);
    }

    @FXML
    private void showRegistration() {
        loadContent("/views/registration.fxml", "Register New User", navRegister, false);
    }

    private void loadContent(String fxmlPath, String title, Button navBtn, boolean useCache) {
        // Skip if already on this view
        if (fxmlPath.equals(currentView)) return;
        currentView = fxmlPath;

        try {
            Parent content;
            if (useCache && viewCache.containsKey(fxmlPath)) {
                content = viewCache.get(fxmlPath);
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                content = loader.load();
            }

            // Slide + Fade transition
            content.setOpacity(0);
            content.setTranslateY(18);
            contentArea.getChildren().setAll(content);

            // Fade in
            FadeTransition fade = new FadeTransition(Duration.millis(280), content);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_OUT);

            // Slide up
            TranslateTransition slide = new TranslateTransition(Duration.millis(320), content);
            slide.setFromY(18);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));

            ParallelTransition transition = new ParallelTransition(fade, slide);
            transition.play();

            // Update header with smooth text change
            pageTitle.setText(title);

            // Update active nav
            setActiveNav(navBtn);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Failed to load: " + fxmlPath + " — " + e.getMessage());
        }
    }

    private void setActiveNav(Button navBtn) {
        if (navBtn == null) return;
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("nav-btn-active");
        }
        navBtn.getStyleClass().add("nav-btn-active");
        activeNavButton = navBtn;
    }

    // ==================== Sidebar Toggle ====================

    @FXML
    private void toggleSidebar() {
        double targetWidth = sidebarCollapsed ? 240 : 65;
        sidebarCollapsed = !sidebarCollapsed;

        // Smooth width animation with easing
        KeyValue kv = new KeyValue(sidebar.prefWidthProperty(), targetWidth, Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));
        KeyFrame kf = new KeyFrame(Duration.millis(280), kv);
        Timeline timeline = new Timeline(kf);
        timeline.play();

        // Toggle label visibility with fade
        if (sidebarCollapsed) {
            FadeTransition ft = new FadeTransition(Duration.millis(150), brandLabel);
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                brandLabel.setVisible(false);
                brandLabel.setManaged(false);
                brandSubLabel.setVisible(false);
                brandSubLabel.setManaged(false);
            });
            ft.play();
        } else {
            brandLabel.setVisible(true);
            brandLabel.setManaged(true);
            brandSubLabel.setVisible(true);
            brandSubLabel.setManaged(true);
            brandLabel.setOpacity(0);
            brandSubLabel.setOpacity(0);

            FadeTransition ft = new FadeTransition(Duration.millis(250), brandLabel);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(100));
            ft.play();

            FadeTransition ft2 = new FadeTransition(Duration.millis(250), brandSubLabel);
            ft2.setFromValue(0);
            ft2.setToValue(1);
            ft2.setDelay(Duration.millis(150));
            ft2.play();
        }
    }
}
