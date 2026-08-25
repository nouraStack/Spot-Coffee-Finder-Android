package com.example.spot.models;

public class Booking {
    private String bookingId;
    private String userId;
    private String cafeId;
    private String cafeName;
    private String date;
    private String startTime;
    private String endTime;
    private int guests;
    private double totalPrice;
    private String status; // requested, awaiting_customer_decision, confirmed, rejected, cancelled, completed
    private long createdAt;
    private String notes;
    private String assignedTableId;
    private String assignedTableLabel;
    private String customerName;
    private String proposalDate;
    private String proposalStartTime;
    private String proposalEndTime;
    private String decisionDeadline;

    public Booking() {}

    public Booking(String bookingId, String userId, String cafeId, String cafeName,
                   String date, String startTime, String endTime, int guests,
                   double totalPrice, String status) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.cafeId = cafeId;
        this.cafeName = cafeName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.guests = guests;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
        this.notes = "";
        this.assignedTableId = "";
        this.assignedTableLabel = "";
        this.customerName = "";
        this.proposalDate = "";
        this.proposalStartTime = "";
        this.proposalEndTime = "";
        this.decisionDeadline = "";
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCafeId() { return cafeId; }
    public void setCafeId(String cafeId) { this.cafeId = cafeId; }
    public String getCafeName() { return cafeName; }
    public void setCafeName(String cafeName) { this.cafeName = cafeName; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public int getGuests() { return guests; }
    public void setGuests(int guests) { this.guests = guests; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getAssignedTableId() { return assignedTableId; }
    public void setAssignedTableId(String assignedTableId) { this.assignedTableId = assignedTableId; }
    public String getAssignedTableLabel() { return assignedTableLabel; }
    public void setAssignedTableLabel(String assignedTableLabel) { this.assignedTableLabel = assignedTableLabel; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getProposalDate() { return proposalDate; }
    public void setProposalDate(String proposalDate) { this.proposalDate = proposalDate; }
    public String getProposalStartTime() { return proposalStartTime; }
    public void setProposalStartTime(String proposalStartTime) { this.proposalStartTime = proposalStartTime; }
    public String getProposalEndTime() { return proposalEndTime; }
    public void setProposalEndTime(String proposalEndTime) { this.proposalEndTime = proposalEndTime; }
    public String getDecisionDeadline() { return decisionDeadline; }
    public void setDecisionDeadline(String decisionDeadline) { this.decisionDeadline = decisionDeadline; }
}
