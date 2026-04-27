package com.labcompare.dto;

public class LabDTO {
    private Long id;
    private String name;
    private String city;
    private String address;
    private String phone;
    private Double rating;
    private String accreditation;
    private Boolean homeCollection;

    public LabDTO() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getCity() { return city; } public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; } public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public Double getRating() { return rating; } public void setRating(Double rating) { this.rating = rating; }
    public String getAccreditation() { return accreditation; } public void setAccreditation(String a) { this.accreditation = a; }
    public Boolean getHomeCollection() { return homeCollection; } public void setHomeCollection(Boolean h) { this.homeCollection = h; }
}
