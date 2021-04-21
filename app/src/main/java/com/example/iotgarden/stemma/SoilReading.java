package com.example.iotgarden.stemma;

import java.util.HashMap;

/**
 * Class representing a Hash Map with the unique date identifier and the soil reading
 */
public class SoilReading {
    public HashMap<String, Reading> stemma_1;

    // Empty Constructor
    public SoilReading() {

    }

    /**
     * The Hash Map with a Key of the unique date and the Value of the reading.
     * @param stemma_1 HashMap<String, Reading>
     */
    public SoilReading(HashMap<String, Reading> stemma_1) {
        this.stemma_1 = stemma_1;
    }
}
