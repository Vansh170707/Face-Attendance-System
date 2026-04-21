package com.attendance.controllers;

import com.attendance.models.User;
import com.attendance.services.AttendanceService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Students Controller — Displays registered students as profile cards in a grid.
 */
public class StudentsController implements Initializable {

    @FXML private FlowPane studentGrid;
    @FXML private TextField searchField;
    @FXML private Label studentCountLabel;

    private AttendanceService attendanceService;
    private List<User> allUsers;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        attendanceService = new AttendanceService();
        loadStudents();

        // Live search filter
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterStudents(newVal));
    }

    private void loadStudents() {
        allUsers = attendanceService.getAllUsers();
        studentCountLabel.setText(allUsers.size() + " students");
        displayStudents(allUsers);
    }

    private void filterStudents(String query) {
        if (query == null || query.isEmpty()) {
            displayStudents(allUsers);
            return;
        }
        String lowerQuery = query.toLowerCase();
        List<User> filtered = allUsers.stream()
                .filter(u -> u.getName().toLowerCase().contains(lowerQuery)
                        || (u.getDepartment() != null && u.getDepartment().toLowerCase().contains(lowerQuery))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(lowerQuery)))
                .toList();
        displayStudents(filtered);
    }

    private void displayStudents(List<User> users) {
        studentGrid.getChildren().clear();

        if (users.isEmpty()) {
            Label empty = new Label("No students found");
            empty.getStyleClass().add("placeholder-text");
            studentGrid.getChildren().add(empty);
            return;
        }

        for (User user : users) {
            VBox card = createStudentCard(user);
            studentGrid.getChildren().add(card);
        }
    }

    private VBox createStudentCard(User user) {
        VBox card = new VBox(10);
        card.getStyleClass().add("student-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(220);
        card.setPrefHeight(180);
        card.setPadding(new Insets(20, 15, 20, 15));

        // Avatar
        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 36px;");

        // Name
        Label nameLabel = new Label(user.getName());
        nameLabel.getStyleClass().add("student-card-name");
        nameLabel.setWrapText(true);

        // Department
        Label deptLabel = new Label(user.getDepartment() != null ? user.getDepartment() : "—");
        deptLabel.getStyleClass().add("student-card-dept");

        // Email
        Label emailLabel = new Label(user.getEmail() != null ? user.getEmail() : "");
        emailLabel.getStyleClass().add("student-card-email");
        emailLabel.setWrapText(true);

        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().addAll("action-btn-small", "danger-btn-small");
        deleteBtn.setOnAction(e -> deleteStudent(user));

        actions.getChildren().addAll(deleteBtn);

        card.getChildren().addAll(avatar, nameLabel, deptLabel, emailLabel, actions);
        return card;
    }

    private void deleteStudent(User user) {
        boolean confirmed = com.attendance.utils.AlertHelper.showConfirmation(
                "Delete Student", "Are you sure you want to delete '" + user.getName() + "'? This cannot be undone.");
        if (confirmed) {
            // TODO: Implement actual deletion via service
            System.out.println("Delete: " + user.getName());
            loadStudents();
        }
    }
}
