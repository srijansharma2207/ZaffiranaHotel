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

            // Guest fields
            TextField tfName    = styledField("Full name");
            TextField tfPhone   = styledField("Phone number");
            TextField tfEmail   = styledField("Email address");
            TextField tfIdProof = styledField("Aadhar/Passport No.");
            ComboBox<String> cbGender = new ComboBox<>(FXCollections.observableArrayList("Male", "Female", "Other"));
            cbGender.getStyleClass().add("dialog-form-field");
            cbGender.setPromptText("Select gender");

            // Room selection
            ComboBox<Room> cbRoom = new ComboBox<>(FXCollections.observableArrayList(availableRooms));
            cbRoom.getStyleClass().add("dialog-form-field");
            cbRoom.setPromptText("Select a room");

            // Check-in date
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

                Guest guest = new Guest();
                guest.setName(tfName.getText().trim());
                guest.setPhone(tfPhone.getText().trim());
                guest.setEmail(tfEmail.getText().trim());
                guest.setIdProof(tfIdProof.getText().trim());
                guest.setGender(cbGender.getValue());

                if (guest.getGender() == null || guest.getGender().isBlank()) {
                    showAlert("Validation", "Gender is required.", Alert.AlertType.WARNING);
                    return;
                }

                int guestId = guestDAO.addGuest(guest);
                Room selectedRoom = cbRoom.getValue();
                int bookingId = bookingDAO.createBooking(
                    selectedRoom.getRoomId(), guestId, dpCheckIn.getValue());
                roomDAO.updateRoomStatus(selectedRoom.getRoomId(), "OCCUPIED");

                loadBookings();
                showAlert("Booked!", "Booking #" + bookingId + " created for " + guest.getName(), Alert.AlertType.INFORMATION);
            }

        } catch (SQLException e) {
            showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void checkoutSelected() {
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Select Booking", "Please select a booking to checkout.", Alert.AlertType.WARNING);
            return;
        }

        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Checkout");
        dialog.setHeaderText("Checkout: " + selected.getGuestName() + " | Room " + selected.getRoomNumber());

        ButtonType checkoutBtn = new ButtonType("Checkout", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(checkoutBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(12);
        grid.setPadding(new Insets(20));
        DatePicker dpCheckOut = new DatePicker(LocalDate.now());
        grid.add(label("Check-Out Date:"), 0, 0);
        grid.add(dpCheckOut, 1, 0);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/hotel/styles.css").toExternalForm());

        dialog.setResultConverter(btn -> btn == checkoutBtn ? dpCheckOut.getValue() : null);

        Optional<LocalDate> result = dialog.showAndWait();
        result.ifPresent(checkOut -> {
            try {
                double total = bookingDAO.checkoutBooking(selected.getBookingId(), checkOut);
                loadBookings();
                showAlert("Checkout Complete",
                    "Guest checked out.\nTotal Bill: ₹" + String.format("%.2f", total),
                    Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
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