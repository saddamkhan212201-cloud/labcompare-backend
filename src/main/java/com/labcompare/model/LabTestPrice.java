package com.labcompare.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "lab_test_prices",
       uniqueConstraints = @UniqueConstraint(columnNames = {"lab_id", "test_id"}))
public class LabTestPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id", nullable = false)
    private Lab lab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @NotNull
    @Column(nullable = false)
    private Double price;

    @Column(name = "discount_percent")
    private Double discountPercent = 0.0;

    private String reportDuration;

    public LabTestPrice() {}

    public Double getEffectivePrice() {
        if (discountPercent == null || discountPercent == 0) return price;
        return Math.round(price * (1 - discountPercent / 100) * 100.0) / 100.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Lab getLab() { return lab; }
    public void setLab(Lab lab) { this.lab = lab; }
    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }
    public String getReportDuration() { return reportDuration; }
    public void setReportDuration(String reportDuration) { this.reportDuration = reportDuration; }
}
