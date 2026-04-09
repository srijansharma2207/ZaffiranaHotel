package com.hotel.controller;

import com.hotel.dao.AppUserDAO;
import com.hotel.dao.GuestDAO;
import com.hotel.model.AppUser;
import com.hotel.model.Guest;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UsersController {

    @FXML private TableView<AppUser> usersTable;
    @FXML private TableColumn<AppUser, Integer> colUserId;
    @FXML private TableColumn<AppUser, String> colEmail;
    @FXML private TableColumn<AppUser, String> colName;
    @FXML private TableColumn<AppUser, String> colRole;

    private final AppUserDAO appUserDAO = new AppUserDAO();
    private final GuestDAO guestDAO = new GuestDAO();

    @FXML
    public void initialize() {
        colUserId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getUserId()).asObject());
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));
        loadUsers();
    }

    @FXML
    public void loadUsers() {
        try {
            List<AppUser> users = appUserDAO.listUsers();
            usersTable.setItems(FXCollections.observableArrayList(users));
        } catch (SQLException e) {
            showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void openCreateUserDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create User");
        dialog.setHeaderText("Create a USER account (email login)");

        ButtonType createBtn = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));

        TextField tfEmail = new TextField();
        tfEmail.setPromptText("user@example.com");
        TextField tfName = new TextField();
        tfName.setPromptText("Full name");
        TextField tfPhone = new TextField();
        tfPhone.setPromptText("Phone");
        TextField tfIdProof = new TextField();
        tfIdProof.setPromptText("Aadhar/Passport");
        ComboBox<String> cbGender = new ComboBox<>();
        cbGender.getItems().setAll("Male", "Female", "Other");
        cbGender.setPromptText("Gender");
        PasswordField pfPass = new PasswordField();
        pfPass.setPromptText("Password");

        grid.add(new Label("Email:"), 0, 0);
        grid.add(tfEmail, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1);
        grid.add(tfName, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(tfPhone, 1, 2);
        grid.add(new Label("ID Proof:"), 0, 3);
        grid.add(tfIdProof, 1, 3);
        grid.add(new Label("Gender:"), 0, 4);
        grid.add(cbGender, 1, 4);
        grid.add(new Label("Password:"), 0, 5);
        grid.add(pfPass, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/hotel/styles.css").toExternalForm());

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != createBtn) return;

        if (tfEmail.getText().isBlank() || tfName.getText().isBlank() || pfPass.getText().isBlank() || cbGender.getValue() == null) {
            showAlert("Validation", "Email, name, gender and password are required.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Guest g = new Guest();
            g.setName(tfName.getText().trim());
            g.setPhone(tfPhone.getText().trim());
            g.setEmail(tfEmail.getText().trim());
            g.setIdProof(tfIdProof.getText().trim());
            g.setGender(cbGender.getValue());
            int guestId = guestDAO.addGuest(g);

            appUserDAO.createUser(
                tfEmail.getText().trim(),
                tfName.getText().trim(),
                tfPhone.getText().trim(),
                guestId,
                "USER",
                pfPass.getText().toCharArray()
            );

            loadUsers();
            showAlert("Created", "User account created.", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
