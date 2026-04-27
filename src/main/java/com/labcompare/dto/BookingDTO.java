package com.labcompare.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookingDTO {
    private Long id;
    private String bookingRef;
    private String patientName;
    private Integer patientAge;
    private String phone;
    private String email;
    private String labName;
    private String testName;
    private Double testPrice;
    private Double collectionFee;
    private Double totalAmount;
    private String collectionType;
    private String collectionAddress;
    private LocalDate appointmentDate;
    private String appointmentSlot;
    private String paymentMethod;
    private String status;
    private LocalDateTime createdAt;

    public BookingDTO() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getBookingRef() { return bookingRef; } public void setBookingRef(String b) { this.bookingRef = b; }
    public String getPatientName() { return patientName; } public void setPatientName(String p) { this.patientName = p; }
    public Integer getPatientAge() { return patientAge; } public void setPatientAge(Integer p) { this.patientAge = p; }
    public String getPhone() { return phone; } public void setPhone(String p) { this.phone = p; }
    public String getEmail() { return email; } public void setEmail(String e) { this.email = e; }
    public String getLabName() { return labName; } public void setLabName(String l) { this.labName = l; }
    public String getTestName() { return testName; } public void setTestName(String t) { this.testName = t; }
    public Double getTestPrice() { return testPrice; } public void setTestPrice(Double t) { this.testPrice = t; }
    public Double getCollectionFee() { return collectionFee; } public void setCollectionFee(Double c) { this.collectionFee = c; }
    public Double getTotalAmount() { return totalAmount; } public void setTotalAmount(Double t) { this.totalAmount = t; }
    public String getCollectionType() { return collectionType; } public void setCollectionType(String c) { this.collectionType = c; }
    public String getCollectionAddress() { return collectionAddress; } public void setCollectionAddress(String c) { this.collectionAddress = c; }
    public LocalDate getAppointmentDate() { return appointmentDate; } public void setAppointmentDate(LocalDate a) { this.appointmentDate = a; }
    public String getAppointmentSlot() { return appointmentSlot; } public void setAppointmentSlot(String a) { this.appointmentSlot = a; }
    public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String p) { this.paymentMethod = p; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
}
