package com.example.iotgarden.stemma;

/**
 * Class representing a reading build from the Date class. Includes the name of the reading as
 * well as the soil information from the Soil class
 */
public class Reading {
    public Date date;
    public String name;
    public Soil soil;

    // Empty Constructor
    public Reading() {

    }

    /**
     * Reading including date, name, and soil information.
     * @param date Date unique date of the soil reading
     * @param name String that represents the name of the reading
     * @param soil Soil object that contains all the soil information
     */
    public Reading(Date date, String name, Soil soil) {
        this.date = date;
        this.name = name;
        this.soil = soil;
    }
}
