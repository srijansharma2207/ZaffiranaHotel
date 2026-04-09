package com.hotel.controller;

import com.hotel.dao.BookingDAO;
import com.hotel.model.Booking;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BillingController {

    @FXML private TableView<Booking> billingTable;
    @FXML private TableColumn<Booking, Integer> colBlId;
    @FXML private TableColumn<Booking, String>  colBlGuest;
    @FXML private TableColumn<Booking, String>  colBlRoom;
    @FXML private TableColumn<Booking, String>  colBlIn;
    @FXML private TableColumn<Booking, String>  colBlOut;
    @FXML private TableColumn<Booking, String>  colBlStatus;
    @FXML private TableColumn<Booking, Double>  colBlAmount;

    @FXML private Label lblBkId, lblBkGuest, lblBkRoom;
    @FXML private Label lblBkIn, lblBkOut, lblNights, lblRate, lblTotal;

    private final BookingDAO bookingDAO = new BookingDAO();

    @FXML
    public void initialize() {
        colBlId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getBookingId()).asObject());
        colBlGuest.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getGuestName()));
        colBlRoom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoomNumber()));
        colBlIn.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCheckIn() != null ? d.getValue().getCheckIn().toString() : ""));
        colBlOut.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCheckOut() != null ? d.getValue().getCheckOut().toString() : "—"));
        colBlStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colBlAmount.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getTotalAmount()).asObject());

        colBlStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.equals("ACTIVE")
                    ? "-fx-text-fill: #2e7d52; -fx-font-weight: bold;"
                    : "-fx-text-fill: #1a3a5c; -fx-font-weight: bold;");
            }
        });

        billingTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> { if (selected != null) showBillSummary(selected); });

        loadBilling();
    }

    @FXML
    public void loadBilling() {
        try {
            List<Booking> all = bookingDAO.getAllBookings();
            billingTable.setItems(FXCollections.observableArrayList(all));
        } catch (SQLException e) {
            showAlert("DB Error", e.getMessage());
        }
    }

    private void showBillSummary(Booking b) {
        lblBkId.setText(String.valueOf(b.getBookingId()));
        lblBkGuest.setText(b.getGuestName());
        lblBkRoom.setText(b.getRoomNumber() + " (" + b.getRoomType() + ")");
        lblBkIn.setText(b.getCheckIn() != null ? b.getCheckIn().toString() : "—");
        lblBkOut.setText(b.getCheckOut() != null ? b.getCheckOut().toString() : "Not checked out");

        if (b.getCheckIn() != null && b.getCheckOut() != null) {
            long nights = ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut());
            if (nights < 1) nights = 1;
            lblNights.setText(nights + " night(s)");
            lblRate.setText("₹" + String.format("%.2f", b.getPricePerNight()));
            lblTotal.setText("₹" + String.format("%.2f", b.getTotalAmount()));
        } else {
            lblNights.setText("—");
            lblRate.setText("₹" + String.format("%.2f", b.getPricePerNight()));
            lblTotal.setText("Still Active");
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}