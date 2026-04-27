package com.labcompare.dto;

import jakarta.validation.constraints.*;

public class TestRequest {
    @NotBlank public String name;
    @NotBlank public String category;
    public String description;

    public String getName() { return name; } public void setName(String n) { this.name = n; }
    public String getCategory() { return category; } public void setCategory(String c) { this.category = c; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
}
