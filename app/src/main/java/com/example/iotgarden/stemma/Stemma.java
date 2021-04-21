package com.example.iotgarden.stemma;

import com.example.iotgarden.ui.home.Date;
import com.example.iotgarden.ui.home.Reading;
import com.example.iotgarden.ui.home.Soil;
import com.example.iotgarden.ui.home.SoilReading;

import org.jetbrains.annotations.NotNull;

public class Stemma {

    public Reading reading;
    public Soil soil;
    public SoilReading soilReading;
    public Date date;

    public Stemma(Reading reading, Soil soil, SoilReading soilReading, Date date) {
        this.reading = reading;
        this.soil = soil;
        this.soilReading = soilReading;
        this.date = date;
    }

    public Reading getReading() {
        return reading;
    }

    public void setReading(Reading reading) {
        this.reading = reading;
    }

    public Soil getSoil() {
        return soil;
    }

    public void setSoil(Soil soil) {
        this.soil = soil;
    }

    public SoilReading getSoilReading() {
        return soilReading;
    }

    public void setSoilReading(SoilReading soilReading) {
        this.soilReading = soilReading;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @NotNull
    @Override
    public String toString() {
        return "Stemma{" +
                ", reading=" + reading +
                ", soil=" + soil +
                ", soilReading=" + soilReading +
                ", date=" + date +
                '}';
    }
}
