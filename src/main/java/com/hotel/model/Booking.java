package com.hotel.model;

import java.time.LocalDate;

public class Booking {
    private int bookingId;
    private int roomId;
    private int guestId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String status;
    private double totalAmount;

    // Display helpers (joined from other tables)
    private String roomNumber;
    private String guestName;
    private String roomType;
    private double pricePerNight;

    public Booking() {}

    public int getBookingId()           { return bookingId; }
    public int getRoomId()              { return roomId; }
    public int getGuestId()             { return guestId; }
    public LocalDate getCheckIn()       { return checkIn; }
    public LocalDate getCheckOut()      { return checkOut; }
    public String getStatus()           { return status; }
    public double getTotalAmount()      { return totalAmount; }
    public String getRoomNumber()       { return roomNumber; }
    public String getGuestName()        { return guestName; }
    public String getRoomType()         { return roomType; }
    public double getPricePerNight()    { return pricePerNight; }

    public void setBookingId(int v)         { bookingId = v; }
    public void setRoomId(int v)            { roomId = v; }
    public void setGuestId(int v)           { guestId = v; }
    public void setCheckIn(LocalDate v)     { checkIn = v; }
    public void setCheckOut(LocalDate v)    { checkOut = v; }
    public void setStatus(String v)         { status = v; }
    public void setTotalAmount(double v)    { totalAmount = v; }
    public void setRoomNumber(String v)     { roomNumber = v; }
    public void setGuestName(String v)      { guestName = v; }
    public void setRoomType(String v)       { roomType = v; }
    public void setPricePerNight(double v)  { pricePerNight = v; }
}