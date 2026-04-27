package com.labcompare.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String bookingRef;

    @NotBlank
    private String patientName;

    private Integer patientAge;
    private String phone;
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id", nullable = false)
    private Lab lab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    private Double testPrice;
    private Double collectionFee;
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    private CollectionType collectionType;

    private String collectionAddress;
    private LocalDate appointmentDate;
    private String appointmentSlot;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Booking() {}

    public enum CollectionType { HOME, LAB }
    public enum PaymentMethod { UPI, CARD, CASH }
    public enum BookingStatus { CONFIRMED, CANCELLED, COMPLETED }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Integer getPatientAge() { return patientAge; }
    public void setPatientAge(Integer patientAge) { this.patientAge = patientAge; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Lab getLab() { return lab; }
    public void setLab(Lab lab) { this.lab = lab; }
    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }
    public Double getTestPrice() { return testPrice; }
    public void setTestPrice(Double testPrice) { this.testPrice = testPrice; }
    public Double getCollectionFee() { return collectionFee; }
    public void setCollectionFee(Double collectionFee) { this.collectionFee = collectionFee; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public CollectionType getCollectionType() { return collectionType; }
    public void setCollectionType(CollectionType collectionType) { this.collectionType = collectionType; }
    public String getCollectionAddress() { return collectionAddress; }
    public void setCollectionAddress(String collectionAddress) { this.collectionAddress = collectionAddress; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public String getAppointmentSlot() { return appointmentSlot; }
    public void setAppointmentSlot(String appointmentSlot) { this.appointmentSlot = appointmentSlot; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
