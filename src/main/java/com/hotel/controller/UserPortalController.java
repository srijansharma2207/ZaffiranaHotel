package com.hotel.controller;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.RoomDAO;
import com.hotel.dao.ServiceDAO;
import com.hotel.model.Room;
import com.hotel.session.Session;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserPortalController {

    @FXML private Label userInfoLabel;

    @FXML private DatePicker dpCheckIn;
    @FXML private DatePicker dpCheckOut;
    @FXML private Spinner<Integer> spGuests;
    @FXML private CheckBox cbBreakfast;
    @FXML private ComboBox<Room> cbRoomType;
    @FXML private Label availabilityLabel;
    @FXML private Label finalPriceLabel;

    private final RoomDAO roomDAO = new RoomDAO();
    private final BookingDAO bookingDAO = new BookingDAO();

    @FXML
    public void initialize() {
        if (dpCheckIn != null) dpCheckIn.setValue(LocalDate.now());
        if (dpCheckOut != null) dpCheckOut.setValue(LocalDate.now().plusDays(1));

        if (spGuests != null) {
            spGuests.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
            spGuests.valueProperty().addListener((obs, oldV, newV) -> {
                refreshEligibleRoomTypes();
                updateFinalPriceAndAvailability();
            });
        }

        if (dpCheckIn != null) {
            dpCheckIn.valueProperty().addListener((obs, oldV, newV) -> {
                refreshEligibleRoomTypes();
                updateFinalPriceAndAvailability();
            });
        }
        if (dpCheckOut != null) {
            dpCheckOut.valueProperty().addListener((obs, oldV, newV) -> {
                refreshEligibleRoomTypes();
                updateFinalPriceAndAvailability();
            });
        }

        if (cbBreakfast != null) {
            cbBreakfast.selectedProperty().addListener((obs, oldV, newV) -> updateFinalPriceAndAvailability());
        }

        if (cbRoomType != null) {
            cbRoomType.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateFinalPriceAndAvailability());
            // Show room type + price in dropdown
            cbRoomType.setCellFactory(param -> new ListCell<Room>() {
                @Override protected void updateItem(Room item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? "" : item.getRoomType() + " (¥" + String.format("%.0f", item.getPrice()) + "/night)");
                }
            });
            cbRoomType.setButtonCell(new ListCell<Room>() {
                @Override protected void updateItem(Room item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? "" : item.getRoomType());
                }
            });
        }

        refresh();
    }

    @FXML
    public void refresh() {
        if (Session.getCurrentUser() != null) {
            userInfoLabel.setText("Logged in as: " + Session.getCurrentUser().getEmail());
        } else {
            userInfoLabel.setText("Not logged in");
        }

        refreshEligibleRoomTypes();
    }

    @FXML
    public void bookNow() {
        if (Session.getCurrentUser() == null) {
            showAlert("Login required", "Please login as a user to book a room.", Alert.AlertType.WARNING);
            return;
        }
        if (Session.getCurrentUser().getGuestId() == null) {
            showAlert("Profile incomplete", "This user account is missing a guest profile. Ask admin to recreate the user.", Alert.AlertType.WARNING);
            return;
        }

        LocalDate checkIn = dpCheckIn != null ? dpCheckIn.getValue() : null;
        LocalDate checkOut = dpCheckOut != null ? dpCheckOut.getValue() : null;
        Integer guests = spGuests != null ? spGuests.getValue() : null;
        boolean breakfast = cbBreakfast != null && cbBreakfast.isSelected();

        if (checkIn == null || checkOut == null) {
            showAlert("Validation", "Please select both check-in and check-out dates.", Alert.AlertType.WARNING);
            return;
        }
        if (!checkOut.isAfter(checkIn)) {
            showAlert("Validation", "Check-out date must be after check-in date.", Alert.AlertType.WARNING);
            return;
        }
        if (guests == null || guests < 1) {
            showAlert("Validation", "Please select number of guests.", Alert.AlertType.WARNING);
            return;
        }
        if (guests > 5) {
            showAlert("Room limit exceeded", "Room limit exceeded. Maximum allowed guests per room is 5.", Alert.AlertType.WARNING);
            return;
        }

        Room selectedRoom = cbRoomType != null ? cbRoomType.getValue() : null;
        String roomType = selectedRoom != null ? selectedRoom.getRoomType() : null;
        if (roomType == null || roomType.isBlank()) {
            showAlert("Validation", "Please select a room type.", Alert.AlertType.WARNING);
            return;
        }

        if (!isRoomTypeEligible(roomType, guests)) {
            showAlert("Not eligible", "Selected room type is not eligible for " + guests + " guest(s).", Alert.AlertType.WARNING);
            refreshEligibleRoomTypes();
            return;
        }

        try {
            Room allocated = roomDAO.getRandomAvailableRoomByType(roomType);
            if (allocated == null) {
                showAlert("No Rooms", "No rooms available for type: " + roomType, Alert.AlertType.WARNING);
                refreshEligibleRoomTypes();
                return;
            }

            int bookingId = bookingDAO.createBooking(
                allocated.getRoomId(),
                Session.getCurrentUser().getGuestId(),
                checkIn,
                checkOut,
                guests,
                breakfast
            );

            roomDAO.updateRoomStatus(allocated.getRoomId(), "OCCUPIED");
            showAlert("Booked",
                "Booking created! Booking #" + bookingId + "\nAllocated Room: " + allocated.getRoomNumber() + " (" + allocated.getRoomType() + ")",
                Alert.AlertType.INFORMATION);

            openServicesDialog(bookingId, checkIn, checkOut, guests);
            refresh();
        } catch (SQLException e) {
            showAlert("Booking failed", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void refreshEligibleRoomTypes() {
        if (availabilityLabel != null) availabilityLabel.setText("");

        Integer guests = (spGuests != null && spGuests.getValue() != null) ? spGuests.getValue() : 1;

        if (guests > 5) {
            if (cbRoomType != null) {
                cbRoomType.getItems().setAll();
                cbRoomType.setValue(null);
            }
            if (availabilityLabel != null) {
                availabilityLabel.setText("Room limit exceeded (max 5 guests). Please reduce guests.");
            }
            return;
        }

        try {
            List<Room> availableRooms = roomDAO.getAvailableRooms();
            List<Room> eligible = new ArrayList<>();
            for (Room r : availableRooms) {
                if (isRoomTypeEligible(r.getRoomType(), guests)) {
                    eligible.add(r);
                }
            }

            if (cbRoomType != null) {
                cbRoomType.getItems().setAll(eligible);
                if (!eligible.contains(cbRoomType.getValue())) {
                    cbRoomType.setValue(eligible.isEmpty() ? null : eligible.get(0));
                }
            }

            if (availabilityLabel != null && eligible.isEmpty()) {
                availabilityLabel.setText("No rooms available for the selected guests.");
            }
        } catch (SQLException e) {
            showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean isRoomTypeEligible(String roomType, int guests) {
        if (roomType == null) return false;
        String t = roomType.trim().toUpperCase();

        if (guests <= 1) {
            return true;
        }
        if (guests == 2) {
            return t.equals("DOUBLE") || t.equals("SUITE") || t.equals("DELUXE");
        }
        return t.equals("SUITE");
    }

    private void updateFinalPriceAndAvailability() {
        if (finalPriceLabel == null) return;

        LocalDate checkIn = dpCheckIn != null ? dpCheckIn.getValue() : null;
        LocalDate checkOut = dpCheckOut != null ? dpCheckOut.getValue() : null;
        Integer guests = spGuests != null ? spGuests.getValue() : 1;
        boolean breakfast = cbBreakfast != null && cbBreakfast.isSelected();
        Room selected = cbRoomType != null ? cbRoomType.getValue() : null;

        if (checkIn == null || checkOut == null || selected == null) {
            finalPriceLabel.setText("");
            return;
        }

        if (!checkOut.isAfter(checkIn)) {
            finalPriceLabel.setText("Invalid dates");
            return;
        }

        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 1) nights = 1;

        double roomTotal = nights * selected.getPrice();

        // Breakfast rules: free for DELUXE/SUITE, else charge per person per night if selected
        double breakfastTotal = 0;
        if (breakfast) {
            String rt = selected.getRoomType().trim().toUpperCase();
            boolean free = rt.contains("DELUXE") || rt.contains("SUITE");
            if (!free) {
                breakfastTotal = 200.0 * guests * nights;
            }
        }

        double finalTotal = roomTotal + breakfastTotal;
        boolean available = cbRoomType.getItems().contains(selected);

        String status = available ? "Available" : "Unavailable";
        String priceText = String.format("Final Price: %.2f | %s | %d night(s)", finalTotal, status, nights);
        if (!available) {
            priceText = "Room type not available for selected dates/guests";
        }
        finalPriceLabel.setText(priceText);
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void openServicesDialog(int bookingId, LocalDate checkIn, LocalDate checkOut, int maxPax) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Services");
        dialog.setHeaderText("Add extra services (optional) — charges will be added to final bill");

        ButtonType saveBtn = new ButtonType("Save Services", ButtonBar.ButtonData.OK_DONE);
        ButtonType skipBtn = new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, skipBtn, ButtonType.CANCEL);

        VBox root = new VBox(12);
        root.setPadding(new Insets(14));

        Label priceInfo = new Label(
            "Food/Room Service: ₹400 per order\n" +
            "Spa: ₹500 per person\n" +
            "Pool: ₹50 per person per day\n" +
            "Banquet Hall: ₹10000 flat\n" +
            "Breakfast: Free for Deluxe/Suite, else ₹200 per person per night (if selected)");
        priceInfo.getStyleClass().add("info-label");

        CheckBox cbBanquet = new CheckBox("Banquet Hall (₹10000)");

        Spinner<Integer> spFoodOrders = new Spinner<>(0, 20, 0);
        spFoodOrders.setPrefWidth(120);
        HBox foodRow = new HBox(10, new Label("Food/Room Service orders (₹400 each):"), spFoodOrders);

        Spinner<Integer> spSpaPax = new Spinner<>(0, Math.max(0, maxPax), 0);
        spSpaPax.setPrefWidth(120);
        HBox spaRow = new HBox(10, new Label("Spa pax (₹500 per person):"), spSpaPax);

        ListView<String> poolList = new ListView<>();
        poolList.setPrefHeight(110);

        DatePicker dpPoolDay = new DatePicker(checkIn);
        Spinner<Integer> spPoolPax = new Spinner<>(1, Math.max(1, maxPax), 1);
        spPoolPax.setPrefWidth(90);
        Button btnAddPoolDay = new Button("Add Pool Day");

        btnAddPoolDay.setOnAction(evt -> {
            LocalDate day = dpPoolDay.getValue();
            Integer pax = spPoolPax.getValue();
            if (day == null) return;
            if (checkIn != null && checkOut != null) {
                if (day.isBefore(checkIn) || !day.isBefore(checkOut)) {
                    showAlert("Validation", "Pool day must be within the stay period.", Alert.AlertType.WARNING);
                    return;
                }
            }
            poolList.getItems().add(day + " | pax: " + pax);
        });

        GridPane poolGrid = new GridPane();
        poolGrid.setHgap(10);
        poolGrid.setVgap(10);
        poolGrid.add(new Label("Pool day:"), 0, 0);
        poolGrid.add(dpPoolDay, 1, 0);
        poolGrid.add(new Label("Pax:"), 2, 0);
        poolGrid.add(spPoolPax, 3, 0);
        poolGrid.add(btnAddPoolDay, 4, 0);
        poolGrid.add(new Label("Added pool entries:"), 0, 1);
        poolGrid.add(poolList, 1, 1, 4, 1);

        root.getChildren().addAll(priceInfo, cbBanquet, foodRow, spaRow, new Separator(), poolGrid);
        dialog.getDialogPane().setContent(root);

        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/hotel/styles.css").toExternalForm());

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() == ButtonType.CANCEL) return;
        if (res.get() == skipBtn) return;

        try {
            ServiceDAO serviceDAO = new ServiceDAO();

            if (spFoodOrders.getValue() != null && spFoodOrders.getValue() > 0) {
                double price = serviceDAO.getUnitPrice("FOOD");
                serviceDAO.addServiceItem(bookingId, "FOOD", null, null, spFoodOrders.getValue(), price);
            }

            if (spSpaPax.getValue() != null && spSpaPax.getValue() > 0) {
                double price = serviceDAO.getUnitPrice("SPA");
                serviceDAO.addServiceItem(bookingId, "SPA", null, spSpaPax.getValue(), spSpaPax.getValue(), price);
            }

            if (cbBanquet.isSelected()) {
                double price = serviceDAO.getUnitPrice("BANQUET");
                serviceDAO.addServiceItem(bookingId, "BANQUET", null, null, 1, price);
            }

            if (!poolList.getItems().isEmpty()) {
                double price = serviceDAO.getUnitPrice("POOL");
                for (String entry : poolList.getItems()) {
                    // format: yyyy-mm-dd | pax: N
                    String[] parts = entry.split("\\|pax:");
                    if (parts.length != 2) continue;
                    LocalDate day = LocalDate.parse(parts[0].trim());
                    int pax = Integer.parseInt(parts[1].trim());
                    serviceDAO.addServiceItem(bookingId, "POOL", day, pax, pax, price);
                }
            }

            showAlert("Saved", "Services saved. They will be added to the final bill at checkout.", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Service Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}
