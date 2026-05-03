package com.oranx.model;

/**
 * Deployment status of an xApp.
 */
public enum DeploymentStatus {
    /**
     * Deployment is pending
     */
    PENDING,

    /**
     * Deployment is in progress
     */
    DEPLOYING,

    /**
     * Deployment completed successfully
     */
    DEPLOYED,

    /**
     * Deployment failed
     */
    FAILED,

    /**
     * Deployment is being stopped
     */
    STOPPING,

    /**
     * Deployment has been stopped
     */
    STOPPED,

    /**
     * Deployment is being deleted
     */
    DELETING,

    /**
     * Deployment has been deleted
     */
    DELETED
}
