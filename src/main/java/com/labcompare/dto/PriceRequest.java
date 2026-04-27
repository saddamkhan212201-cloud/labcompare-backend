package com.labcompare.dto;

import jakarta.validation.constraints.*;

public class PriceRequest {
    @NotNull public Long labId;
    @NotNull public Long testId;
    @NotNull @Min(1) public Double price;
    @Min(0) @Max(100) public Double discountPercent;
    public String reportDuration;

    public Long getLabId() { return labId; } public void setLabId(Long l) { this.labId = l; }
    public Long getTestId() { return testId; } public void setTestId(Long t) { this.testId = t; }
    public Double getPrice() { return price; } public void setPrice(Double p) { this.price = p; }
    public Double getDiscountPercent() { return discountPercent; } public void setDiscountPercent(Double d) { this.discountPercent = d; }
    public String getReportDuration() { return reportDuration; } public void setReportDuration(String r) { this.reportDuration = r; }
}
