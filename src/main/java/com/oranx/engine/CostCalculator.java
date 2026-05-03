package com.oranx.engine;

import com.oranx.model.*;

/**
 * Calculates costs and optimization scores for orchestration results.
 */
public class CostCalculator {

    /**
     * Calculates the optimization score based on the specified goal.
     */
    public double calculateScore(OrchestrationResult result, OptimizationGoal goal) {
        switch (goal) {
            case MAXIMIZE_SERVICES:
                return calculateServiceScore(result);
            case MINIMIZE_RESOURCES:
                return calculateResourceScore(result);
            case MAXIMIZE_QUALITY:
                return calculateQualityScore(result);
            case MINIMIZE_LATENCY:
                return calculateLatencyScore(result);
            case BALANCED:
                return calculateBalancedScore(result);
            default:
                return calculateBalancedScore(result);
        }
    }

    /**
     * Score based on number of services deployed (weighted by priority).
     */
    private double calculateServiceScore(OrchestrationResult result) {
        double score = 0;
        for (ServiceConfiguration config : result.getDeployedConfigurations()) {
            // Base score for deployment
            score += 100;
            // Bonus for high quality
            score += config.getTotalQuality() * 50;
        }
        // Penalty for failed services
        score -= result.getFailedServices().size() * 50;
        return score;
    }

    /**
     * Score based on resource efficiency (lower resource usage = higher score).
     */
    private double calculateResourceScore(OrchestrationResult result) {
        double totalCost = result.getTotalCpuUsed() * 10 +
                          result.getTotalMemoryUsedGB() * 5 +
                          result.getTotalDiskUsedGB() * 1;

        // Invert cost so lower usage = higher score
        double score = 1000 - totalCost;

        // Bonus for each deployed service
        score += result.getTotalServicesDeployed() * 20;

        return score;
    }

    /**
     * Score based on quality across all services.
     */
    private double calculateQualityScore(OrchestrationResult result) {
        double avgQuality = result.getAverageQuality();
        return avgQuality * 1000 + result.getTotalServicesDeployed() * 10;
    }

    /**
     * Score based on latency (lower latency = higher score).
     */
    private double calculateLatencyScore(OrchestrationResult result) {
        double avgLatency = result.getAverageLatencyMs();
        // Invert latency so lower = higher score
        double score = (100 - avgLatency) * 10;
        score += result.getTotalServicesDeployed() * 20;
        return Math.max(0, score);
    }

    /**
     * Balanced score considering all factors equally.
     */
    private double calculateBalancedScore(OrchestrationResult result) {
        double serviceScore = calculateServiceScore(result) / 100;
        double resourceScore = calculateResourceScore(result) / 100;
        double qualityScore = calculateQualityScore(result) / 100;
        double latencyScore = calculateLatencyScore(result) / 100;

        // Normalize and average
        return (serviceScore + resourceScore + qualityScore + latencyScore) / 4;
    }

    /**
     * Calculates the total resource cost of a configuration.
     */
    public double calculateConfigurationCost(ServiceConfiguration config) {
        return config.getTotalCpuCores() * 10 +
               config.getTotalMemoryGB() * 5 +
               config.getTotalDiskGB() * 1;
    }

    /**
     * Calculates the cost per service unit (cost / service value).
     */
    public double calculateCostPerUnit(ServiceConfiguration config, double serviceValue) {
        if (serviceValue <= 0) return Double.MAX_VALUE;
        return calculateConfigurationCost(config) / serviceValue;
    }

    /**
     * Calculates resource utilization percentage.
     */
    public double calculateUtilization(OrchestrationResult result, ResourceBudget totalBudget) {
        double cpuUtil = (result.getTotalCpuUsed() / totalBudget.getCpuCores()) * 100;
        double memUtil = (result.getTotalMemoryUsedGB() / totalBudget.getMemoryGB()) * 100;
        double diskUtil = (result.getTotalDiskUsedGB() / totalBudget.getDiskGB()) * 100;

        return (cpuUtil + memUtil + diskUtil) / 3;
    }
}
