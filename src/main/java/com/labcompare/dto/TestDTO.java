package com.labcompare.dto;

public class TestDTO {
    private Long id;
    private String name;
    private String category;
    private String description;

    public TestDTO() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getCategory() { return category; } public void setCategory(String c) { this.category = c; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
}
