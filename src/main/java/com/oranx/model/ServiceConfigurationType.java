package com.oranx.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Defines the different DAG configurations for fulfilling a service.
 * Each configuration represents a different path through RAN functions.
 */
public enum ServiceConfigurationType {
    /**
     * Full pipeline: forecaster → classificator → slicer
     */
    FULL_PIPELINE("forecaster+classificator+slicer", 
                  Arrays.asList(RANFunction.TRAFFIC_FORECASTER, 
                               RANFunction.TRAFFIC_CLASSIFICATOR, 
                               RANFunction.NETWORK_SLICER)),

    /**
     * Forecaster only: direct traffic prediction
     */
    FORECASTER_ONLY("forecaster", 
                    Arrays.asList(RANFunction.TRAFFIC_FORECASTER)),

    /**
     * Classificator only: direct classification
     */
    CLASSIFICATOR_ONLY("classificator", 
                       Arrays.asList(RANFunction.TRAFFIC_CLASSIFICATOR)),

    /**
     * Slicer only: direct network slicing
     */
    SLICER_ONLY("slicer", 
                Arrays.asList(RANFunction.NETWORK_SLICER)),

    /**
     * Forecaster + Slicer: predict then slice
     */
    FORECASTER_SLICER("forecaster+slicer", 
                      Arrays.asList(RANFunction.TRAFFIC_FORECASTER, 
                                   RANFunction.NETWORK_SLICER)),

    /**
     * Classificator + Slicer: classify then slice
     */
    CLASSIFICATOR_SLICER("classificator+slicer", 
                         Arrays.asList(RANFunction.TRAFFIC_CLASSIFICATOR, 
                                      RANFunction.NETWORK_SLICER));

    private final String name;
    private final List<RANFunction> requiredFunctions;

    ServiceConfigurationType(String name, List<RANFunction> requiredFunctions) {
        this.name = name;
        this.requiredFunctions = requiredFunctions;
    }

    public String getName() { return name; }
    public List<RANFunction> getRequiredFunctions() { return requiredFunctions; }

    public static ServiceConfigurationType fromString(String name) {
        for (ServiceConfigurationType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown configuration type: " + name);
    }

    public static List<String> getAllNames() {
        return Arrays.stream(values()).map(ServiceConfigurationType::getName).collect(Collectors.toList());
    }
}
