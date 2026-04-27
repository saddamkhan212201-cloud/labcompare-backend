package com.labcompare.dto;

import com.labcompare.model.Booking;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class BookingRequest {
    @NotBlank public String patientName;
    @Min(1) @Max(120) public Integer patientAge;
    @NotBlank public String phone;
    public String email;
    @NotNull public Long labId;
    @NotNull public Long testId;
    @NotNull public Booking.CollectionType collectionType;
    public String collectionAddress;
    @NotNull public LocalDate appointmentDate;
    @NotBlank public String appointmentSlot;
    @NotNull public Booking.PaymentMethod paymentMethod;

    public String getPatientName() { return patientName; } public void setPatientName(String p) { this.patientName = p; }
    public Integer getPatientAge() { return patientAge; } public void setPatientAge(Integer p) { this.patientAge = p; }
    public String getPhone() { return phone; } public void setPhone(String p) { this.phone = p; }
    public String getEmail() { return email; } public void setEmail(String e) { this.email = e; }
    public Long getLabId() { return labId; } public void setLabId(Long l) { this.labId = l; }
    public Long getTestId() { return testId; } public void setTestId(Long t) { this.testId = t; }
    public Booking.CollectionType getCollectionType() { return collectionType; } public void setCollectionType(Booking.CollectionType c) { this.collectionType = c; }
    public String getCollectionAddress() { return collectionAddress; } public void setCollectionAddress(String c) { this.collectionAddress = c; }
    public LocalDate getAppointmentDate() { return appointmentDate; } public void setAppointmentDate(LocalDate a) { this.appointmentDate = a; }
    public String getAppointmentSlot() { return appointmentSlot; } public void setAppointmentSlot(String a) { this.appointmentSlot = a; }
    public Booking.PaymentMethod getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(Booking.PaymentMethod p) { this.paymentMethod = p; }
}
