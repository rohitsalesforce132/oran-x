package com.oranx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feasibility analysis report for orchestration constraints.
 */
public class FeasibilityReport {

    @JsonProperty("feasible")
    private boolean feasible;

    @JsonProperty("constraintViolations")
    private List<ConstraintViolation> constraintViolations;

    @JsonProperty("resourceUtilization")
    private Map<String, Double> resourceUtilization;

    @JsonProperty("qualityCompliance")
    private boolean qualityCompliance;

    @JsonProperty("latencyCompliance")
    private boolean latencyCompliance;

    @JsonProperty("recommendations")
    private List<String> recommendations;

    public FeasibilityReport() {
        this.constraintViolations = new ArrayList<>();
        this.resourceUtilization = new HashMap<>();
        this.recommendations = new ArrayList<>();
        this.feasible = true;
        this.qualityCompliance = true;
        this.latencyCompliance = true;
    }

    // Getters and Setters
    public boolean isFeasible() { return feasible; }
    public void setFeasible(boolean feasible) { this.feasible = feasible; }

    public List<ConstraintViolation> getConstraintViolations() { return constraintViolations; }
    public void setConstraintViolations(List<ConstraintViolation> constraintViolations) { this.constraintViolations = constraintViolations; }

    public Map<String, Double> getResourceUtilization() { return resourceUtilization; }
    public void setResourceUtilization(Map<String, Double> resourceUtilization) { this.resourceUtilization = resourceUtilization; }

    public boolean isQualityCompliance() { return qualityCompliance; }
    public void setQualityCompliance(boolean qualityCompliance) { this.qualityCompliance = qualityCompliance; }

    public boolean isLatencyCompliance() { return latencyCompliance; }
    public void setLatencyCompliance(boolean latencyCompliance) { this.latencyCompliance = latencyCompliance; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    public void addViolation(ConstraintViolation violation) {
        constraintViolations.add(violation);
        feasible = false;
    }

    public void addRecommendation(String recommendation) {
        recommendations.add(recommendation);
    }

    @Override
    public String toString() {
        return String.format("FeasibilityReport[feasible=%s, violations=%d, quality=%s, latency=%s]",
                feasible, constraintViolations.size(), qualityCompliance, latencyCompliance);
    }

    public static class ConstraintViolation {
        @JsonProperty("type")
        private String type;

        @JsonProperty("description")
        private String description;

        @JsonProperty("severity")
        private ViolationSeverity severity;

        public ConstraintViolation() {}

        public ConstraintViolation(String type, String description, ViolationSeverity severity) {
            this.type = type;
            this.description = description;
            this.severity = severity;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public ViolationSeverity getSeverity() { return severity; }
        public void setSeverity(ViolationSeverity severity) { this.severity = severity; }

        @Override
        public String toString() {
            return String.format("ConstraintViolation[type=%s, severity=%s, desc=%s]",
                    type, severity, description);
        }
    }

    public enum ViolationSeverity {
        CRITICAL, HIGH, MEDIUM, LOW
    }
}
