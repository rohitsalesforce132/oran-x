package com.oranx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Input request for xApp orchestration.
 * Contains services to deploy, SLA requirements, and resource constraints.
 */
public class OrchestrationRequest {

    @JsonProperty("services")
    private List<Service> services;

    @JsonProperty("resourceBudget")
    private ResourceBudget resourceBudget;

    @JsonProperty("optimizationGoal")
    private OptimizationGoal optimizationGoal;

    @JsonProperty("sharedXAppsAllowed")
    private boolean sharedXAppsAllowed;

    @JsonProperty("qualityThreshold")
    private double qualityThreshold;

    @JsonProperty("latencyThreshold")
    private double latencyThreshold;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public OrchestrationRequest() {
        this.services = new ArrayList<>();
        this.sharedXAppsAllowed = true;
        this.qualityThreshold = 0.8;
        this.latencyThreshold = 10.0;
        this.optimizationGoal = OptimizationGoal.MAXIMIZE_SERVICES;
        this.metadata = new HashMap<>();
    }

    // Getters and Setters
    public List<Service> getServices() { return services; }
    public void setServices(List<Service> services) { this.services = services; }

    public ResourceBudget getResourceBudget() { return resourceBudget; }
    public void setResourceBudget(ResourceBudget resourceBudget) { this.resourceBudget = resourceBudget; }

    public OptimizationGoal getOptimizationGoal() { return optimizationGoal; }
    public void setOptimizationGoal(OptimizationGoal optimizationGoal) { this.optimizationGoal = optimizationGoal; }

    public boolean isSharedXAppsAllowed() { return sharedXAppsAllowed; }
    public void setSharedXAppsAllowed(boolean sharedXAppsAllowed) { this.sharedXAppsAllowed = sharedXAppsAllowed; }

    public double getQualityThreshold() { return qualityThreshold; }
    public void setQualityThreshold(double qualityThreshold) { this.qualityThreshold = qualityThreshold; }

    public double getLatencyThreshold() { return latencyThreshold; }
    public void setLatencyThreshold(double latencyThreshold) { this.latencyThreshold = latencyThreshold; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public void addService(Service service) {
        this.services.add(service);
    }

    @Override
    public String toString() {
        return String.format("OrchestrationRequest[id=%s, services=%d, budget=%s, goal=%s]",
                requestId, services.size(), resourceBudget, optimizationGoal);
    }
}
