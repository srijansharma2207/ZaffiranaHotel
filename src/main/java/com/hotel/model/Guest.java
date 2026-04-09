package com.hotel.model;

public class Guest {
    private int guestId;
    private String name;
    private String phone;
    private String email;
    private String idProof;
    private String gender;

    public Guest() {}

    public Guest(int guestId, String name, String phone, String email, String idProof, String gender) {
        this.guestId = guestId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.idProof = idProof;
        this.gender = gender;
    }

    public int getGuestId()     { return guestId; }
    public String getName()     { return name; }
    public String getPhone()    { return phone; }
    public String getEmail()    { return email; }
    public String getIdProof()  { return idProof; }
    public String getGender()   { return gender; }

    public void setGuestId(int guestId)     { this.guestId = guestId; }
    public void setName(String name)        { this.name = name; }
    public void setPhone(String phone)      { this.phone = phone; }
    public void setEmail(String email)      { this.email = email; }
    public void setIdProof(String idProof)  { this.idProof = idProof; }
    public void setGender(String gender)    { this.gender = gender; }

    @Override
    public String toString() { return name + " (" + phone + ")"; }
}