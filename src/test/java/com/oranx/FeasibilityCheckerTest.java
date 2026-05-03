package com.oranx;

import com.oranx.engine.FeasibilityChecker;
import com.oranx.engine.LagrangianOrchestrator;
import com.oranx.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for feasibility checking.
 * Validates constraint validation for orchestration results.
 */
class FeasibilityCheckerTest {

    @Test
    void testFeasibleResult() {
        // Arrange
        LagrangianOrchestrator orchestrator = new LagrangianOrchestrator();
        FeasibilityChecker checker = new FeasibilityChecker();

        OrchestrationRequest request = createFeasibleRequest();
        List<XApp> catalog = createSampleCatalog();

        // Act
        OrchestrationResult result = orchestrator.orchestrate(request, catalog);
        FeasibilityReport report = checker.validate(result);

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.isQualityCompliance()).isTrue();
        assertThat(report.isLatencyCompliance()).isTrue();
    }

    @Test
    void testQualityViolation() {
        // Arrange
        FeasibilityChecker checker = new FeasibilityChecker();
        OrchestrationResult result = createResultWithQualityViolation();

        // Act
        FeasibilityReport report = checker.validate(result);

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.isQualityCompliance()).isFalse();
        assertThat(report.getConstraintViolations()).isNotEmpty();
        assertThat(report.getConstraintViolations().stream()
            .anyMatch(v -> v.getType().equals("quality"))).isTrue();
    }

    @Test
    void testLatencyViolation() {
        // Arrange
        FeasibilityChecker checker = new FeasibilityChecker();
        OrchestrationResult result = createResultWithLatencyViolation();

        // Act
        FeasibilityReport report = checker.validate(result);

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.isLatencyCompliance()).isFalse();
        assertThat(report.getConstraintViolations()).isNotEmpty();
        assertThat(report.getConstraintViolations().stream()
            .anyMatch(v -> v.getType().equals("latency"))).isTrue();
    }

    @Test
    void testResourceUtilization() {
        // Arrange
        FeasibilityChecker checker = new FeasibilityChecker();
        OrchestrationResult result = createResultWithHighResourceUsage();

        // Act
        FeasibilityReport report = checker.validate(result);

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.getResourceUtilization()).isNotEmpty();
        assertThat(report.getResourceUtilization().get("cpu_percent")).isNotNull();
        assertThat(report.getResourceUtilization().get("memory_percent")).isNotNull();
    }

    @Test
    void testIncompleteConfiguration() {
        // Arrange
        FeasibilityChecker checker = new FeasibilityChecker();
        OrchestrationResult result = createResultWithIncompleteConfig();

        // Act
        FeasibilityReport report = checker.validate(result);

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.isFeasible()).isFalse();
        assertThat(report.getConstraintViolations().stream()
            .anyMatch(v -> v.getType().equals("configuration"))).isTrue();
    }

    @Test
    void testRecommendationsGeneration() {
        // Arrange
        FeasibilityChecker checker = new FeasibilityChecker();
        OrchestrationResult result = createResultWithViolations();

        // Act
        FeasibilityReport report = checker.validate(result);

        // Assert
        assertThat(report).isNotNull();
        if (!report.isFeasible()) {
            assertThat(report.getRecommendations()).isNotEmpty();
        }
    }

    // Helper methods

    private OrchestrationRequest createFeasibleRequest() {
        OrchestrationRequest request = new OrchestrationRequest();
        request.setRequestId("test-feasible");
        request.setResourceBudget(new ResourceBudget(16, 32, 100));
        request.setQualityThreshold(0.8);
        request.setLatencyThreshold(10.0);

        Service service = new Service("svc-1", "Feasible Service", 1.0, 1.0, 10.0, 0.85, ServiceConfigurationType.FORECASTER_ONLY);
        request.addService(service);

        return request;
    }

    private OrchestrationResult createResultWithQualityViolation() {
        OrchestrationResult result = new OrchestrationResult("test-quality-violation");

        ServiceConfiguration config = new ServiceConfiguration("svc-1", ServiceConfigurationType.FORECASTER_ONLY);
        config.setTotalQuality(0.70);  // Below 0.8 threshold
        config.setTotalLatencyMs(2.0);
        config.setTotalCpuCores(1.0);
        config.setTotalMemoryGB(2.0);

        result.addConfiguration(config);
        result.setTotalServicesDeployed(1);

        return result;
    }

    private OrchestrationResult createResultWithLatencyViolation() {
        OrchestrationResult result = new OrchestrationResult("test-latency-violation");

        ServiceConfiguration config = new ServiceConfiguration("svc-1", ServiceConfigurationType.FORECASTER_ONLY);
        config.setTotalQuality(0.90);
        config.setTotalLatencyMs(15.0);  // Above 10ms threshold
        config.setTotalCpuCores(1.0);
        config.setTotalMemoryGB(2.0);

        result.addConfiguration(config);
        result.setTotalServicesDeployed(1);

        return result;
    }

    private OrchestrationResult createResultWithHighResourceUsage() {
        OrchestrationResult result = new OrchestrationResult("test-high-resource");

        ServiceConfiguration config = new ServiceConfiguration("svc-1", ServiceConfigurationType.FORECASTER_ONLY);
        config.setTotalQuality(0.90);
        config.setTotalLatencyMs(2.0);
        config.setTotalCpuCores(95.0);  // High CPU usage
        config.setTotalMemoryGB(120.0);  // High memory usage

        result.addConfiguration(config);
        result.setTotalServicesDeployed(1);
        result.setTotalCpuUsed(95.0);
        result.setTotalMemoryUsedGB(120.0);

        return result;
    }

    private OrchestrationResult createResultWithIncompleteConfig() {
        OrchestrationResult result = new OrchestrationResult("test-incomplete");

        ServiceConfiguration config = new ServiceConfiguration("svc-1", ServiceConfigurationType.FULL_PIPELINE);
        // Don't assign all required xApps - leave it incomplete
        config.setTotalQuality(0.90);
        config.setTotalLatencyMs(5.0);

        result.addConfiguration(config);
        result.setTotalServicesDeployed(1);

        return result;
    }

    private OrchestrationResult createResultWithViolations() {
        OrchestrationResult result = new OrchestrationResult("test-violations");

        ServiceConfiguration config1 = new ServiceConfiguration("svc-1", ServiceConfigurationType.FORECASTER_ONLY);
        config1.setTotalQuality(0.70);  // Quality violation
        config1.setTotalLatencyMs(15.0);  // Latency violation
        config1.setTotalCpuCores(95.0);  // Resource violation

        result.addConfiguration(config1);
        result.setTotalServicesDeployed(1);
        result.setTotalCpuUsed(95.0);

        return result;
    }

    private List<XApp> createSampleCatalog() {
        List<XApp> catalog = new ArrayList<>();

        catalog.add(new XApp("xapp-tf-0", "Forecaster Lite", RANFunction.TRAFFIC_FORECASTER,
            0.5, 1.0, 5.0, 0.85, 0.8));
        catalog.add(new XApp("xapp-tf-1", "Forecaster Pro", RANFunction.TRAFFIC_FORECASTER,
            1.5, 3.0, 10.0, 0.95, 1.5));

        return catalog;
    }
}
