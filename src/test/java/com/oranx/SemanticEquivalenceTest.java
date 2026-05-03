package com.oranx;

import com.oranx.engine.SemanticEquivalenceResolver;
import com.oranx.model.RANFunction;
import com.oranx.model.XApp;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for semantic equivalence resolution.
 * Validates xApp sharing across services.
 */
class SemanticEquivalenceTest {

    @Test
    void testXAppEquivalence() {
        // Arrange
        SemanticEquivalenceResolver resolver = new SemanticEquivalenceResolver();

        XApp xapp1 = new XApp("xapp-1", "Forecaster", RANFunction.TRAFFIC_FORECASTER,
            1.0, 2.0, 5.0, 0.95, 1.5);
        XApp xapp2 = new XApp("xapp-2", "Forecaster Clone", RANFunction.TRAFFIC_FORECASTER,
            1.0, 2.0, 5.0, 0.95, 1.5);

        // Act
        boolean equivalent = resolver.areEquivalent(xapp1, xapp2);

        // Assert
        assertThat(equivalent).isTrue();
    }

    @Test
    void testXAppNotEquivalentDifferentFunction() {
        // Arrange
        SemanticEquivalenceResolver resolver = new SemanticEquivalenceResolver();

        XApp forecaster = new XApp("xapp-tf", "Forecaster", RANFunction.TRAFFIC_FORECASTER,
            1.0, 2.0, 5.0, 0.95, 1.5);
        XApp classificator = new XApp("xapp-tc", "Classificator", RANFunction.TRAFFIC_CLASSIFICATOR,
            1.0, 2.0, 5.0, 0.95, 1.5);

        // Act
        boolean equivalent = resolver.areEquivalent(forecaster, classificator);

        // Assert
        assertThat(equivalent).isFalse();
    }

    @Test
    void testXAppNotEquivalentDifferentQuality() {
        // Arrange
        SemanticEquivalenceResolver resolver = new SemanticEquivalenceResolver();

        XApp xapp1 = new XApp("xapp-1", "Forecaster", RANFunction.TRAFFIC_FORECASTER,
            1.0, 2.0, 5.0, 0.95, 1.5);
        XApp xapp2 = new XApp("xapp-2", "Forecaster Low", RANFunction.TRAFFIC_FORECASTER,
            1.0, 2.0, 5.0, 0.80, 1.5);  // Different quality

        // Act
        boolean equivalent = resolver.areEquivalent(xapp1, xapp2);

        // Assert
        assertThat(equivalent).isFalse();
    }

    @Test
    void testEquivalenceClassResolution() {
        // Arrange
        SemanticEquivalenceResolver resolver = new SemanticEquivalenceResolver();
        List<XApp> xApps = createCatalogWithEquivalence();

        // Act
        Map<String, List<XApp>> classes = resolver.resolveEquivalenceClasses(xApps);

        // Assert
        assertThat(classes).isNotEmpty();
        // Should have fewer classes than xApps due to equivalence
        assertThat(classes.size()).isLessThan(xApps.size());
    }

    @Test
    void testSharingPlanWithSharingEnabled() {
        // Arrange
        SemanticEquivalenceResolver resolver = new SemanticEquivalenceResolver();
        List<XApp> xApps = createCatalogWithEquivalence();

        // Simulate service xApp options
        java.util.Map<String, List<XApp>> serviceOptions = new java.util.HashMap<>();
        serviceOptions.put("svc-1", List.of(xApps.get(0), xApps.get(1)));
        serviceOptions.put("svc-2", List.of(xApps.get(0), xApps.get(1)));

        // Act
        Map<XApp, List<String>> plan = resolver.buildSharingPlan(serviceOptions, true);

        // Assert
        assertThat(plan).isNotEmpty();
        // With sharing, services should share xApps
        int totalServiceUsages = plan.values().stream().mapToInt(List::size).sum();
        assertThat(totalServiceUsages).isEqualTo(2);  // 2 services
        assertThat(plan.size()).isLessThan(2);  // Fewer xApps than services
    }

    @Test
    void testSharingPlanWithoutSharing() {
        // Arrange
        SemanticEquivalenceResolver resolver = new SemanticEquivalenceResolver();
        List<XApp> xApps = createCatalogWithEquivalence();

        java.util.Map<String, List<XApp>> serviceOptions = new java.util.HashMap<>();
        serviceOptions.put("svc-1", List.of(xApps.get(0)));
        serviceOptions.put("svc-2", List.of(xApps.get(0)));

        // Act
        Map<XApp, List<String>> plan = resolver.buildSharingPlan(serviceOptions, false);

        // Assert
        // Without sharing, each service gets its own xApp instance
        int totalServiceUsages = plan.values().stream().mapToInt(List::size).sum();
        assertThat(totalServiceUsages).isEqualTo(2);
    }

    @Test
    void testSavingsCalculation() {
        // Arrange
        SemanticEquivalenceResolver resolver = new SemanticEquivalenceResolver();
        Map<XApp, List<String>> plan = new java.util.HashMap<>();

        XApp xapp = new XApp("xapp-1", "Test", RANFunction.TRAFFIC_FORECASTER,
            1.0, 2.0, 5.0, 0.95, 1.5);
        plan.put(xapp, List.of("svc-1", "svc-2", "svc-3"));

        // Act
        double savings = resolver.calculateSavings(plan, 10.0);

        // Assert
        // 3 services, 1 xApp = 2 saved instances = 20.0 savings
        assertThat(savings).isEqualTo(20.0);
    }

    private List<XApp> createCatalogWithEquivalence() {
        List<XApp> catalog = new ArrayList<>();

        // Two equivalent forecasters
        catalog.add(new XApp("xapp-tf-1", "Forecaster A", RANFunction.TRAFFIC_FORECASTER,
            1.0, 2.0, 5.0, 0.95, 1.5));
        catalog.add(new XApp("xapp-tf-2", "Forecaster B", RANFunction.TRAFFIC_FORECASTER,
            1.0, 2.0, 5.0, 0.95, 1.5));

        // Two equivalent classificators
        catalog.add(new XApp("xapp-tc-1", "Classificator A", RANFunction.TRAFFIC_CLASSIFICATOR,
            0.8, 1.5, 4.0, 0.92, 1.0));
        catalog.add(new XApp("xapp-tc-2", "Classificator B", RANFunction.TRAFFIC_CLASSIFICATOR,
            0.8, 1.5, 4.0, 0.92, 1.0));

        // One different forecaster (not equivalent)
        catalog.add(new XApp("xapp-tf-3", "Forecaster High Perf", RANFunction.TRAFFIC_FORECASTER,
            2.0, 4.0, 10.0, 0.98, 2.0));

        return catalog;
    }
}
