package com.example.iotgarden.ui.home;

public class Reading {
    public Date date;
    public String name;
    public Soil soil;

    public Reading() {

    }

    public Reading(Date date, String name, Soil soil) {
        this.date = date;
        this.name = name;
        this.soil = soil;
    }
}
