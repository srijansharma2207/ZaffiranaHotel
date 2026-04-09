package com.hotel.controller;

import com.hotel.dao.RoomDAO;
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
import java.util.List;
import java.util.Optional;

public class RoomController {

    @FXML private TableView<Room> roomTable;
    @FXML private TableColumn<Room, Integer> colRoomId;
    @FXML private TableColumn<Room, String>  colRoomNumber;
    @FXML private TableColumn<Room, String>  colRoomType;
    @FXML private TableColumn<Room, Double>  colPrice;
    @FXML private TableColumn<Room, String>  colStatus;
    @FXML private Label roomCountLabel;
    @FXML private Label availableLabel;

    private final RoomDAO roomDAO = new RoomDAO();

    @FXML
    public void initialize() {
        colRoomId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getRoomId()).asObject());
        colRoomNumber.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoomNumber()));
        colRoomType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoomType()));
        colPrice.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPrice()).asObject());
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));

        // Color code status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.equals("AVAILABLE")
                    ? "-fx-text-fill: #2e7d52; -fx-font-weight: bold;"
                    : "-fx-text-fill: #c0392b; -fx-font-weight: bold;");
            }
        });

        loadRooms();
    }

    @FXML
    public void loadRooms() {
        try {
            List<Room> rooms = roomDAO.getAllRooms();
            roomTable.setItems(FXCollections.observableArrayList(rooms));
            roomCountLabel.setText("Total Rooms: " + rooms.size());
            long available = rooms.stream().filter(r -> r.getStatus().equals("AVAILABLE")).count();
            availableLabel.setText("Available: " + available);
        } catch (SQLException e) {
            showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void openAddRoomDialog() {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Add New Room");
        dialog.setHeaderText("Enter Room Details");

        ButtonType addBtn = new ButtonType("Add Room", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField tfId     = styledField("e.g. 106");
        TextField tfNum    = styledField("e.g. 106");
        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("SINGLE","DOUBLE","SUITE","DELUXE");
        cbType.setValue("SINGLE");
        cbType.getStyleClass().add("dialog-form-field");
        TextField tfPrice  = styledField("e.g. 2000");

        grid.add(label("Room ID:"),    0, 0); grid.add(tfId,    1, 0);
        grid.add(label("Room No:"),    0, 1); grid.add(tfNum,   1, 1);
        grid.add(label("Type:"),       0, 2); grid.add(cbType,  1, 2);
        grid.add(label("Price/Night:"),0, 3); grid.add(tfPrice, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/hotel/styles.css").toExternalForm());

        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                try {
                    Room r = new Room();
                    r.setRoomId(Integer.parseInt(tfId.getText().trim()));
                    r.setRoomNumber(tfNum.getText().trim());
                    r.setRoomType(cbType.getValue());
                    r.setPrice(Double.parseDouble(tfPrice.getText().trim()));
                    r.setStatus("AVAILABLE");
                    return r;
                } catch (NumberFormatException e) {
                    showAlert("Input Error", "Please enter valid numeric values.", Alert.AlertType.ERROR);
                }
            }
            return null;
        });

        Optional<Room> result = dialog.showAndWait();
        result.ifPresent(room -> {
            try {
                roomDAO.addRoom(room);
                loadRooms();
                showAlert("Success", "Room " + room.getRoomNumber() + " added!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    public void deleteSelectedRoom() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a room to delete.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Room " + selected.getRoomNumber() + "?");
        confirm.setContentText("This will permanently delete room ID " + selected.getRoomId() + ". This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                roomDAO.deleteRoom(selected.getRoomId());
                loadRooms();
                showAlert("Deleted", "Room " + selected.getRoomNumber() + " has been deleted.", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR);
            }
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