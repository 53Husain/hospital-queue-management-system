package com.shqms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "hospitals")
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;
    private String city;
    private String contactNumber;

    public Hospital() {
    }

    public Hospital(Long id,
                    String name,
                    String address,
                    String city,
                    String contactNumber) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
        this.contactNumber = contactNumber;
    }

    // Generate Getters and Setters using IntelliJ
}