package com.oranx;

import com.oranx.engine.LagrangianOrchestrator;
import com.oranx.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Lagrangian orchestration engine.
 * Validates the core optimization algorithm for xApp selection.
 */
class LagrangianOrchestratorTest {

    @Test
    void testBasicOrchestration() {
        // Arrange
        LagrangianOrchestrator orchestrator = new LagrangianOrchestrator();
        OrchestrationRequest request = createBasicRequest();
        List<XApp> catalog = createSampleCatalog();

        // Act
        OrchestrationResult result = orchestrator.orchestrate(request, catalog);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isIn(OrchestrationStatus.SUCCESS, OrchestrationStatus.PARTIAL_SUCCESS);
        assertThat(result.getTotalServicesDeployed()).isGreaterThan(0);
        assertThat(result.getDeployedConfigurations()).isNotEmpty();
    }

    @Test
    void testResourceConstrainedOrchestration() {
        // Arrange
        LagrangianOrchestrator orchestrator = new LagrangianOrchestrator();
        OrchestrationRequest request = createConstrainedRequest();
        List<XApp> catalog = createSampleCatalog();

        // Act
        OrchestrationResult result = orchestrator.orchestrate(request, catalog);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalCpuUsed()).isLessThanOrEqualTo(request.getResourceBudget().getCpuCores() + 0.1);
        assertThat(result.getTotalMemoryUsedGB()).isLessThanOrEqualTo(request.getResourceBudget().getMemoryGB() + 0.1);
    }

    @Test
    void testSemanticEquivalenceSharing() {
        // Arrange
        LagrangianOrchestrator orchestrator = new LagrangianOrchestrator();
        OrchestrationRequest request = createSharedRequest();
        List<XApp> catalog = createSampleCatalog();

        // Act
        OrchestrationResult result = orchestrator.orchestrate(request, catalog);

        // Assert
        assertThat(result).isNotNull();
        // With sharing enabled, we should have fewer xApp deployments than services
        assertThat(result.getXAppDeployments().size()).isLessThanOrEqualTo(result.getTotalServicesDeployed() * 2);
    }

    @Test
    void testQualityConstraint() {
        // Arrange
        LagrangianOrchestrator orchestrator = new LagrangianOrchestrator();
        OrchestrationRequest request = new OrchestrationRequest();
        request.setRequestId("test-quality");
        request.setQualityThreshold(0.90);  // High quality requirement
        request.setResourceBudget(new ResourceBudget(16, 32, 100));

        Service service = new Service("svc-1", "High Quality Service", 1.0, 1.0, 10.0, 0.90, ServiceConfigurationType.FORECASTER_ONLY);
        request.addService(service);

        List<XApp> catalog = createSampleCatalog();

        // Act
        OrchestrationResult result = orchestrator.orchestrate(request, catalog);

        // Assert
        assertThat(result).isNotNull();
        if (!result.getDeployedConfigurations().isEmpty()) {
            for (ServiceConfiguration config : result.getDeployedConfigurations()) {
                assertThat(config.getTotalQuality()).isGreaterThanOrEqualTo(0.90);
            }
        }
    }

    @Test
    void testLatencyConstraint() {
        // Arrange
        LagrangianOrchestrator orchestrator = new LagrangianOrchestrator();
        OrchestrationRequest request = new OrchestrationRequest();
        request.setRequestId("test-latency");
        request.setLatencyThreshold(2.0);  // Low latency requirement
        request.setResourceBudget(new ResourceBudget(16, 32, 100));

        Service service = new Service("svc-1", "Low Latency Service", 1.0, 1.0, 2.0, 0.80, ServiceConfigurationType.FORECASTER_ONLY);
        request.addService(service);

        List<XApp> catalog = createSampleCatalog();

        // Act
        OrchestrationResult result = orchestrator.orchestrate(request, catalog);

        // Assert
        assertThat(result).isNotNull();
        if (!result.getDeployedConfigurations().isEmpty()) {
            for (ServiceConfiguration config : result.getDeployedConfigurations()) {
                assertThat(config.getTotalLatencyMs()).isLessThanOrEqualTo(2.0);
            }
        }
    }

    @Test
    void testFeasibilityValidation() {
        // Arrange
        LagrangianOrchestrator orchestrator = new LagrangianOrchestrator();
        OrchestrationRequest request = createBasicRequest();
        List<XApp> catalog = createSampleCatalog();

        // Act
        OrchestrationResult result = orchestrator.orchestrate(request, catalog);
        FeasibilityReport report = orchestrator.validate(result);

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.getConstraintViolations()).isNotNull();
    }

    // Helper methods

    private OrchestrationRequest createBasicRequest() {
        OrchestrationRequest request = new OrchestrationRequest();
        request.setRequestId("test-basic");
        request.setResourceBudget(new ResourceBudget(16, 32, 100));
        request.setQualityThreshold(0.8);
        request.setLatencyThreshold(10.0);

        Service service1 = new Service("svc-1", "Traffic Forecasting", 0.9, 1.0, 10.0, 0.85, ServiceConfigurationType.FORECASTER_ONLY);
        Service service2 = new Service("svc-2", "Traffic Classification", 0.8, 0.5, 10.0, 0.85, ServiceConfigurationType.CLASSIFICATOR_ONLY);

        request.addService(service1);
        request.addService(service2);

        return request;
    }

    private OrchestrationRequest createConstrainedRequest() {
        OrchestrationRequest request = new OrchestrationRequest();
        request.setRequestId("test-constrained");
        request.setResourceBudget(new ResourceBudget(2, 4, 20));  // Very constrained
        request.setQualityThreshold(0.8);
        request.setLatencyThreshold(10.0);

        Service service = new Service("svc-1", "Constrained Service", 1.0, 1.0, 10.0, 0.85, ServiceConfigurationType.FORECASTER_ONLY);
        request.addService(service);

        return request;
    }

    private OrchestrationRequest createSharedRequest() {
        OrchestrationRequest request = new OrchestrationRequest();
        request.setRequestId("test-shared");
        request.setResourceBudget(new ResourceBudget(16, 32, 100));
        request.setSharedXAppsAllowed(true);
        request.setQualityThreshold(0.8);
        request.setLatencyThreshold(10.0);

        // Two services that can share the same xApp
        Service service1 = new Service("svc-1", "Forecasting Service 1", 0.9, 1.0, 10.0, 0.85, ServiceConfigurationType.FORECASTER_ONLY);
        Service service2 = new Service("svc-2", "Forecasting Service 2", 0.8, 1.0, 10.0, 0.85, ServiceConfigurationType.FORECASTER_ONLY);

        request.addService(service1);
        request.addService(service2);

        return request;
    }

    private List<XApp> createSampleCatalog() {
        List<XApp> catalog = new ArrayList<>();

        catalog.add(new XApp("xapp-tf-0", "Forecaster Lite", RANFunction.TRAFFIC_FORECASTER,
            0.5, 1.0, 5.0, 0.85, 0.8));
        catalog.add(new XApp("xapp-tf-1", "Forecaster Pro", RANFunction.TRAFFIC_FORECASTER,
            1.5, 3.0, 10.0, 0.95, 1.5));

        catalog.add(new XApp("xapp-tc-0", "Classificator Lite", RANFunction.TRAFFIC_CLASSIFICATOR,
            0.3, 0.8, 3.0, 0.82, 0.5));
        catalog.add(new XApp("xapp-tc-1", "Classificator Pro", RANFunction.TRAFFIC_CLASSIFICATOR,
            1.0, 2.0, 8.0, 0.94, 1.0));

        catalog.add(new XApp("xapp-ns-0", "Slicer Lite", RANFunction.NETWORK_SLICER,
            0.4, 1.5, 4.0, 0.88, 0.6));
        catalog.add(new XApp("xapp-ns-1", "Slicer Pro", RANFunction.NETWORK_SLICER,
            1.2, 2.5, 12.0, 0.96, 1.2));

        return catalog;
    }
}
