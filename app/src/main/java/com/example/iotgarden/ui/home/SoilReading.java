package com.example.iotgarden.ui.home;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class SoilReading {
    public HashMap<String, Reading> stemma_1;

    public SoilReading() {
        // Empty Constructor
    }

    public SoilReading(HashMap<String, Reading> stemma_1) {
        this.stemma_1 = stemma_1;
    }
}
