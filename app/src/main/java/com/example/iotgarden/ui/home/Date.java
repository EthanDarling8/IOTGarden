package com.example.iotgarden.ui.home;


public class Date {
    public String year;
    public String month;
    public String day;
    public String hour;
    public String minute;

    // Empty Constructor
    public Date() {

    }

    /**
     * Full date acquired from the Soil Reading
     * @param year String
     * @param month String
     * @param day String
     * @param hour String
     * @param minute String
     */
    public Date(String year, String month, String day, String hour, String minute) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
    }

    /**
     * Partial date acquired from the Soil Reading
     * @param year String
     * @param month String
     * @param day String
     */
    public Date(String year, String month, String day) {
    }
}
