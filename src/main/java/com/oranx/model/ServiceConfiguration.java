package com.oranx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a specific configuration (DAG) of RAN functions that fulfills a service.
 * Each configuration requires specific functions and produces different quality/latency outcomes.
 */
public class ServiceConfiguration {

    @JsonProperty("serviceId")
    private String serviceId;

    @JsonProperty("configurationType")
    private ServiceConfigurationType configurationType;

    @JsonProperty("functions")
    private List<RANFunction> functions;

    @JsonProperty("xAppAssignments")
    private Map<RANFunction, XApp> xAppAssignments;

    @JsonProperty("totalQuality")
    private double totalQuality;

    @JsonProperty("totalLatencyMs")
    private double totalLatencyMs;

    @JsonProperty("totalCpuCores")
    private double totalCpuCores;

    @JsonProperty("totalMemoryGB")
    private double totalMemoryGB;

    @JsonProperty("totalDiskGB")
    private double totalDiskGB;

    public ServiceConfiguration() {
        this.functions = new ArrayList<>();
        this.xAppAssignments = new HashMap<>();
    }

    public ServiceConfiguration(String serviceId, ServiceConfigurationType configurationType) {
        this();
        this.serviceId = serviceId;
        this.configurationType = configurationType;
        this.functions = new ArrayList<>(configurationType.getRequiredFunctions());
    }

    // Getters and Setters
    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public ServiceConfigurationType getConfigurationType() { return configurationType; }
    public void setConfigurationType(ServiceConfigurationType configurationType) { this.configurationType = configurationType; }

    public List<RANFunction> getFunctions() { return functions; }
    public void setFunctions(List<RANFunction> functions) { this.functions = functions; }

    public Map<RANFunction, XApp> getXAppAssignments() { return xAppAssignments; }
    public void setXAppAssignments(Map<RANFunction, XApp> xAppAssignments) { this.xAppAssignments = xAppAssignments; }

    public double getTotalQuality() { return totalQuality; }
    public void setTotalQuality(double totalQuality) { this.totalQuality = totalQuality; }

    public double getTotalLatencyMs() { return totalLatencyMs; }
    public void setTotalLatencyMs(double totalLatencyMs) { this.totalLatencyMs = totalLatencyMs; }

    public double getTotalCpuCores() { return totalCpuCores; }
    public void setTotalCpuCores(double totalCpuCores) { this.totalCpuCores = totalCpuCores; }

    public double getTotalMemoryGB() { return totalMemoryGB; }
    public void setTotalMemoryGB(double totalMemoryGB) { this.totalMemoryGB = totalMemoryGB; }

    public double getTotalDiskGB() { return totalDiskGB; }
    public void setTotalDiskGB(double totalDiskGB) { this.totalDiskGB = totalDiskGB; }

    public void assignXApp(RANFunction function, XApp xApp) {
        xAppAssignments.put(function, xApp);
        recalculateTotals();
    }

    public void recalculateTotals() {
        totalQuality = 1.0;
        totalLatencyMs = 0.0;
        totalCpuCores = 0.0;
        totalMemoryGB = 0.0;
        totalDiskGB = 0.0;

        for (XApp xApp : xAppAssignments.values()) {
            // Quality combines multiplicatively (weakest link)
            totalQuality *= xApp.getQualityScore();
            // Latency adds up (theta models processing time)
            totalLatencyMs += xApp.getTheta();
            // Resources add up
            totalCpuCores += xApp.getCpuCores();
            totalMemoryGB += xApp.getMemoryGB();
            totalDiskGB += xApp.getDiskGB();
        }
    }

    public boolean isComplete() {
        return xAppAssignments.keySet().containsAll(functions);
    }

    public double getCost() {
        return totalCpuCores * 10 + totalMemoryGB * 5 + totalDiskGB * 1;
    }

    @Override
    public String toString() {
        return String.format("ServiceConfiguration[service=%s, type=%s, Q=%.3f, L=%.2fms, cost=%.2f]",
                serviceId, configurationType, totalQuality, totalLatencyMs, getCost());
    }
}
