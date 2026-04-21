package com.attendance;

import com.attendance.database.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.geometry.Pos;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;

/**
 * Face Recognition Attendance System
 * Main Application Entry Point — v2.0 Premium
 */
public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showSplashScreen(stage, () -> {
            try {
                launchMainApp(stage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void showSplashScreen(Stage stage, Runnable onComplete) {
        VBox splashRoot = new VBox(18);
        splashRoot.setAlignment(Pos.CENTER);
        splashRoot.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #0A1628, #0D1B30, #0A1628);" +
            "-fx-padding: 50;"
        );

        // Animated logo with glow
        Label logoLabel = new Label("🎯");
        logoLabel.setStyle("-fx-font-size: 72px;");
        DropShadow logoGlow = new DropShadow(25, Color.web("#1E90FF", 0.7));
        logoLabel.setEffect(logoGlow);

        // Pulsing logo glow animation
        Timeline pulseGlow = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(logoGlow.radiusProperty(), 15)),
            new KeyFrame(Duration.millis(800), new KeyValue(logoGlow.radiusProperty(), 35)),
            new KeyFrame(Duration.millis(1600), new KeyValue(logoGlow.radiusProperty(), 15))
        );
        pulseGlow.setCycleCount(Timeline.INDEFINITE);
        pulseGlow.play();

        // Scale bounce on logo
        ScaleTransition logoScale = new ScaleTransition(Duration.millis(600), logoLabel);
        logoScale.setFromX(0.5);
        logoScale.setFromY(0.5);
        logoScale.setToX(1.0);
        logoScale.setToY(1.0);
        logoScale.setInterpolator(Interpolator.EASE_OUT);
        logoScale.play();

        Label titleLabel = new Label("FaceAttend");
        titleLabel.setStyle(
            "-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: #1E90FF;" +
            "-fx-effect: dropshadow(gaussian, rgba(30,144,255,0.4), 10, 0, 0, 0);"
        );
        titleLabel.setOpacity(0);

        Label subtitleLabel = new Label("Smart Attendance System v2.0");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #556677;");
        subtitleLabel.setOpacity(0);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(320);
        progressBar.setPrefHeight(6);
        progressBar.setOpacity(0);

        Label loadingLabel = new Label("Initializing...");
        loadingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4A5A6A;");
        loadingLabel.setOpacity(0);

        // Fade in all elements staggered
        FadeTransition ftTitle = new FadeTransition(Duration.millis(400), titleLabel);
        ftTitle.setFromValue(0); ftTitle.setToValue(1); ftTitle.setDelay(Duration.millis(300));

        FadeTransition ftSub = new FadeTransition(Duration.millis(400), subtitleLabel);
        ftSub.setFromValue(0); ftSub.setToValue(1); ftSub.setDelay(Duration.millis(500));

        FadeTransition ftBar = new FadeTransition(Duration.millis(300), progressBar);
        ftBar.setFromValue(0); ftBar.setToValue(1); ftBar.setDelay(Duration.millis(700));

        FadeTransition ftLoading = new FadeTransition(Duration.millis(300), loadingLabel);
        ftLoading.setFromValue(0); ftLoading.setToValue(1); ftLoading.setDelay(Duration.millis(800));

        ParallelTransition staggered = new ParallelTransition(ftTitle, ftSub, ftBar, ftLoading);
        staggered.play();

        splashRoot.getChildren().addAll(logoLabel, titleLabel, subtitleLabel, progressBar, loadingLabel);

        Scene splashScene = new Scene(splashRoot, 520, 420);
        splashScene.setFill(Color.TRANSPARENT);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(splashScene);
        stage.show();

        // Animate progress with loading messages
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(progressBar.progressProperty(), 0)),
            new KeyFrame(Duration.millis(500),
                e -> loadingLabel.setText("Connecting to database..."),
                new KeyValue(progressBar.progressProperty(), 0.25)),
            new KeyFrame(Duration.millis(900),
                e -> {
                    loadingLabel.setText("Initializing database tables...");
                    DatabaseInitializer.initialize();
                },
                new KeyValue(progressBar.progressProperty(), 0.50)),
            new KeyFrame(Duration.millis(1300),
                e -> loadingLabel.setText("Loading face recognition models..."),
                new KeyValue(progressBar.progressProperty(), 0.75)),
            new KeyFrame(Duration.millis(1700),
                e -> loadingLabel.setText("Preparing UI components..."),
                new KeyValue(progressBar.progressProperty(), 0.90)),
            new KeyFrame(Duration.millis(2100),
                e -> loadingLabel.setText("✅ Ready!"),
                new KeyValue(progressBar.progressProperty(), 1.0)),
            new KeyFrame(Duration.millis(2500), e -> {
                // Fade out splash
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), splashRoot);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> {
                    stage.close();
                    pulseGlow.stop();
                    Stage mainStage = new Stage();
                    primaryStage = mainStage;
                    onComplete.run();
                });
                fadeOut.play();
            })
        );
        timeline.play();
    }

    private void launchMainApp(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main_layout.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1350, 870);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        primaryStage.setTitle("FaceAttend — Smart Attendance System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(750);
        primaryStage.show();

        // Smooth fade in for main app
        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(500), root);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_OUT);
        ft.play();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
