package com.example.iotgarden.ui.home;

/**
 * Class representing a single soil reading. Contains the moisture and temperature info.
 */
public class Soil {
    public String moisture;
    public String temperature;

    // Empty Constructor
    public Soil() {

    }

    /**
     * Soil reading with moisture and temperature levels
     * @param moisture String of moisture
     * @param temperature String of temperature
     */
    public Soil(String moisture, String temperature) {
        this.moisture = moisture;
        this.temperature = temperature;
    }
}
