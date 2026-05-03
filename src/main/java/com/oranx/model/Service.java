package com.oranx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Represents a telecom service that needs to be fulfilled by a configuration of RAN functions.
 * Services have SLA requirements including quality targets, latency constraints, and resource budgets.
 */
public class Service {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("priority")
    private double priority;

    @JsonProperty("frequency")
    private double frequency;

    @JsonProperty("latencyTargetMs")
    private double latencyTargetMs;

    @JsonProperty("qualityTarget")
    private double qualityTarget;

    @JsonProperty("configurationType")
    private ServiceConfigurationType configurationType;

    @JsonProperty("slaParameters")
    private Map<String, Object> slaParameters;

    public Service() {
    }

    public Service(String id, String name, double priority, double frequency, 
                   double latencyTargetMs, double qualityTarget, 
                   ServiceConfigurationType configurationType) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.frequency = frequency;
        this.latencyTargetMs = latencyTargetMs;
        this.qualityTarget = qualityTarget;
        this.configurationType = configurationType;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPriority() { return priority; }
    public void setPriority(double priority) { this.priority = priority; }

    public double getFrequency() { return frequency; }
    public void setFrequency(double frequency) { this.frequency = frequency; }

    public double getLatencyTargetMs() { return latencyTargetMs; }
    public void setLatencyTargetMs(double latencyTargetMs) { this.latencyTargetMs = latencyTargetMs; }

    public double getQualityTarget() { return qualityTarget; }
    public void setQualityTarget(double qualityTarget) { this.qualityTarget = qualityTarget; }

    public ServiceConfigurationType getConfigurationType() { return configurationType; }
    public void setConfigurationType(ServiceConfigurationType configurationType) { this.configurationType = configurationType; }

    public Map<String, Object> getSlaParameters() { return slaParameters; }
    public void setSlaParameters(Map<String, Object> slaParameters) { this.slaParameters = slaParameters; }

    public double getWeightedValue() {
        return priority * frequency;
    }

    @Override
    public String toString() {
        return String.format("Service[id=%s, name=%s, priority=%.2f, frequency=%.2f, Q=%.2f, L=%.2fms]",
                id, name, priority, frequency, qualityTarget, latencyTargetMs);
    }
}
