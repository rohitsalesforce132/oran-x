package com.oranx.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resource budget constraints for the orchestration.
 * Defines available CPU, memory, and disk resources.
 */
public class ResourceBudget {

    @JsonProperty("cpuCores")
    private double cpuCores;

    @JsonProperty("memoryGB")
    private double memoryGB;

    @JsonProperty("diskGB")
    private double diskGB;

    public ResourceBudget() {
    }

    public ResourceBudget(double cpuCores, double memoryGB, double diskGB) {
        this.cpuCores = cpuCores;
        this.memoryGB = memoryGB;
        this.diskGB = diskGB;
    }

    // Getters and Setters
    public double getCpuCores() { return cpuCores; }
    public void setCpuCores(double cpuCores) { this.cpuCores = cpuCores; }

    public double getMemoryGB() { return memoryGB; }
    public void setMemoryGB(double memoryGB) { this.memoryGB = memoryGB; }

    public double getDiskGB() { return diskGB; }
    public void setDiskGB(double diskGB) { this.diskGB = diskGB; }

    public boolean canFit(double cpu, double memory, double disk) {
        return cpuCores >= cpu && memoryGB >= memory && diskGB >= disk;
    }

    public ResourceBudget subtract(double cpu, double memory, double disk) {
        return new ResourceBudget(
            cpuCores - cpu,
            memoryGB - memory,
            diskGB - disk
        );
    }

    @Override
    public String toString() {
        return String.format("ResourceBudget[cpu=%.2f, mem=%.2fGB, disk=%.2fGB]",
                cpuCores, memoryGB, diskGB);
    }
}
