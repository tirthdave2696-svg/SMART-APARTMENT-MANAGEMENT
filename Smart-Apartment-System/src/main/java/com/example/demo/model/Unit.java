package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "units")
public class Unit {
    @Id
    private String unitNumber;

    public Unit() {}
    public Unit(String unitNumber) { this.unitNumber = unitNumber; }
    public String getUnitNumber() { return unitNumber; }
    public void setUnitNumber(String v) { unitNumber = v; }
}