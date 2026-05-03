package com.oranx.engine;

import com.oranx.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Lagrangian-based orchestration engine implementing the OREO algorithm.
 * Uses Lagrangian decomposition to optimize xApp selection across all services.
 *
 * Algorithm:
 * 1. Decompose the multi-service optimization problem into per-function subproblems
 * 2. Use semantic equivalence to share xApps across services
 * 3. Maximize deployed services subject to resource, quality, and latency constraints
 */
@ApplicationScoped
public class LagrangianOrchestrator implements OrchestrationEngine {

    private static final Logger LOG = LoggerFactory.getLogger(LagrangianOrchestrator.class);

    private final XAppSelector xAppSelector;
    private final SemanticEquivalenceResolver semanticResolver;
    private final FeasibilityChecker feasibilityChecker;
    private final CostCalculator costCalculator;

    public LagrangianOrchestrator() {
        this.xAppSelector = new XAppSelector();
        this.semanticResolver = new SemanticEquivalenceResolver();
        this.feasibilityChecker = new FeasibilityChecker();
        this.costCalculator = new CostCalculator();
    }

    @Override
    public OrchestrationResult orchestrate(OrchestrationRequest request, List<XApp> xAppCatalog) {
        LOG.info("Starting orchestration for request: {}", request.getRequestId());

        OrchestrationResult result = new OrchestrationResult(request.getRequestId());
        result.setTotalServicesRequested(request.getServices().size());

        try {
            // Group xApps by function for efficient selection
            Map<RANFunction, List<XApp>> xAppsByFunction = groupXAppsByFunction(xAppCatalog);

            // Generate all possible configurations for each service
            Map<Service, List<ServiceConfiguration>> configOptions = generateConfigurations(
                request.getServices(), xAppsByFunction, request);

            // Lagrangian decomposition: optimize per-function subproblems
            List<ServiceConfiguration> selectedConfigs = lagrangianDecomposition(
                configOptions, request, result);

            // Build deployment plan
            buildDeploymentPlan(selectedConfigs, result, request);

            // Validate the result
            FeasibilityReport feasibility = validate(result);
            result.setFeasibilityReport(feasibility);

            // Calculate final statistics
            calculateStatistics(result);

            // Set status based on results
            if (result.getTotalServicesDeployed() == result.getTotalServicesRequested()) {
                result.setStatus(OrchestrationStatus.SUCCESS);
            } else if (result.getTotalServicesDeployed() > 0) {
                result.setStatus(OrchestrationStatus.PARTIAL_SUCCESS);
            } else {
                result.setStatus(OrchestrationStatus.FAILURE);
            }

            LOG.info("Orchestration completed: deployed {}/{} services",
                result.getTotalServicesDeployed(), result.getTotalServicesRequested());

        } catch (Exception e) {
            LOG.error("Orchestration failed", e);
            result.setStatus(OrchestrationStatus.FAILURE);
            result.addError("Orchestration failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Groups xApps by the RAN function they implement.
     */
    private Map<RANFunction, List<XApp>> groupXAppsByFunction(List<XApp> xAppCatalog) {
        Map<RANFunction, List<XApp>> grouped = new HashMap<>();
        for (XApp xApp : xAppCatalog) {
            grouped.computeIfAbsent(xApp.getFunction(), k -> new ArrayList<>()).add(xApp);
        }
        return grouped;
    }

    /**
     * Generates all possible configurations for each service.
     * For each service, tries all configuration types and xApp combinations.
     */
    private Map<Service, List<ServiceConfiguration>> generateConfigurations(
            List<Service> services,
            Map<RANFunction, List<XApp>> xAppsByFunction,
            OrchestrationRequest request) {

        Map<Service, List<ServiceConfiguration>> configOptions = new HashMap<>();

        for (Service service : services) {
            List<ServiceConfiguration> configs = new ArrayList<>();

            for (ServiceConfigurationType configType : ServiceConfigurationType.values()) {
                // Try all possible xApp combinations for this configuration type
                List<List<XApp>> xAppCombinations = generateXAppCombinations(
                    configType.getRequiredFunctions(), xAppsByFunction);

                for (List<XApp> xAppCombo : xAppCombinations) {
                    ServiceConfiguration config = new ServiceConfiguration(
                        service.getId(), configType);

                    // Assign xApps to functions
                    for (int i = 0; i < configType.getRequiredFunctions().size(); i++) {
                        config.assignXApp(
                            configType.getRequiredFunctions().get(i),
                            xAppCombo.get(i)
                        );
                    }

                    // Check if this configuration meets service SLAs
                    if (meetsSLA(config, service, request)) {
                        configs.add(config);
                    }
                }
            }

            // Sort by cost (ascending) - prefer cheaper configurations
            configs.sort(Comparator.comparingDouble(ServiceConfiguration::getCost));
            configOptions.put(service, configs);
        }

        return configOptions;
    }

    /**
     * Generates all possible xApp combinations for a list of required functions.
     */
    private List<List<XApp>> generateXAppCombinations(
            List<RANFunction> requiredFunctions,
            Map<RANFunction, List<XApp>> xAppsByFunction) {

        if (requiredFunctions.isEmpty()) {
            return Collections.singletonList(Collections.emptyList());
        }

        List<List<XApp>> result = new ArrayList<>();
        RANFunction firstFunction = requiredFunctions.get(0);
        List<RANFunction> remainingFunctions = requiredFunctions.subList(1, requiredFunctions.size());

        // Get xApps for the first function
        List<XApp> firstXApps = xAppsByFunction.getOrDefault(firstFunction, Collections.emptyList());

        // Recursively get combinations for remaining functions
        List<List<XApp>> remainingCombinations = generateXAppCombinations(remainingFunctions, xAppsByFunction);

        // Cartesian product
        for (XApp firstXApp : firstXApps) {
            for (List<XApp> remainingCombo : remainingCombinations) {
                List<XApp> combo = new ArrayList<>();
                combo.add(firstXApp);
                combo.addAll(remainingCombo);
                result.add(combo);
            }
        }

        return result;
    }

    /**
     * Checks if a configuration meets the service's SLA requirements.
     */
    private boolean meetsSLA(ServiceConfiguration config, Service service, OrchestrationRequest request) {
        double qualityThreshold = request.getQualityThreshold();
        double latencyThreshold = request.getLatencyThreshold();

        return config.getTotalQuality() >= qualityThreshold &&
               config.getTotalLatencyMs() <= latencyThreshold;
    }

    /**
     * Lagrangian decomposition: selects optimal configurations maximizing services
     * while respecting resource constraints.
     *
     * This is a simplified implementation of the OREO algorithm.
     * In production, this would use iterative Lagrangian multiplier updates.
     */
    private List<ServiceConfiguration> lagrangianDecomposition(
            Map<Service, List<ServiceConfiguration>> configOptions,
            OrchestrationRequest request,
            OrchestrationResult result) {

        List<ServiceConfiguration> selected = new ArrayList<>();
        ResourceBudget remainingBudget = new ResourceBudget(
            request.getResourceBudget().getCpuCores(),
            request.getResourceBudget().getMemoryGB(),
            request.getResourceBudget().getDiskGB()
        );

        // Sort services by weighted value (priority * frequency)
        List<Service> sortedServices = configOptions.keySet().stream()
            .sorted(Comparator.comparingDouble(Service::getWeightedValue).reversed())
            .collect(Collectors.toList());

        // Greedy selection with resource constraints
        for (Service service : sortedServices) {
            List<ServiceConfiguration> configs = configOptions.get(service);

            if (configs.isEmpty()) {
                result.addFailedService(service);
                continue;
            }

            // Find the best configuration that fits in remaining budget
            ServiceConfiguration bestConfig = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (ServiceConfiguration config : configs) {
                if (remainingBudget.canFit(
                    config.getTotalCpuCores(),
                    config.getTotalMemoryGB(),
                    config.getTotalDiskGB())) {

                    // Calculate score: weighted value - resource cost
                    double score = service.getWeightedValue() - (config.getCost() * 0.1);

                    // If sharing xApps is allowed, prefer configurations that can reuse xApps
                    if (request.isSharedXAppsAllowed()) {
                        int sharedCount = countSharedXApps(config, selected);
                        score += sharedCount * 0.5; // Bonus for sharing
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestConfig = config;
                    }
                }
            }

            if (bestConfig != null) {
                selected.add(bestConfig);
                remainingBudget = remainingBudget.subtract(
                    bestConfig.getTotalCpuCores(),
                    bestConfig.getTotalMemoryGB(),
                    bestConfig.getTotalDiskGB()
                );
            } else {
                result.addFailedService(service);
            }
        }

        return selected;
    }

    /**
     * Counts how many xApps in a configuration are already in use by selected configs.
     * Used for semantic equivalence sharing.
     */
    private int countSharedXApps(ServiceConfiguration config, List<ServiceConfiguration> selected) {
        int count = 0;
        for (XApp xApp : config.getXAppAssignments().values()) {
            for (ServiceConfiguration selectedConfig : selected) {
                if (selectedConfig.getXAppAssignments().containsValue(xApp)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    /**
     * Builds the deployment plan from selected configurations.
     */
    private void buildDeploymentPlan(
            List<ServiceConfiguration> selectedConfigs,
            OrchestrationResult result,
            OrchestrationRequest request) {

        // Track deployed xApps for sharing
        Map<XApp, XAppDeployment> deployedXApps = new HashMap<>();

        for (ServiceConfiguration config : selectedConfigs) {
            result.addConfiguration(config);

            for (Map.Entry<RANFunction, XApp> entry : config.getXAppAssignments().entrySet()) {
                XApp xApp = entry.getValue();
                RANFunction function = entry.getKey();

                // Check if this xApp is already deployed (semantic equivalence)
                if (request.isSharedXAppsAllowed() && deployedXApps.containsKey(xApp)) {
                    // Reuse existing deployment
                    continue;
                }

                // Create new deployment
                String deploymentId = String.format("deploy-%s-%d",
                    xApp.getId(), System.currentTimeMillis());
                XAppDeployment deployment = new XAppDeployment(deploymentId, xApp, config.getServiceId(), function);
                deployment.setStatus(DeploymentStatus.DEPLOYED);

                result.addXAppDeployment(deployment);
                deployedXApps.put(xApp, deployment);
            }
        }

        result.setTotalServicesDeployed(selectedConfigs.size());
    }

    /**
     * Calculates statistics for the orchestration result.
     */
    private void calculateStatistics(OrchestrationResult result) {
        double totalQuality = 0;
        double totalLatency = 0;
        double totalCpu = 0;
        double totalMemory = 0;
        double totalDisk = 0;

        for (ServiceConfiguration config : result.getDeployedConfigurations()) {
            totalQuality += config.getTotalQuality();
            totalLatency += config.getTotalLatencyMs();
            totalCpu += config.getTotalCpuCores();
            totalMemory += config.getTotalMemoryGB();
            totalDisk += config.getTotalDiskGB();
        }

        int deployedCount = result.getDeployedConfigurations().size();
        if (deployedCount > 0) {
            result.setAverageQuality(totalQuality / deployedCount);
            result.setAverageLatencyMs(totalLatency / deployedCount);
        }

        result.setTotalCpuUsed(totalCpu);
        result.setTotalMemoryUsedGB(totalMemory);
        result.setTotalDiskUsedGB(totalDisk);
    }

    @Override
    public FeasibilityReport validate(OrchestrationResult result) {
        return feasibilityChecker.validate(result);
    }

    @Override
    public double calculateOptimizationScore(OrchestrationResult result, OptimizationGoal goal) {
        return costCalculator.calculateScore(result, goal);
    }
}
