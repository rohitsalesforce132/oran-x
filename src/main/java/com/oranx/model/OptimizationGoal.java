package com.oranx.model;

/**
 * Defines optimization goals for the orchestration engine.
 */
public enum OptimizationGoal {
    /**
     * Maximize the number of services deployed (weighted by priority and frequency)
     */
    MAXIMIZE_SERVICES,

    /**
     * Minimize total resource usage while meeting all service SLAs
     */
    MINIMIZE_RESOURCES,

    /**
     * Maximize quality scores across all deployed services
     */
    MAXIMIZE_QUALITY,

    /**
     * Minimize end-to-end latency for all services
     */
    MINIMIZE_LATENCY,

    /**
     * Balance services, quality, latency, and resources equally
     */
    BALANCED
}
