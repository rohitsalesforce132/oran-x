package com.oranx.engine;

import com.oranx.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that orchestration results meet all constraints.
 * Checks resource budgets, quality thresholds, and latency constraints.
 */
public class FeasibilityChecker {

    private static final Logger LOG = LoggerFactory.getLogger(FeasibilityChecker.class);

    /**
     * Validates an orchestration result against its constraints.
     */
    public FeasibilityReport validate(OrchestrationResult result) {
        FeasibilityReport report = new FeasibilityReport();

        // Check resource constraints
        checkResourceConstraints(result, report);

        // Check quality compliance
        checkQualityCompliance(result, report);

        // Check latency compliance
        checkLatencyCompliance(result, report);

        // Check configuration completeness
        checkConfigurationCompleteness(result, report);

        // Generate recommendations if not feasible
        if (!report.isFeasible()) {
            generateRecommendations(result, report);
        }

        return report;
    }

    /**
     * Checks if resource constraints are respected.
     */
    private void checkResourceConstraints(OrchestrationResult result, FeasibilityReport report) {
        double cpuUsed = result.getTotalCpuUsed();
        double memUsed = result.getTotalMemoryUsedGB();
        double diskUsed = result.getTotalDiskUsedGB();

        // Calculate utilization percentages
        double cpuUtil = cpuUsed > 0 ? cpuUsed / 100.0 * 100 : 0;  // Assume 100 cores max
        double memUtil = memUsed > 0 ? memUsed / 128.0 * 100 : 0;  // Assume 128GB max
        double diskUtil = diskUsed > 0 ? diskUsed / 500.0 * 100 : 0;  // Assume 500GB max

        report.getResourceUtilization().put("cpu_percent", cpuUtil);
        report.getResourceUtilization().put("memory_percent", memUtil);
        report.getResourceUtilization().put("disk_percent", diskUtil);

        // Check for over-provisioning
        if (cpuUtil > 90) {
            report.addViolation(new FeasibilityReport.ConstraintViolation(
                "resource",
                "CPU utilization exceeds 90%: " + String.format("%.1f%%", cpuUtil),
                FeasibilityReport.ViolationSeverity.HIGH
            ));
        }

        if (memUtil > 90) {
            report.addViolation(new FeasibilityReport.ConstraintViolation(
                "resource",
                "Memory utilization exceeds 90%: " + String.format("%.1f%%", memUtil),
                FeasibilityReport.ViolationSeverity.HIGH
            ));
        }

        LOG.debug("Resource check: CPU={:.1f}%, Mem={:.1f}%, Disk={:.1f}%",
            cpuUtil, memUtil, diskUtil);
    }

    /**
     * Checks if quality targets are met for all services.
     */
    private void checkQualityCompliance(OrchestrationResult result, FeasibilityReport report) {
        boolean qualityCompliant = true;
        double minQuality = Double.MAX_VALUE;

        for (ServiceConfiguration config : result.getDeployedConfigurations()) {
            if (config.getTotalQuality() < 0.8) {  // 80% minimum quality
                qualityCompliant = false;
                report.addViolation(new FeasibilityReport.ConstraintViolation(
                    "quality",
                    String.format("Service %s quality %.3f below threshold 0.8",
                        config.getServiceId(), config.getTotalQuality()),
                    FeasibilityReport.ViolationSeverity.MEDIUM
                ));
            }
            minQuality = Math.min(minQuality, config.getTotalQuality());
        }

        report.setQualityCompliance(qualityCompliant);
        result.setAverageQuality(result.getDeployedConfigurations().stream()
            .mapToDouble(ServiceConfiguration::getTotalQuality)
            .average()
            .orElse(0.0));

        LOG.debug("Quality check: compliant={}, min_quality={:.3f}", qualityCompliant, minQuality);
    }

    /**
     * Checks if latency targets are met for all services.
     */
    private void checkLatencyCompliance(OrchestrationResult result, FeasibilityReport report) {
        boolean latencyCompliant = true;
        double maxLatency = 0;

        for (ServiceConfiguration config : result.getDeployedConfigurations()) {
            if (config.getTotalLatencyMs() > 10.0) {  // 10ms maximum latency
                latencyCompliant = false;
                report.addViolation(new FeasibilityReport.ConstraintViolation(
                    "latency",
                    String.format("Service %s latency %.2fms exceeds threshold 10ms",
                        config.getServiceId(), config.getTotalLatencyMs()),
                    FeasibilityReport.ViolationSeverity.HIGH
                ));
            }
            maxLatency = Math.max(maxLatency, config.getTotalLatencyMs());
        }

        report.setLatencyCompliance(latencyCompliant);
        result.setAverageLatencyMs(result.getDeployedConfigurations().stream()
            .mapToDouble(ServiceConfiguration::getTotalLatencyMs)
            .average()
            .orElse(0.0));

        LOG.debug("Latency check: compliant={}, max_latency={:.2f}ms", latencyCompliant, maxLatency);
    }

    /**
     * Checks if all configurations are complete (all functions assigned).
     */
    private void checkConfigurationCompleteness(OrchestrationResult result, FeasibilityReport report) {
        for (ServiceConfiguration config : result.getDeployedConfigurations()) {
            if (!config.isComplete()) {
                report.addViolation(new FeasibilityReport.ConstraintViolation(
                    "configuration",
                    String.format("Service %s configuration is incomplete",
                        config.getServiceId()),
                    FeasibilityReport.ViolationSeverity.CRITICAL
                ));
                report.setFeasible(false);
            }
        }
    }

    /**
     * Generates recommendations for improving feasibility.
     */
    private void generateRecommendations(OrchestrationResult result, FeasibilityReport report) {
        // Analyze constraint violations and suggest improvements
        boolean resourceIssue = report.getConstraintViolations().stream()
            .anyMatch(v -> v.getType().equals("resource"));

        boolean qualityIssue = report.getConstraintViolations().stream()
            .anyMatch(v -> v.getType().equals("quality"));

        boolean latencyIssue = report.getConstraintViolations().stream()
            .anyMatch(v -> v.getType().equals("latency"));

        if (resourceIssue) {
            report.addRecommendation("Consider increasing resource budget or reducing xApp resource requirements");
            report.addRecommendation("Enable semantic equivalence to share xApps across services");
        }

        if (qualityIssue) {
            report.addRecommendation("Select xApps with higher quality scores");
            report.addRecommendation("Consider relaxing quality thresholds if possible");
        }

        if (latencyIssue) {
            report.addRecommendation("Select xApps with lower theta (faster processing)");
            report.addRecommendation("Consider service configurations with fewer function stages");
        }

        double deploymentRate = result.getDeploymentRate();
        if (deploymentRate < 0.5) {
            report.addRecommendation("Significant service failure rate - review service priorities and constraints");
        }
    }
}
