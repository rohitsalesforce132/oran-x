package com.oranx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Output result from xApp orchestration.
 * Contains deployment plans, cost analysis, and feasibility status.
 */
public class OrchestrationResult {

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("status")
    private OrchestrationStatus status;

    @JsonProperty("deployedConfigurations")
    private List<ServiceConfiguration> deployedConfigurations;

    @JsonProperty("failedServices")
    private List<Service> failedServices;

    @JsonProperty("xAppDeployments")
    private List<XAppDeployment> xAppDeployments;

    @JsonProperty("totalServicesRequested")
    private int totalServicesRequested;

    @JsonProperty("totalServicesDeployed")
    private int totalServicesDeployed;

    @JsonProperty("totalCpuUsed")
    private double totalCpuUsed;

    @JsonProperty("totalMemoryUsedGB")
    private double totalMemoryUsedGB;

    @JsonProperty("totalDiskUsedGB")
    private double totalDiskUsedGB;

    @JsonProperty("averageQuality")
    private double averageQuality;

    @JsonProperty("averageLatencyMs")
    private double averageLatencyMs;

    @JsonProperty("optimizationScore")
    private double optimizationScore;

    @JsonProperty("feasibilityReport")
    private FeasibilityReport feasibilityReport;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("errors")
    private List<String> errors;

    public OrchestrationResult() {
        this.deployedConfigurations = new ArrayList<>();
        this.failedServices = new ArrayList<>();
        this.xAppDeployments = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.timestamp = LocalDateTime.now();
        this.status = OrchestrationStatus.IN_PROGRESS;
    }

    public OrchestrationResult(String requestId) {
        this();
        this.requestId = requestId;
    }

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public OrchestrationStatus getStatus() { return status; }
    public void setStatus(OrchestrationStatus status) { this.status = status; }

    public List<ServiceConfiguration> getDeployedConfigurations() { return deployedConfigurations; }
    public void setDeployedConfigurations(List<ServiceConfiguration> deployedConfigurations) { this.deployedConfigurations = deployedConfigurations; }

    public List<Service> getFailedServices() { return failedServices; }
    public void setFailedServices(List<Service> failedServices) { this.failedServices = failedServices; }

    public List<XAppDeployment> getXAppDeployments() { return xAppDeployments; }
    public void setXAppDeployments(List<XAppDeployment> xAppDeployments) { this.xAppDeployments = xAppDeployments; }

    public int getTotalServicesRequested() { return totalServicesRequested; }
    public void setTotalServicesRequested(int totalServicesRequested) { this.totalServicesRequested = totalServicesRequested; }

    public int getTotalServicesDeployed() { return totalServicesDeployed; }
    public void setTotalServicesDeployed(int totalServicesDeployed) { this.totalServicesDeployed = totalServicesDeployed; }

    public double getTotalCpuUsed() { return totalCpuUsed; }
    public void setTotalCpuUsed(double totalCpuUsed) { this.totalCpuUsed = totalCpuUsed; }

    public double getTotalMemoryUsedGB() { return totalMemoryUsedGB; }
    public void setTotalMemoryUsedGB(double totalMemoryUsedGB) { this.totalMemoryUsedGB = totalMemoryUsedGB; }

    public double getTotalDiskUsedGB() { return totalDiskUsedGB; }
    public void setTotalDiskUsedGB(double totalDiskUsedGB) { this.totalDiskUsedGB = totalDiskUsedGB; }

    public double getAverageQuality() { return averageQuality; }
    public void setAverageQuality(double averageQuality) { this.averageQuality = averageQuality; }

    public double getAverageLatencyMs() { return averageLatencyMs; }
    public void setAverageLatencyMs(double averageLatencyMs) { this.averageLatencyMs = averageLatencyMs; }

    public double getOptimizationScore() { return optimizationScore; }
    public void setOptimizationScore(double optimizationScore) { this.optimizationScore = optimizationScore; }

    public FeasibilityReport getFeasibilityReport() { return feasibilityReport; }
    public void setFeasibilityReport(FeasibilityReport feasibilityReport) { this.feasibilityReport = feasibilityReport; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public void addConfiguration(ServiceConfiguration config) {
        deployedConfigurations.add(config);
    }

    public void addFailedService(Service service) {
        failedServices.add(service);
    }

    public void addXAppDeployment(XAppDeployment deployment) {
        xAppDeployments.add(deployment);
    }

    public void addError(String error) {
        errors.add(error);
    }

    public double getDeploymentRate() {
        return totalServicesRequested > 0 ? 
            (double) totalServicesDeployed / totalServicesRequested : 0.0;
    }

    @Override
    public String toString() {
        return String.format("OrchestrationResult[id=%s, status=%s, deployed=%d/%d, Q=%.3f, L=%.2fms]",
                requestId, status, totalServicesDeployed, totalServicesRequested, 
                averageQuality, averageLatencyMs);
    }
}
