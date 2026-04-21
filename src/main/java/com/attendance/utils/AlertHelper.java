package com.attendance.utils;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import java.util.Optional;

/**
 * Custom-styled alert dialogs matching the dark futuristic theme.
 * Replaces default JavaFX alerts with professional-looking dialogs.
 */
public class AlertHelper {

    public static void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.initStyle(StageStyle.UNDECORATED);

        // Apply custom styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                AlertHelper.class.getResource("/styles/main.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-dialog");

        // Style based on type
        String icon = switch (type) {
            case INFORMATION -> "✅";
            case WARNING -> "⚠️";
            case ERROR -> "❌";
            case CONFIRMATION -> "❓";
            default -> "ℹ️";
        };
        alert.setHeaderText(icon + "  " + header);

        alert.showAndWait();
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText("❓  " + title);
        alert.setContentText(message);
        alert.initStyle(StageStyle.UNDECORATED);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                AlertHelper.class.getResource("/styles/main.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-dialog");

        // Custom buttons
        ButtonType yesBtn = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noBtn = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesBtn, noBtn);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesBtn;
    }
}
