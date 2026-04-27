package com.labcompare.dto;

public class PriceDTO {
    private Long id;
    private Long labId;
    private String labName;
    private String labCity;
    private String labAccreditation;
    private Double labRating;
    private Long testId;
    private String testName;
    private String testCategory;
    private Double price;
    private Double discountPercent;
    private Double effectivePrice;
    private String reportDuration;

    public PriceDTO() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getLabId() { return labId; } public void setLabId(Long labId) { this.labId = labId; }
    public String getLabName() { return labName; } public void setLabName(String labName) { this.labName = labName; }
    public String getLabCity() { return labCity; } public void setLabCity(String labCity) { this.labCity = labCity; }
    public String getLabAccreditation() { return labAccreditation; } public void setLabAccreditation(String a) { this.labAccreditation = a; }
    public Double getLabRating() { return labRating; } public void setLabRating(Double labRating) { this.labRating = labRating; }
    public Long getTestId() { return testId; } public void setTestId(Long testId) { this.testId = testId; }
    public String getTestName() { return testName; } public void setTestName(String testName) { this.testName = testName; }
    public String getTestCategory() { return testCategory; } public void setTestCategory(String testCategory) { this.testCategory = testCategory; }
    public Double getPrice() { return price; } public void setPrice(Double price) { this.price = price; }
    public Double getDiscountPercent() { return discountPercent; } public void setDiscountPercent(Double d) { this.discountPercent = d; }
    public Double getEffectivePrice() { return effectivePrice; } public void setEffectivePrice(Double e) { this.effectivePrice = e; }
    public String getReportDuration() { return reportDuration; } public void setReportDuration(String r) { this.reportDuration = r; }
}
