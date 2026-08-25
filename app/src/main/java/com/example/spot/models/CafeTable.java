package com.example.spot.models;

public class CafeTable {
    private String tableId;
    private String cafeId;
    private int tableNumber;
    private String zone;        // e.g. "Window", "Quiet Zone", "Near Outlet"
    private int seats;
    private String status;      // "available", "occupied", "reserved"
    private String assignedBookingId;

    public CafeTable() {}

    public CafeTable(String tableId, String cafeId, int tableNumber, String zone, int seats) {
        this.tableId = tableId;
        this.cafeId = cafeId;
        this.tableNumber = tableNumber;
        this.zone = zone;
        this.seats = seats;
        this.status = "available";
        this.assignedBookingId = "";
    }

    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }
    public String getCafeId() { return cafeId; }
    public void setCafeId(String cafeId) { this.cafeId = cafeId; }
    public int getTableNumber() { return tableNumber; }
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAssignedBookingId() { return assignedBookingId; }
    public void setAssignedBookingId(String assignedBookingId) { this.assignedBookingId = assignedBookingId; }
}

