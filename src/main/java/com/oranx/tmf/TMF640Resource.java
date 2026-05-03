package com.oranx.tmf;

import com.oranx.model.*;
import com.oranx.engine.OrchestrationEngine;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * TMF640: Service Activation API
 * Manages the activation and deactivation of xApp services.
 *
 * TMF640 defines APIs for:
 * - Service activation requests
 * - Service status queries
 * - Service modification and termination
 */
@Path("/tmf-api/serviceActivation/v4")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TMF640Resource {

    private static final Logger LOG = LoggerFactory.getLogger(TMF640Resource.class);

    @Inject
    OrchestrationEngine orchestrationEngine;

    // In-memory service storage
    private static final Map<String, Service> services = new HashMap<>();
    private static final Map<String, ServiceActivation> activations = new HashMap<>();

    /**
     * Activate a service.
     * POST /service
     */
    @POST
    @Path("/service")
    public Response activateService(ServiceActivationRequest request) {
        LOG.info("Activating service: {}", request);

        try {
            String activationId = "SA-" + UUID.randomUUID().toString().substring(0, 8);

            // Create or retrieve service
            Service service = services.get(request.getServiceId());
            if (service == null) {
                service = new Service();
                service.setId(request.getServiceId());
                service.setName(request.getServiceName());
                service.setConfigurationType(request.getConfigurationType());
                services.put(service.getId(), service);
            }

            // Create activation record
            ServiceActivation activation = new ServiceActivation();
            activation.setId(activationId);
            activation.setServiceId(service.getId());
            activation.setServiceName(service.getName());
            activation.setState(ActivationState.IN_PROGRESS);
            activation.setRequestedStartDate(LocalDateTime.now());

            // For demo: simulate successful activation
            activation.setState(ActivationState.ACTIVE);
            activation.setActivationDate(LocalDateTime.now());
            activation.setServiceCharacteristics(buildServiceCharacteristics(request));

            activations.put(activationId, activation);

            LOG.info("Service activated: {}", activationId);
            return Response.status(Response.Status.CREATED).entity(activation).build();

        } catch (Exception e) {
            LOG.error("Failed to activate service", e);
            return Response.serverError()
                .entity(createErrorResponse("ACTIVATION_FAILED", e.getMessage()))
                .build();
        }
    }

    /**
     * Get service activation status.
     * GET /service/{id}
     */
    @GET
    @Path("/service/{id}")
    public Response getService(@PathParam("id") String id) {
        LOG.info("Getting service: {}", id);

        ServiceActivation activation = activations.get(id);
        if (activation == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("SERVICE_NOT_FOUND", "Service not found: " + id))
                .build();
        }

        return Response.ok(activation).build();
    }

    /**
     * List all service activations.
     * GET /service
     */
    @GET
    @Path("/service")
    public Response listServices(
            @QueryParam("state") ActivationState state,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        List<ServiceActivation> all = new ArrayList<>(activations.values());

        // Filter by state if specified
        if (state != null) {
            all = all.stream()
                .filter(s -> s.getState() == state)
                .toList();
        }

        int total = all.size();
        List<ServiceActivation> page = all.stream()
            .skip(offset)
            .limit(limit)
            .toList();

        return Response.ok(page)
            .header("X-Total-Count", total)
            .header("X-Result-Count", page.size())
            .build();
    }

    /**
     * Modify a service (update characteristics).
     * PATCH /service/{id}
     */
    @PATCH
    @Path("/service/{id}")
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    public Response modifyService(
            @PathParam("id") String id,
            ServiceModificationRequest modification) {

        LOG.info("Modifying service: {}", id);

        ServiceActivation activation = activations.get(id);
        if (activation == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("SERVICE_NOT_FOUND", "Service not found: " + id))
                .build();
        }

        // Apply modifications
        if (modification.getServiceCharacteristics() != null) {
            activation.getServiceCharacteristics().putAll(modification.getServiceCharacteristics());
        }

        if (modification.getState() != null) {
            activation.setState(modification.getState());
        }

        return Response.ok(activation).build();
    }

    /**
     * Deactivate/terminate a service.
     * DELETE /service/{id}
     */
    @DELETE
    @Path("/service/{id}")
    public Response deactivateService(@PathParam("id") String id) {
        LOG.info("Deactivating service: {}", id);

        ServiceActivation activation = activations.get(id);
        if (activation == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("SERVICE_NOT_FOUND", "Service not found: " + id))
                .build();
        }

        // Check if service can be deactivated
        if (activation.getState() == ActivationState.TERMINATED) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(createErrorResponse("SERVICE_ALREADY_TERMINATED", "Service is already terminated"))
                .build();
        }

        // Deactivate service
        activation.setState(ActivationState.TERMINATED);
        activation.setTerminationDate(LocalDateTime.now());

        return Response.noContent().build();
    }

    /**
     * Get service health status.
     * GET /service/{id}/health
     */
    @GET
    @Path("/service/{id}/health")
    public Response getServiceHealth(@PathParam("id") String id) {
        LOG.info("Getting service health: {}", id);

        ServiceActivation activation = activations.get(id);
        if (activation == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("SERVICE_NOT_FOUND", "Service not found: " + id))
                .build();
        }

        ServiceHealth health = new ServiceHealth();
        health.setServiceId(id);
        health.setStatus(activation.getState() == ActivationState.ACTIVE ? "HEALTHY" : "UNHEALTHY");
        health.setUptimePercent(99.5);
        health.setLastCheckTime(LocalDateTime.now());
        health.setHealthIndicators(Map.of(
            "quality", 0.95,
            "latency", 1.5,
            "cpu_utilization", 45.2,
            "memory_utilization", 62.8
        ));

        return Response.ok(health).build();
    }

    private Map<String, Object> buildServiceCharacteristics(ServiceActivationRequest request) {
        Map<String, Object> characteristics = new HashMap<>();

        if (request.getQualityTarget() != null) {
            characteristics.put("quality", request.getQualityTarget());
        }
        if (request.getLatencyTargetMs() != null) {
            characteristics.put("latency", request.getLatencyTargetMs());
        }
        if (request.getConfigurationType() != null) {
            characteristics.put("configuration", request.getConfigurationType().getName());
        }

        return characteristics;
    }

    private ErrorResponse createErrorResponse(String code, String message) {
        ErrorResponse error = new ErrorResponse();
        error.setCode(code);
        error.setMessage(message);
        return error;
    }

    // TMF Data Models
    public static class ServiceActivationRequest {
        private String serviceId;
        private String serviceName;
        private ServiceConfigurationType configurationType;
        private Double qualityTarget;
        private Double latencyTargetMs;

        public String getServiceId() { return serviceId; }
        public void setServiceId(String serviceId) { this.serviceId = serviceId; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public ServiceConfigurationType getConfigurationType() { return configurationType; }
        public void setConfigurationType(ServiceConfigurationType configurationType) { this.configurationType = configurationType; }
        public Double getQualityTarget() { return qualityTarget; }
        public void setQualityTarget(Double qualityTarget) { this.qualityTarget = qualityTarget; }
        public Double getLatencyTargetMs() { return latencyTargetMs; }
        public void setLatencyTargetMs(Double latencyTargetMs) { this.latencyTargetMs = latencyTargetMs; }
    }

    public static class ServiceModificationRequest {
        private ActivationState state;
        private Map<String, Object> serviceCharacteristics;

        public ActivationState getState() { return state; }
        public void setState(ActivationState state) { this.state = state; }
        public Map<String, Object> getServiceCharacteristics() { return serviceCharacteristics; }
        public void setServiceCharacteristics(Map<String, Object> serviceCharacteristics) { this.serviceCharacteristics = serviceCharacteristics; }
    }

    public static class ServiceActivation {
        private String id;
        private String serviceId;
        private String serviceName;
        private ActivationState state;
        private LocalDateTime requestedStartDate;
        private LocalDateTime activationDate;
        private LocalDateTime terminationDate;
        private Map<String, Object> serviceCharacteristics;

        public ServiceActivation() {
            this.serviceCharacteristics = new HashMap<>();
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getServiceId() { return serviceId; }
        public void setServiceId(String serviceId) { this.serviceId = serviceId; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public ActivationState getState() { return state; }
        public void setState(ActivationState state) { this.state = state; }
        public LocalDateTime getRequestedStartDate() { return requestedStartDate; }
        public void setRequestedStartDate(LocalDateTime requestedStartDate) { this.requestedStartDate = requestedStartDate; }
        public LocalDateTime getActivationDate() { return activationDate; }
        public void setActivationDate(LocalDateTime activationDate) { this.activationDate = activationDate; }
        public LocalDateTime getTerminationDate() { return terminationDate; }
        public void setTerminationDate(LocalDateTime terminationDate) { this.terminationDate = terminationDate; }
        public Map<String, Object> getServiceCharacteristics() { return serviceCharacteristics; }
        public void setServiceCharacteristics(Map<String, Object> serviceCharacteristics) { this.serviceCharacteristics = serviceCharacteristics; }
    }

    public static class ServiceHealth {
        private String serviceId;
        private String status;
        private double uptimePercent;
        private LocalDateTime lastCheckTime;
        private Map<String, Double> healthIndicators;

        public String getServiceId() { return serviceId; }
        public void setServiceId(String serviceId) { this.serviceId = serviceId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getUptimePercent() { return uptimePercent; }
        public void setUptimePercent(double uptimePercent) { this.uptimePercent = uptimePercent; }
        public LocalDateTime getLastCheckTime() { return lastCheckTime; }
        public void setLastCheckTime(LocalDateTime lastCheckTime) { this.lastCheckTime = lastCheckTime; }
        public Map<String, Double> getHealthIndicators() { return healthIndicators; }
        public void setHealthIndicators(Map<String, Double> healthIndicators) { this.healthIndicators = healthIndicators; }
    }

    public enum ActivationState {
        PENDING, IN_PROGRESS, ACTIVE, SUSPENDED, TERMINATED, FAILED
    }

    public static class ErrorResponse {
        private String code;
        private String message;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
