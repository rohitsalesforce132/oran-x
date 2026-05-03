package com.oranx.model;

/**
 * Represents a RAN function that can be implemented by different xApps.
 * These are the building blocks of service configurations.
 */
public enum RANFunction {
    /**
     * Traffic forecasting - predicts future traffic patterns
     */
    TRAFFIC_FORECASTER("traffic_forecaster"),

    /**
     * Traffic classification - categorizes traffic types
     */
    TRAFFIC_CLASSIFICATOR("traffic_classificator"),

    /**
     * Network slicing - allocates network resources for slices
     */
    NETWORK_SLICER("network_slicer");

    private final String name;

    RANFunction(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public static RANFunction fromString(String name) {
        for (RANFunction func : values()) {
            if (func.name.equalsIgnoreCase(name)) {
                return func;
            }
        }
        throw new IllegalArgumentException("Unknown RAN function: " + name);
    }
}
