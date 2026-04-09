package com.hotel.model;

public class Room {
    private int roomId;
    private String roomNumber;
    private String roomType;
    private double price;
    private String status;

    public Room() {}

    public Room(int roomId, String roomNumber, String roomType, double price, String status) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.price = price;
        this.status = status;
    }

    public int getRoomId()         { return roomId; }
    public String getRoomNumber()  { return roomNumber; }
    public String getRoomType()    { return roomType; }
    public double getPrice()       { return price; }
    public String getStatus()      { return status; }

    public void setRoomId(int roomId)           { this.roomId = roomId; }
    public void setRoomNumber(String roomNumber){ this.roomNumber = roomNumber; }
    public void setRoomType(String roomType)    { this.roomType = roomType; }
    public void setPrice(double price)          { this.price = price; }
    public void setStatus(String status)        { this.status = status; }

    @Override
    public String toString() { return roomNumber + " (" + roomType + ")"; }
}