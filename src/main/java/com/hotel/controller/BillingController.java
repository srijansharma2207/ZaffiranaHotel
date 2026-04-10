package com.hotel.controller;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.ServiceDAO;
import com.hotel.model.Booking;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    @FXML private VBox vbAdditionalCosts;

    private final BookingDAO bookingDAO = new BookingDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();

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

        // ✅ For ACTIVE bookings show "—" in total column, for CHECKED_OUT show the real amount
        colBlAmount.setCellValueFactory(d -> {
            Booking b = d.getValue();
            if ("ACTIVE".equals(b.getStatus())) {
                return new SimpleDoubleProperty(0).asObject();
            }
            return new SimpleDoubleProperty(b.getTotalAmount()).asObject();
        });

        colBlAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                Booking b = getTableView().getItems().get(getIndex());
                if ("ACTIVE".equals(b.getStatus())) {
                    setText("—");
                    setStyle("-fx-text-fill: #888;");
                } else {
                    setText("₹" + String.format("%.2f", item));
                    setStyle("-fx-text-fill: #1a3a5c; -fx-font-weight: bold;");
                }
            }
        });

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
            showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void clearBillingHistory() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Billing History");
        confirm.setHeaderText("Delete All Billing Records");
        confirm.setContentText(
            "Are you sure you want to delete all billing history?\n\n" +
            "This will permanently delete:\n" +
            "- All booking records\n" +
            "- All service items\n" +
            "- All billing data\n\n" +
            "This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                bookingDAO.clearAllBillingData();
                loadBilling();
                lblBkId.setText("");
                lblBkGuest.setText("");
                lblBkRoom.setText("");
                lblBkIn.setText("");
                lblBkOut.setText("");
                lblNights.setText("");
                lblRate.setText("");
                lblTotal.setText("");
                vbAdditionalCosts.getChildren().clear();
                showAlert("Cleared", "All billing history has been deleted.", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("DB Error", "Failed to clear billing history: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showBillSummary(Booking b) {
        lblBkId.setText(String.valueOf(b.getBookingId()));
        lblBkGuest.setText(b.getGuestName());
        lblBkRoom.setText(b.getRoomNumber() + " (" + b.getRoomType() + ")");
        lblBkIn.setText(b.getCheckIn() != null ? b.getCheckIn().toString() : "—");
        lblBkOut.setText(b.getCheckOut() != null ? b.getCheckOut().toString() : "Not checked out yet");

        vbAdditionalCosts.getChildren().clear();

        if (b.getCheckIn() != null && b.getCheckOut() != null) {
            // ✅ CHECKED OUT — show real bill from DB
            long nights = ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut());
            if (nights < 1) nights = 1;
            lblNights.setText(nights + " night(s)");
            lblRate.setText("₹" + String.format("%.2f", b.getPricePerNight()));
            lblTotal.setText("₹" + String.format("%.2f", b.getTotalAmount()));

            try {
                Map<String, Double> breakdown = serviceDAO.getBookingServiceBreakdown(b.getBookingId());
                if (breakdown != null && !breakdown.isEmpty()) {
                    for (Map.Entry<String, Double> e : breakdown.entrySet()) {
                        Label costLabel = new Label(String.format("  • %s:  ₹%.2f", e.getKey(), e.getValue()));
                        costLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 13px;");
                        vbAdditionalCosts.getChildren().add(costLabel);
                    }
                } else {
                    Label noCosts = new Label("No additional services");
                    noCosts.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-font-size: 13px;");
                    vbAdditionalCosts.getChildren().add(noCosts);
                }
            } catch (SQLException ex) {
                Label error = new Label("Unable to load service details: " + ex.getMessage());
                error.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12px;");
                vbAdditionalCosts.getChildren().add(error);
            }

        } else {
            // ✅ STILL ACTIVE — compute estimated bill live
            long nightsSoFar = ChronoUnit.DAYS.between(b.getCheckIn(), java.time.LocalDate.now());
            if (nightsSoFar < 1) nightsSoFar = 1;
            lblNights.setText(nightsSoFar + " night(s) so far");
            lblRate.setText("₹" + String.format("%.2f", b.getPricePerNight()));

            double estimated = nightsSoFar * b.getPricePerNight();
            lblTotal.setText("~₹" + String.format("%.2f", estimated) + " (estimated)");

            try {
                Map<String, Double> breakdown = serviceDAO.getBookingServiceBreakdown(b.getBookingId());
                if (breakdown != null && !breakdown.isEmpty()) {
                    Label heading = new Label("Services added so far:");
                    heading.setStyle("-fx-text-fill: #555; -fx-font-size: 13px; -fx-font-weight: bold;");
                    vbAdditionalCosts.getChildren().add(heading);
                    for (Map.Entry<String, Double> e : breakdown.entrySet()) {
                        Label costLabel = new Label(String.format("  • %s:  ₹%.2f", e.getKey(), e.getValue()));
                        costLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 13px;");
                        vbAdditionalCosts.getChildren().add(costLabel);
                    }
                } else {
                    Label note = new Label("No additional services added yet");
                    note.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-font-size: 13px;");
                    vbAdditionalCosts.getChildren().add(note);
                }
            } catch (SQLException ex) {
                Label error = new Label("Unable to load service details: " + ex.getMessage());
                error.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12px;");
                vbAdditionalCosts.getChildren().add(error);
            }
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