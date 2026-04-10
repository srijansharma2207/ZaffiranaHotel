package com.hotel.controller;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.GuestDAO;
import com.hotel.dao.RoomDAO;
import com.hotel.model.Booking;
import com.hotel.model.Guest;
import com.hotel.model.Room;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BookingController {

    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, Integer> colBkId;
    @FXML private TableColumn<Booking, String>  colBkGuest;
    @FXML private TableColumn<Booking, String>  colBkRoom;
    @FXML private TableColumn<Booking, String>  colBkType;
    @FXML private TableColumn<Booking, String>  colBkCheckIn;
    @FXML private TableColumn<Booking, String>  colBkStatus;
    @FXML private TableColumn<Booking, Double>  colBkRate;

    private final BookingDAO bookingDAO = new BookingDAO();
    private final RoomDAO    roomDAO    = new RoomDAO();
    private final GuestDAO   guestDAO   = new GuestDAO();

    @FXML
    public void initialize() {
        colBkId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getBookingId()).asObject());
        colBkGuest.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getGuestName()));
        colBkRoom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoomNumber()));
        colBkType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoomType()));
        colBkCheckIn.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCheckIn() != null ? d.getValue().getCheckIn().toString() : ""));
        colBkStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colBkRate.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPricePerNight()).asObject());

        colBkStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.equals("ACTIVE")
                    ? "-fx-text-fill: #2e7d52; -fx-font-weight: bold;"
                    : "-fx-text-fill: #888;");
            }
        });

        loadBookings();
    }

    @FXML
    public void loadBookings() {
        try {
            List<Booking> bookings = bookingDAO.getActiveBookings();
            bookingTable.setItems(FXCollections.observableArrayList(bookings));
        } catch (SQLException e) {
            showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void openBookingDialog() {
        try {
            List<Room> availableRooms = roomDAO.getAvailableRooms();
            if (availableRooms.isEmpty()) {
                showAlert("No Rooms", "No available rooms at the moment.", Alert.AlertType.WARNING);
                return;
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("New Booking");
            dialog.setHeaderText("Fill in guest and room details");

            ButtonType bookBtn = new ButtonType("Book Now", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(bookBtn, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(15); grid.setVgap(12);
            grid.setPadding(new Insets(20));

            TextField tfName    = styledField("Full name");
            TextField tfPhone   = styledField("Phone number");
            TextField tfEmail   = styledField("Email address");
            TextField tfIdProof = styledField("Aadhar/Passport No.");

            ComboBox<String> cbGender = new ComboBox<>(FXCollections.observableArrayList("Male", "Female", "Other"));
            cbGender.getStyleClass().add("dialog-form-field");
            cbGender.setPromptText("Select gender");

            ComboBox<Room> cbRoom = new ComboBox<>(FXCollections.observableArrayList(availableRooms));
            cbRoom.getStyleClass().add("dialog-form-field");
            cbRoom.setPromptText("Select a room");

            DatePicker dpCheckIn = new DatePicker(LocalDate.now());

            grid.add(label("Guest Name:"),  0, 0); grid.add(tfName,    1, 0);
            grid.add(label("Phone:"),       0, 1); grid.add(tfPhone,   1, 1);
            grid.add(label("Email:"),       0, 2); grid.add(tfEmail,   1, 2);
            grid.add(label("ID Proof:"),    0, 3); grid.add(tfIdProof, 1, 3);
            grid.add(label("Gender:"),      0, 4); grid.add(cbGender,  1, 4);
            grid.add(label("Room:"),        0, 5); grid.add(cbRoom,    1, 5);
            grid.add(label("Check-In:"),    0, 6); grid.add(dpCheckIn, 1, 6);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/hotel/styles.css").toExternalForm());

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == bookBtn) {
                if (tfName.getText().isBlank() || cbRoom.getValue() == null) {
                    showAlert("Validation", "Name and Room are required.", Alert.AlertType.WARNING);
                    return;
                }
                if (cbGender.getValue() == null || cbGender.getValue().isBlank()) {
                    showAlert("Validation", "Gender is required.", Alert.AlertType.WARNING);
                    return;
                }

                Guest guest = new Guest();
                guest.setName(tfName.getText().trim());
                guest.setPhone(tfPhone.getText().trim());
                guest.setEmail(tfEmail.getText().trim());
                guest.setIdProof(tfIdProof.getText().trim());
                guest.setGender(cbGender.getValue());

                int guestId = guestDAO.addGuest(guest);
                Room selectedRoom = cbRoom.getValue();
                int bookingId = bookingDAO.createBooking(
                    selectedRoom.getRoomId(), guestId, dpCheckIn.getValue());
                roomDAO.updateRoomStatus(selectedRoom.getRoomId(), "OCCUPIED");

                loadBookings();
                showAlert("Booked!", "Booking #" + bookingId + " created for " + guest.getName(),
                    Alert.AlertType.INFORMATION);
            }

        } catch (SQLException e) {
            showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void checkoutSelected() {
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Select Booking", "Please click on a booking row first, then click Checkout.",
                Alert.AlertType.WARNING);
            return;
        }

        LocalDate checkIn = selected.getCheckIn();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Checkout Guest");
        dialog.setHeaderText("Checking out: " + selected.getGuestName() + " | Room " + selected.getRoomNumber());

        ButtonType checkoutBtn = new ButtonType("Confirm Checkout", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(checkoutBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        // Opens calendar at check-in date — admin picks any date on or after it
        DatePicker dpCheckout = new DatePicker(checkIn);

        Label infoLabel = new Label("Select any date on or after check-in: " + checkIn);
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic; -fx-font-size: 11px;");

        grid.add(label("Checkout Date:"), 0, 0);
        grid.add(dpCheckout, 1, 0);
        grid.add(infoLabel, 0, 1, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/hotel/styles.css").toExternalForm());

        while (true) {
            Optional<ButtonType> result = dialog.showAndWait();

            // Admin cancelled
            if (result.isEmpty() || result.get() != checkoutBtn) return;

            LocalDate checkoutDate = dpCheckout.getValue();

            if (checkoutDate == null) {
                showAlert("No Date Selected", "Please pick a checkout date from the calendar.",
                    Alert.AlertType.WARNING);
                continue;
            }

            // Only rule: checkout cannot be before check-in
            if (checkoutDate.isBefore(checkIn)) {
                showAlert("Invalid Date",
                    "Checkout date cannot be before check-in date (" + checkIn + ").\nPlease select a valid date.",
                    Alert.AlertType.WARNING);
                continue;
            }

            // All good — process checkout
            try {
                double total = bookingDAO.checkoutBooking(selected.getBookingId(), checkoutDate);
                loadBookings(); // removed from Bookings tab, now appears in Billing tab
                showAlert("Checkout Complete",
                    "Guest:      " + selected.getGuestName() + "\n" +
                    "Room:       " + selected.getRoomNumber() + "\n" +
                    "Check-in:   " + checkIn + "\n" +
                    "Check-out:  " + checkoutDate + "\n\n" +
                    "Total Bill: \u20b9" + String.format("%.2f", total),
                    Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
            }
            return;
        }
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("dialog-form-field");
        return tf;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("dialog-form-label");
        return l;
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}