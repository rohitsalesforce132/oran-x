package com.oranx.engine;

import com.oranx.model.*;

import java.util.List;

/**
 * Main orchestration engine interface.
 * Coordinates xApp selection, deployment planning, and constraint validation.
 */
public interface OrchestrationEngine {

    /**
     * Orchestrates xApp deployments for the given request.
     *
     * @param request The orchestration request containing services and constraints
     * @param xAppCatalog Available xApps for deployment
     * @return Orchestration result with deployment plan and analysis
     */
    OrchestrationResult orchestrate(OrchestrationRequest request, List<XApp> xAppCatalog);

    /**
     * Validates that a deployment plan meets all constraints.
     *
     * @param result The orchestration result to validate
     * @return Updated feasibility report
     */
    FeasibilityReport validate(OrchestrationResult result);

    /**
     * Calculates the optimization score for a given result.
     *
     * @param result The orchestration result
     * @param goal The optimization goal
     * @return Optimization score (higher is better)
     */
    double calculateOptimizationScore(OrchestrationResult result, OptimizationGoal goal);
}
