package com.hotel.controller;

import com.hotel.dao.AppUserDAO;
import com.hotel.db.DBConnection;
import com.hotel.model.AppUser;
import com.hotel.session.Session;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import javafx.util.Duration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private Label statusLabel;
    @FXML private TabPane mainTabPane;

    @FXML private Button logoutBtn;
    @FXML private Button navArrowBtn;
    @FXML private Button hamburgerBtn;
    @FXML private ScrollPane preLoginPane;
    @FXML private Button bookAStayBtn;
    @FXML private Button exploreResortBtn;

    @FXML private Tab homeTab;
    @FXML private Tab userTab;
    @FXML private Tab roomsTab;
    @FXML private Tab bookingsTab;
    @FXML private Tab billingTab;
    @FXML private Tab usersTab;

    @FXML private Pane drawerOverlay;
    @FXML private VBox drawerPane;

    @FXML private Button drawerHomeBtn;
    @FXML private Button drawerUserPortalBtn;
    @FXML private Button drawerRoomsBtn;
    @FXML private Button drawerBookingsBtn;
    @FXML private Button drawerBillingBtn;
    @FXML private Button drawerUsersBtn;

    private List<Tab> allTabs;

    private boolean drawerOpen = false;

    @FXML
    public void initialize() {

        if (rootPane != null) {
            rootPane.setOpacity(0);
            rootPane.setTranslateY(10);

            FadeTransition fade = new FadeTransition(Duration.millis(350), rootPane);
            fade.setFromValue(0);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(350), rootPane);
            slide.setFromY(10);
            slide.setToY(0);

            new ParallelTransition(fade, slide).play();
        }

        if (drawerPane != null) {
            drawerPane.setTranslateX(-drawerPane.getPrefWidth());
            drawerPane.setVisible(false);
            drawerPane.setManaged(false);
        }

        if (drawerOverlay != null && rootPane != null) {
            drawerOverlay.prefWidthProperty().bind(rootPane.widthProperty());
            drawerOverlay.prefHeightProperty().bind(rootPane.heightProperty());
        }

        try {
            DBConnection.getConnection();
            statusLabel.setText("● Connected to Oracle DB  |  XEPDB1@localhost:1521");
            statusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 12px;");
        } catch (SQLException e) {
            statusLabel.setText("● DB Connection Failed: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        }

        if (mainTabPane != null) {
            allTabs = new ArrayList<>(mainTabPane.getTabs());
        } else {
            allTabs = Arrays.asList(homeTab, userTab, roomsTab, bookingsTab, billingTab, usersTab);
        }

        applyRoleRestrictions();
    }

    @FXML
    public void openLoginChooser() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Login");
        dialog.setHeaderText("Choose account type");

        ButtonType adminBtn = new ButtonType("Admin Login", ButtonBar.ButtonData.LEFT);
        ButtonType userBtn = new ButtonType("User Login", ButtonBar.ButtonData.RIGHT);
        dialog.getDialogPane().getButtonTypes().addAll(adminBtn, userBtn, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/hotel/styles.css").toExternalForm());

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty()) return;
        if (res.get() == adminBtn) {
            openAdminLogin();
        } else if (res.get() == userBtn) {
            openUserLogin();
        }
    }

    private void applyRoleRestrictions() {
        boolean loggedIn = Session.isLoggedIn();
        boolean isAdmin = Session.isAdmin();

        if (preLoginPane != null) {
            preLoginPane.setVisible(!loggedIn);
            preLoginPane.setManaged(!loggedIn);
        }

        if (mainTabPane != null) {
            mainTabPane.setVisible(loggedIn);
            mainTabPane.setManaged(loggedIn);
        }

        if (hamburgerBtn != null) {
            hamburgerBtn.setVisible(loggedIn);
            hamburgerBtn.setManaged(loggedIn);
        }

        if (logoutBtn != null) {
            logoutBtn.setVisible(loggedIn);
            logoutBtn.setManaged(loggedIn);
        }

        if (navArrowBtn != null) {
            boolean onLanding = preLoginPane != null && preLoginPane.isVisible();
            navArrowBtn.setVisible(loggedIn);
            navArrowBtn.setManaged(loggedIn);
            if (loggedIn) {
                navArrowBtn.setText(onLanding ? "\u2192" : "\u2190");
            }
        }

        if (mainTabPane != null && allTabs != null) {
            List<Tab> visibleTabs = new ArrayList<>();
            if (loggedIn) {
                if (isAdmin) {
                    visibleTabs.add(homeTab);
                    visibleTabs.add(roomsTab);
                    visibleTabs.add(bookingsTab);
                    visibleTabs.add(billingTab);
                    visibleTabs.add(usersTab);
                } else {
                    visibleTabs.add(userTab);
                }
            }

            mainTabPane.getTabs().setAll(visibleTabs);

            for (Tab t : allTabs) {
                if (t != null) t.setDisable(false);
            }
        }

        if (homeTab != null) homeTab.setDisable(!loggedIn);

        if (drawerHomeBtn != null) {
            boolean show = loggedIn && isAdmin;
            drawerHomeBtn.setVisible(show);
            drawerHomeBtn.setManaged(show);
        }
        if (drawerUserPortalBtn != null) {
            boolean show = loggedIn && !isAdmin;
            drawerUserPortalBtn.setVisible(show);
            drawerUserPortalBtn.setManaged(show);
        }
        if (drawerRoomsBtn != null) {
            boolean show = loggedIn && isAdmin;
            drawerRoomsBtn.setVisible(show);
            drawerRoomsBtn.setManaged(show);
        }
        if (drawerBookingsBtn != null) {
            boolean show = loggedIn && isAdmin;
            drawerBookingsBtn.setVisible(show);
            drawerBookingsBtn.setManaged(show);
        }
        if (drawerBillingBtn != null) {
            boolean show = loggedIn && isAdmin;
            drawerBillingBtn.setVisible(show);
            drawerBillingBtn.setManaged(show);
        }
        if (drawerUsersBtn != null) {
            boolean show = loggedIn && isAdmin;
            drawerUsersBtn.setVisible(show);
            drawerUsersBtn.setManaged(show);
        }

        if (!loggedIn) {
            hideDrawer();
        }

        if (loggedIn) {
            selectTab(isAdmin ? homeTab : userTab);
        }
    }

    @FXML
    public void openAdminLogin() {
        openLogin("ADMIN");
    }

    @FXML
    public void openUserLogin() {
        openLogin("USER");
    }

    private void openLogin(String expectedRole) {
        try {
            AppUserDAO dao = new AppUserDAO();

            if ("ADMIN".equalsIgnoreCase(expectedRole) && !dao.hasAnyAdmin()) {
                boolean created = openCreateAdminDialog(dao);
                if (!created) return;
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Login");
            dialog.setHeaderText(expectedRole.equalsIgnoreCase("ADMIN") ? "Admin Login" : "User Login");

            ButtonType loginBtn = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(loginBtn, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(10);
            grid.setPadding(new Insets(18));

            TextField tfEmail = new TextField();
            tfEmail.setPromptText("Email");
            PasswordField pfPass = new PasswordField();
            pfPass.setPromptText("Password");

            grid.add(new Label("Email:"), 0, 0);
            grid.add(tfEmail, 1, 0);
            grid.add(new Label("Password:"), 0, 1);
            grid.add(pfPass, 1, 1);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/hotel/styles.css").toExternalForm());

            Optional<ButtonType> res = dialog.showAndWait();
            if (res.isEmpty() || res.get() != loginBtn) return;

            if (tfEmail.getText().isBlank() || pfPass.getText().isBlank()) {
                showAlert("Validation", "Email and password are required.", Alert.AlertType.WARNING);
                return;
            }

            AppUser user = dao.authenticate(tfEmail.getText().trim(), pfPass.getText().toCharArray());
            if (user == null) {
                showAlert("Login failed", "Invalid email or password.", Alert.AlertType.ERROR);
                return;
            }

            if (!expectedRole.equalsIgnoreCase(user.getRole())) {
                showAlert("Access denied", "This account is not a " + expectedRole + " account.", Alert.AlertType.ERROR);
                return;
            }

            Session.setCurrentUser(user);
            applyRoleRestrictions();

        } catch (SQLException e) {
            showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean openCreateAdminDialog(AppUserDAO dao) throws SQLException {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Admin");
        dialog.setHeaderText("No admin exists yet. Create the first ADMIN account.");

        ButtonType createBtn = new ButtonType("Create Admin", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));

        TextField tfEmail = new TextField();
        tfEmail.setPromptText("admin@example.com");
        TextField tfName = new TextField();
        tfName.setPromptText("Full name");
        PasswordField pfPass = new PasswordField();
        pfPass.setPromptText("Password");

        grid.add(new Label("Email:"), 0, 0);
        grid.add(tfEmail, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1);
        grid.add(tfName, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(pfPass, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/hotel/styles.css").toExternalForm());

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != createBtn) return false;

        if (tfEmail.getText().isBlank() || tfName.getText().isBlank() || pfPass.getText().isBlank()) {
            showAlert("Validation", "Email, name and password are required.", Alert.AlertType.WARNING);
            return false;
        }

        dao.createUser(tfEmail.getText().trim(), tfName.getText().trim(), null, null, "ADMIN", pfPass.getText().toCharArray());
        showAlert("Admin created", "Admin account created. You can login now.", Alert.AlertType.INFORMATION);
        return true;
    }

    @FXML
    public void toggleDrawer() {
        if (drawerOpen) {
            hideDrawer();
        } else {
            showDrawer();
        }
    }

    private void showDrawer() {
        if (drawerPane == null || drawerOverlay == null) return;
        drawerOpen = true;
        drawerOverlay.setVisible(true);
        drawerOverlay.setManaged(true);
        drawerPane.setVisible(true);
        drawerPane.setManaged(true);

        FadeTransition overlayFade = new FadeTransition(Duration.millis(160), drawerOverlay);
        overlayFade.setFromValue(0);
        overlayFade.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(220), drawerPane);
        slideIn.setFromX(-drawerPane.getPrefWidth());
        slideIn.setToX(0);

        new ParallelTransition(overlayFade, slideIn).play();
    }

    @FXML
    public void hideDrawer() {
        if (drawerPane == null || drawerOverlay == null) return;
        drawerOpen = false;

        FadeTransition overlayFade = new FadeTransition(Duration.millis(160), drawerOverlay);
        overlayFade.setFromValue(drawerOverlay.getOpacity());
        overlayFade.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(220), drawerPane);
        slideOut.setFromX(drawerPane.getTranslateX());
        slideOut.setToX(-drawerPane.getPrefWidth());

        ParallelTransition pt = new ParallelTransition(overlayFade, slideOut);
        pt.setOnFinished(e -> {
            drawerOverlay.setVisible(false);
            drawerOverlay.setManaged(false);
            drawerPane.setVisible(false);
            drawerPane.setManaged(false);
        });
        pt.play();
    }

    @FXML
    public void goHome() {
        selectTab(homeTab);
    }

    @FXML
    public void goUserPortal() {
        selectTab(userTab);
    }

    @FXML
    public void goRooms() {
        selectTab(roomsTab);
    }

    @FXML
    public void goBookings() {
        selectTab(bookingsTab);
    }

    @FXML
    public void goBilling() {
        selectTab(billingTab);
    }

    @FXML
    public void goUsers() {
        selectTab(usersTab);
    }

    private void selectTab(Tab tab) {
        if (mainTabPane != null && tab != null && mainTabPane.getTabs().contains(tab)) {
            mainTabPane.getSelectionModel().select(tab);
        }
        hideDrawer();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML
    public void logout() {
        Session.clear();
        applyRoleRestrictions();
    }

    @FXML
    public void navArrowAction() {
        boolean onLanding = preLoginPane != null && preLoginPane.isVisible();
        if (onLanding) {
            // On landing: go back to app (admin/user)
            if (preLoginPane != null) {
                preLoginPane.setVisible(false);
                preLoginPane.setManaged(false);
            }
            if (mainTabPane != null) {
                mainTabPane.setVisible(true);
                mainTabPane.setManaged(true);
            }
            if (hamburgerBtn != null) {
                hamburgerBtn.setVisible(true);
                hamburgerBtn.setManaged(true);
            }
            if (logoutBtn != null) {
                logoutBtn.setVisible(true);
                logoutBtn.setManaged(true);
            }
            navArrowBtn.setText("\u2190");
            // Re-select the correct tab
            boolean isAdmin = Session.isAdmin();
            selectTab(isAdmin ? homeTab : userTab);
        } else {
            // Inside app: go back to landing page without logging out
            if (preLoginPane != null) {
                preLoginPane.setVisible(true);
                preLoginPane.setManaged(true);
            }
            if (mainTabPane != null) {
                mainTabPane.setVisible(false);
                mainTabPane.setManaged(false);
            }
            if (hamburgerBtn != null) {
                hamburgerBtn.setVisible(false);
                hamburgerBtn.setManaged(false);
            }
            if (logoutBtn != null) {
                logoutBtn.setVisible(false);
                logoutBtn.setManaged(false);
            }
            navArrowBtn.setText("\u2192");
            hideDrawer();
        }
    }

    @FXML
    public void bookAStay() {
        openUserLogin();
    }

    @FXML
    public void exploreResort() {
        showAlert("Coming Soon!", "Explore the Resort feature is coming soon.", Alert.AlertType.INFORMATION);
    }
}