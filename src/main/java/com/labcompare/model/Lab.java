package com.labcompare.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.List;

@Entity
@Table(name = "labs")
public class Lab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Lab name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "City is required")
    @Column(nullable = false)
    private String city;

    private String address;
    private String phone;

    @Column(name = "rating")
    private Double rating;

    private String accreditation;
    private Boolean homeCollection;

    @Column(name = "upi_id")
    private String upiId;

    @OneToMany(mappedBy = "lab", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LabTestPrice> prices;

    public Lab() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public String getAccreditation() { return accreditation; }
    public void setAccreditation(String accreditation) { this.accreditation = accreditation; }
    public Boolean getHomeCollection() { return homeCollection; }
    public void setHomeCollection(Boolean homeCollection) { this.homeCollection = homeCollection; }
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    public List<LabTestPrice> getPrices() { return prices; }
    public void setPrices(List<LabTestPrice> prices) { this.prices = prices; }
}
