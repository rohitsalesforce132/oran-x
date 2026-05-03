package com.oranx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Represents an xApp implementation for a specific RAN function.
 * Multiple xApps can implement the same function with different quality/resource profiles.
 */
public class XApp {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("function")
    private RANFunction function;

    @JsonProperty("cpuCores")
    private double cpuCores;

    @JsonProperty("memoryGB")
    private double memoryGB;

    @JsonProperty("diskGB")
    private double diskGB;

    @JsonProperty("qualityScore")
    private double qualityScore;

    @JsonProperty("theta")
    private double theta;

    @JsonProperty("version")
    private String version;

    @JsonProperty("description")
    private String description;

    public XApp() {
    }

    public XApp(String id, String name, RANFunction function, 
                double cpuCores, double memoryGB, double diskGB,
                double qualityScore, double theta) {
        this.id = id;
        this.name = name;
        this.function = function;
        this.cpuCores = cpuCores;
        this.memoryGB = memoryGB;
        this.diskGB = diskGB;
        this.qualityScore = qualityScore;
        this.theta = theta;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RANFunction getFunction() { return function; }
    public void setFunction(RANFunction function) { this.function = function; }

    public double getCpuCores() { return cpuCores; }
    public void setCpuCores(double cpuCores) { this.cpuCores = cpuCores; }

    public double getMemoryGB() { return memoryGB; }
    public void setMemoryGB(double memoryGB) { this.memoryGB = memoryGB; }

    public double getDiskGB() { return diskGB; }
    public void setDiskGB(double diskGB) { this.diskGB = diskGB; }

    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

    public double getTheta() { return theta; }
    public void setTheta(double theta) { this.theta = theta; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XApp xApp = (XApp) o;
        return Objects.equals(id, xApp.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("XApp[id=%s, name=%s, function=%s, Q=%.2f, theta=%.2f, cpu=%.2f, mem=%.2fGB]",
                id, name, function, qualityScore, theta, cpuCores, memoryGB);
    }
}
