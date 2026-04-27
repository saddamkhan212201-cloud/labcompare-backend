package com.labcompare.dto;

import jakarta.validation.constraints.*;

public class LabRequest {
    @NotBlank(message = "Name is required") public String name;
    @NotBlank(message = "City is required") public String city;
    public String address;
    public String phone;
    @DecimalMin("1.0") @DecimalMax("5.0") public Double rating;
    public String accreditation;
    public Boolean homeCollection;

    public String getName() { return name; } public void setName(String n) { this.name = n; }
    public String getCity() { return city; } public void setCity(String c) { this.city = c; }
    public String getAddress() { return address; } public void setAddress(String a) { this.address = a; }
    public String getPhone() { return phone; } public void setPhone(String p) { this.phone = p; }
    public Double getRating() { return rating; } public void setRating(Double r) { this.rating = r; }
    public String getAccreditation() { return accreditation; } public void setAccreditation(String a) { this.accreditation = a; }
    public Boolean getHomeCollection() { return homeCollection; } public void setHomeCollection(Boolean h) { this.homeCollection = h; }
}
