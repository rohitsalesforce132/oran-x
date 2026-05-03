package com.oranx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a deployed xApp instance with its configuration and runtime parameters.
 */
public class XAppDeployment {

    @JsonProperty("deploymentId")
    private String deploymentId;

    @JsonProperty("xApp")
    private XApp xApp;

    @JsonProperty("serviceId")
    private String serviceId;

    @JsonProperty("function")
    private RANFunction function;

    @JsonProperty("nodeId")
    private String nodeId;

    @JsonProperty("status")
    private DeploymentStatus status;

    @JsonProperty("configParameters")
    private Map<String, Object> configParameters;

    @JsonProperty("deployedAt")
    private LocalDateTime deployedAt;

    @JsonProperty("cpuAllocated")
    private double cpuAllocated;

    @JsonProperty("memoryAllocatedGB")
    private double memoryAllocatedGB;

    @JsonProperty("diskAllocatedGB")
    private double diskAllocatedGB;

    public XAppDeployment() {
        this.configParameters = new HashMap<>();
        this.deployedAt = LocalDateTime.now();
        this.status = DeploymentStatus.PENDING;
    }

    public XAppDeployment(String deploymentId, XApp xApp, String serviceId, RANFunction function) {
        this();
        this.deploymentId = deploymentId;
        this.xApp = xApp;
        this.serviceId = serviceId;
        this.function = function;
        this.cpuAllocated = xApp.getCpuCores();
        this.memoryAllocatedGB = xApp.getMemoryGB();
        this.diskAllocatedGB = xApp.getDiskGB();
    }

    // Getters and Setters
    public String getDeploymentId() { return deploymentId; }
    public void setDeploymentId(String deploymentId) { this.deploymentId = deploymentId; }

    public XApp getXApp() { return xApp; }
    public void setXApp(XApp xApp) { this.xApp = xApp; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public RANFunction getFunction() { return function; }
    public void setFunction(RANFunction function) { this.function = function; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public DeploymentStatus getStatus() { return status; }
    public void setStatus(DeploymentStatus status) { this.status = status; }

    public Map<String, Object> getConfigParameters() { return configParameters; }
    public void setConfigParameters(Map<String, Object> configParameters) { this.configParameters = configParameters; }

    public LocalDateTime getDeployedAt() { return deployedAt; }
    public void setDeployedAt(LocalDateTime deployedAt) { this.deployedAt = deployedAt; }

    public double getCpuAllocated() { return cpuAllocated; }
    public void setCpuAllocated(double cpuAllocated) { this.cpuAllocated = cpuAllocated; }

    public double getMemoryAllocatedGB() { return memoryAllocatedGB; }
    public void setMemoryAllocatedGB(double memoryAllocatedGB) { this.memoryAllocatedGB = memoryAllocatedGB; }

    public double getDiskAllocatedGB() { return diskAllocatedGB; }
    public void setDiskAllocatedGB(double diskAllocatedGB) { this.diskAllocatedGB = diskAllocatedGB; }

    @Override
    public String toString() {
        return String.format("XAppDeployment[id=%s, xApp=%s, service=%s, node=%s, status=%s]",
                deploymentId, xApp != null ? xApp.getName() : "null", serviceId, nodeId, status);
    }
}
