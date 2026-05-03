package com.oranx.resource;

import com.oranx.model.Service;
import com.oranx.model.ServiceConfigurationType;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * REST API for managing service catalog and service configurations.
 * Provides endpoints to list, search, and manage available services.
 */
@Path("/api/v1/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceResource {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceResource.class);

    // In-memory service catalog storage
    private static final Map<String, Service> serviceCatalog = new HashMap<>();

    static {
        initializeCatalog();
    }

    /**
     * List all available services.
     * GET /api/v1/services
     */
    @GET
    public Response listServices(
            @QueryParam("category") String category,
            @QueryParam("minPriority") Double minPriority) {

        LOG.info("Listing services: category={}, minPriority={}", category, minPriority);

        List<Service> filtered = new ArrayList<>(serviceCatalog.values());

        // Filter by category (based on configuration type)
        if (category != null && !category.isEmpty()) {
            try {
                ServiceConfigurationType configType = ServiceConfigurationType.fromString(category);
                filtered = filtered.stream()
                    .filter(s -> s.getConfigurationType() == configType)
                    .toList();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("INVALID_CATEGORY", "Unknown category: " + category))
                    .build();
            }
        }

        // Filter by priority
        if (minPriority != null) {
            filtered = filtered.stream()
                .filter(s -> s.getPriority() >= minPriority)
                .toList();
        }

        // Sort by priority (descending)
        filtered.sort(Comparator.comparingDouble(Service::getPriority).reversed());

        return Response.ok(filtered).build();
    }

    /**
     * Get a specific service by ID.
     * GET /api/v1/services/{id}
     */
    @GET
    @Path("/{id}")
    public Response getService(@PathParam("id") String id) {
        LOG.info("Getting service: {}", id);

        Service service = serviceCatalog.get(id);
        if (service == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("SERVICE_NOT_FOUND", "Service not found: " + id))
                .build();
        }

        return Response.ok(service).build();
    }

    /**
     * Add a new service to the catalog.
     * POST /api/v1/services
     */
    @POST
    public Response addService(Service service) {
        LOG.info("Adding service: {}", service.getName());

        if (service.getId() == null || service.getId().isEmpty()) {
            service.setId("service-" + UUID.randomUUID().toString().substring(0, 8));
        }

        if (serviceCatalog.containsKey(service.getId())) {
            return Response.status(Response.Status.CONFLICT)
                .entity(createErrorResponse("SERVICE_EXISTS", "Service already exists: " + service.getId()))
                .build();
        }

        serviceCatalog.put(service.getId(), service);
        return Response.status(Response.Status.CREATED).entity(service).build();
    }

    /**
     * Update an existing service.
     * PUT /api/v1/services/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateService(@PathParam("id") String id, Service service) {
        LOG.info("Updating service: {}", id);

        Service existing = serviceCatalog.get(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("SERVICE_NOT_FOUND", "Service not found: " + id))
                .build();
        }

        // Update fields
        if (service.getName() != null) existing.setName(service.getName());
        if (service.getPriority() > 0) existing.setPriority(service.getPriority());
        if (service.getFrequency() > 0) existing.setFrequency(service.getFrequency());
        if (service.getLatencyTargetMs() > 0) existing.setLatencyTargetMs(service.getLatencyTargetMs());
        if (service.getQualityTarget() > 0) existing.setQualityTarget(service.getQualityTarget());
        if (service.getConfigurationType() != null) existing.setConfigurationType(service.getConfigurationType());

        return Response.ok(existing).build();
    }

    /**
     * Delete a service from the catalog.
     * DELETE /api/v1/services/{id}
     */
    @DELETE
    @Path("/{id}")
    public Response deleteService(@PathParam("id") String id) {
        LOG.info("Deleting service: {}", id);

        Service removed = serviceCatalog.remove(id);
        if (removed == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("SERVICE_NOT_FOUND", "Service not found: " + id))
                .build();
        }

        return Response.noContent().build();
    }

    /**
     * Search services by name or configuration type.
     * GET /api/v1/services/search
     */
    @GET
    @Path("/search")
    public Response searchServices(@QueryParam("q") String query) {
        LOG.info("Searching services: query={}", query);

        if (query == null || query.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(createErrorResponse("INVALID_QUERY", "Query parameter 'q' is required"))
                .build();
        }

        String lowerQuery = query.toLowerCase();

        List<Service> results = serviceCatalog.values().stream()
            .filter(s -> s.getName().toLowerCase().contains(lowerQuery) ||
                       s.getConfigurationType().getName().toLowerCase().contains(lowerQuery))
            .toList();

        return Response.ok(results).build();
    }

    /**
     * Get all available service configuration types.
     * GET /api/v1/services/configurationTypes
     */
    @GET
    @Path("/configurationTypes")
    public Response getConfigurationTypes() {
        LOG.info("Getting configuration types");

        Map<String, Object> response = new HashMap<>();
        response.put("configurationTypes", ServiceConfigurationType.getAllNames());
        response.put("count", ServiceConfigurationType.values().length);

        return Response.ok(response).build();
    }

    /**
     * Get services grouped by configuration type.
     * GET /api/v1/services/byConfigurationType
     */
    @GET
    @Path("/byConfigurationType")
    public Response getServicesByConfigurationType() {
        LOG.info("Getting services grouped by configuration type");

        Map<String, List<Service>> grouped = new HashMap<>();

        for (Service service : serviceCatalog.values()) {
            String typeName = service.getConfigurationType().getName();
            grouped.computeIfAbsent(typeName, k -> new ArrayList<>()).add(service);
        }

        return Response.ok(grouped).build();
    }

    /**
     * Initialize the service catalog with sample data.
     */
    private static void initializeCatalog() {
        // Traffic forecasting services
        Service forecast1 = new Service("svc-forecast-1", "Low Latency Forecasting",
            0.9, 1.0, 2.0, 0.85, ServiceConfigurationType.FORECASTER_ONLY);
        serviceCatalog.put(forecast1.getId(), forecast1);

        Service forecast2 = new Service("svc-forecast-2", "High Quality Forecasting",
            0.8, 0.5, 10.0, 0.95, ServiceConfigurationType.FORECASTER_ONLY);
        serviceCatalog.put(forecast2.getId(), forecast2);

        // Traffic classification services
        Service classify1 = new Service("svc-classify-1", "Real-time Classification",
            0.85, 1.0, 3.0, 0.90, ServiceConfigurationType.CLASSIFICATOR_ONLY);
        serviceCatalog.put(classify1.getId(), classify1);

        Service classify2 = new Service("svc-classify-2", "Deep Learning Classification",
            0.7, 0.3, 8.0, 0.96, ServiceConfigurationType.CLASSIFICATOR_ONLY);
        serviceCatalog.put(classify2.getId(), classify2);

        // Network slicing services
        Service slice1 = new Service("svc-slice-1", "Dynamic Slicing",
            0.9, 0.8, 5.0, 0.92, ServiceConfigurationType.SLICER_ONLY);
        serviceCatalog.put(slice1.getId(), slice1);

        // Full pipeline services
        Service full1 = new Service("svc-full-1", "Complete RAN Intelligence",
            1.0, 1.0, 10.0, 0.90, ServiceConfigurationType.FULL_PIPELINE);
        serviceCatalog.put(full1.getId(), full1);

        Service full2 = new Service("svc-full-2", "Premium RAN Intelligence",
            0.95, 0.5, 15.0, 0.95, ServiceConfigurationType.FULL_PIPELINE);
        serviceCatalog.put(full2.getId(), full2);

        LOG.info("Initialized service catalog with {} services", serviceCatalog.size());
    }

    private ErrorResponse createErrorResponse(String code, String message) {
        ErrorResponse error = new ErrorResponse();
        error.setCode(code);
        error.setMessage(message);
        return error;
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
