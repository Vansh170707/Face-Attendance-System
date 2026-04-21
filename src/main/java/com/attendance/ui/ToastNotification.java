package com.attendance.ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Toast Notification — slides in from the bottom-right, auto-dismisses after 3 seconds.
 */
public class ToastNotification {

    public static void show(Stage stage, String title, String message, ToastType type) {
        if (stage == null || stage.getScene() == null) return;

        javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) stage.getScene().getRoot();

        // Create toast
        VBox toast = new VBox(5);
        toast.getStyleClass().add("toast-notification");
        toast.getStyleClass().add(switch (type) {
            case SUCCESS -> "toast-success";
            case ERROR -> "toast-error";
            case WARNING -> "toast-warning";
            case INFO -> "toast-info";
        });
        toast.setPadding(new Insets(15, 20, 15, 20));
        toast.setMaxWidth(320);
        toast.setAlignment(Pos.TOP_LEFT);

        String icon = switch (type) {
            case SUCCESS -> "✅";
            case ERROR -> "❌";
            case WARNING -> "⚠️";
            case INFO -> "ℹ️";
        };

        Label titleLabel = new Label(icon + "  " + title);
        titleLabel.getStyleClass().add("toast-title");

        Label msgLabel = new Label(message);
        msgLabel.getStyleClass().add("toast-message");
        msgLabel.setWrapText(true);

        toast.getChildren().addAll(titleLabel, msgLabel);

        // Position bottom right
        if (root instanceof StackPane sp) {
            StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(toast, new Insets(0, 20, 20, 0));
            sp.getChildren().add(toast);
        } else if (root instanceof BorderPane bp) {
            // Wrap in a StackPane overlay approach
            StackPane overlay = new StackPane(toast);
            overlay.setPickOnBounds(false);
            StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(toast, new Insets(0, 20, 20, 0));

            StackPane center = (StackPane) bp.getCenter();
            if (center != null) {
                center.getChildren().add(overlay);

                // Auto-remove after animation
                animateAndDismiss(toast, () -> center.getChildren().remove(overlay));
                return;
            }
        }

        // Default animation
        animateAndDismiss(toast, () -> {
            if (root instanceof StackPane sp) sp.getChildren().remove(toast);
        });
    }

    private static void animateAndDismiss(VBox toast, Runnable onComplete) {
        // Slide in
        toast.setTranslateX(350);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), toast);
        slideIn.setFromX(350);
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        // Fade out after delay
        PauseTransition pause = new PauseTransition(Duration.seconds(3));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> onComplete.run());

        SequentialTransition seq = new SequentialTransition(slideIn, pause, fadeOut);
        seq.play();
    }

    public enum ToastType {
        SUCCESS, ERROR, WARNING, INFO
    }
}
