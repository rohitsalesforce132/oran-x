package com.oranx.model;

/**
 * Status of an orchestration request.
 */
public enum OrchestrationStatus {
    /**
     * Orchestration is in progress
     */
    IN_PROGRESS,

    /**
     * Orchestration completed successfully
     */
    SUCCESS,

    /**
     * Orchestration completed with partial success (some services failed)
     */
    PARTIAL_SUCCESS,

    /**
     * Orchestration failed completely
     */
    FAILURE,

    /**
     * Orchestration was cancelled
     */
    CANCELLED
}
