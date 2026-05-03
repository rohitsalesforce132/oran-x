package com.oranx.resource;

import com.oranx.model.XApp;
import com.oranx.model.RANFunction;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API for managing the xApp catalog.
 * Provides endpoints to list, search, and manage available xApps.
 */
@Path("/api/v1/xapps")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class XAppResource {

    private static final Logger LOG = LoggerFactory.getLogger(XAppResource.class);

    // In-memory xApp catalog storage
    private static final Map<String, XApp> xAppCatalog = new ConcurrentHashMap<>();

    static {
        // Initialize with sample xApps
        initializeCatalog();
    }

    /**
     * List all xApps in the catalog.
     * GET /api/v1/xapps
     */
    @GET
    public Response listXApps(
            @QueryParam("function") String function,
            @QueryParam("minQuality") Double minQuality,
            @QueryParam("maxLatency") Double maxLatency) {

        LOG.info("Listing xApps: function={}, minQuality={}, maxLatency={}",
            function, minQuality, maxLatency);

        List<XApp> filtered = new ArrayList<>(xAppCatalog.values());

        // Filter by function
        if (function != null && !function.isEmpty()) {
            try {
                RANFunction func = RANFunction.fromString(function);
                filtered = filtered.stream()
                    .filter(x -> x.getFunction() == func)
                    .toList();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("INVALID_FUNCTION", "Unknown function: " + function))
                    .build();
            }
        }

        // Filter by quality
        if (minQuality != null) {
            filtered = filtered.stream()
                .filter(x -> x.getQualityScore() >= minQuality)
                .toList();
        }

        // Filter by latency
        if (maxLatency != null) {
            filtered = filtered.stream()
                .filter(x -> x.getTheta() <= maxLatency)
                .toList();
        }

        return Response.ok(filtered).build();
    }

    /**
     * Get a specific xApp by ID.
     * GET /api/v1/xapps/{id}
     */
    @GET
    @Path("/{id}")
    public Response getXApp(@PathParam("id") String id) {
        LOG.info("Getting xApp: {}", id);

        XApp xApp = xAppCatalog.get(id);
        if (xApp == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("XAPP_NOT_FOUND", "xApp not found: " + id))
                .build();
        }

        return Response.ok(xApp).build();
    }

    /**
     * Add a new xApp to the catalog.
     * POST /api/v1/xapps
     */
    @POST
    public Response addXApp(XApp xApp) {
        LOG.info("Adding xApp: {}", xApp.getName());

        if (xApp.getId() == null || xApp.getId().isEmpty()) {
            xApp.setId("xapp-" + UUID.randomUUID().toString().substring(0, 8));
        }

        if (xAppCatalog.containsKey(xApp.getId())) {
            return Response.status(Response.Status.CONFLICT)
                .entity(createErrorResponse("XAPP_EXISTS", "xApp already exists: " + xApp.getId()))
                .build();
        }

        xAppCatalog.put(xApp.getId(), xApp);
        return Response.status(Response.Status.CREATED).entity(xApp).build();
    }

    /**
     * Update an existing xApp.
     * PUT /api/v1/xapps/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateXApp(@PathParam("id") String id, XApp xApp) {
        LOG.info("Updating xApp: {}", id);

        XApp existing = xAppCatalog.get(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("XAPP_NOT_FOUND", "xApp not found: " + id))
                .build();
        }

        // Update fields
        if (xApp.getName() != null) existing.setName(xApp.getName());
        if (xApp.getCpuCores() > 0) existing.setCpuCores(xApp.getCpuCores());
        if (xApp.getMemoryGB() > 0) existing.setMemoryGB(xApp.getMemoryGB());
        if (xApp.getDiskGB() > 0) existing.setDiskGB(xApp.getDiskGB());
        if (xApp.getQualityScore() > 0) existing.setQualityScore(xApp.getQualityScore());
        if (xApp.getTheta() > 0) existing.setTheta(xApp.getTheta());
        if (xApp.getVersion() != null) existing.setVersion(xApp.getVersion());
        if (xApp.getDescription() != null) existing.setDescription(xApp.getDescription());

        return Response.ok(existing).build();
    }

    /**
     * Delete an xApp from the catalog.
     * DELETE /api/v1/xapps/{id}
     */
    @DELETE
    @Path("/{id}")
    public Response deleteXApp(@PathParam("id") String id) {
        LOG.info("Deleting xApp: {}", id);

        XApp removed = xAppCatalog.remove(id);
        if (removed == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("XAPP_NOT_FOUND", "xApp not found: " + id))
                .build();
        }

        return Response.noContent().build();
    }

    /**
     * Search xApps by name or description.
     * GET /api/v1/xapps/search
     */
    @GET
    @Path("/search")
    public Response searchXApps(@QueryParam("q") String query) {
        LOG.info("Searching xApps: query={}", query);

        if (query == null || query.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(createErrorResponse("INVALID_QUERY", "Query parameter 'q' is required"))
                .build();
        }

        String lowerQuery = query.toLowerCase();

        List<XApp> results = xAppCatalog.values().stream()
            .filter(x -> x.getName().toLowerCase().contains(lowerQuery) ||
                       (x.getDescription() != null && x.getDescription().toLowerCase().contains(lowerQuery)))
            .toList();

        return Response.ok(results).build();
    }

    /**
     * Get xApps grouped by function.
     * GET /api/v1/xapps/byFunction
     */
    @GET
    @Path("/byFunction")
    public Response getXAppsByFunction() {
        LOG.info("Getting xApps grouped by function");

        Map<String, List<XApp>> grouped = new HashMap<>();

        for (XApp xApp : xAppCatalog.values()) {
            String funcName = xApp.getFunction().getName();
            grouped.computeIfAbsent(funcName, k -> new ArrayList<>()).add(xApp);
        }

        return Response.ok(grouped).build();
    }

    /**
     * Initialize the xApp catalog with sample data.
     */
    private static void initializeCatalog() {
        // Traffic forecaster xApps
        xAppCatalog.put("xapp-tf-0", new XApp("xapp-tf-0", "Forecaster Lite", RANFunction.TRAFFIC_FORECASTER,
            0.5, 1.0, 5.0, 0.85, 0.8));
        xAppCatalog.put("xapp-tf-1", new XApp("xapp-tf-1", "Forecaster Pro", RANFunction.TRAFFIC_FORECASTER,
            1.5, 3.0, 10.0, 0.95, 1.5));

        // Traffic classificator xApps
        xAppCatalog.put("xapp-tc-0", new XApp("xapp-tc-0", "Classificator Lite", RANFunction.TRAFFIC_CLASSIFICATOR,
            0.3, 0.8, 3.0, 0.82, 0.5));
        xAppCatalog.put("xapp-tc-1", new XApp("xapp-tc-1", "Classificator Pro", RANFunction.TRAFFIC_CLASSIFICATOR,
            1.0, 2.0, 8.0, 0.94, 1.0));

        // Network slicer xApps
        xAppCatalog.put("xapp-ns-0", new XApp("xapp-ns-0", "Slicer Lite", RANFunction.NETWORK_SLICER,
            0.4, 1.5, 4.0, 0.88, 0.6));
        xAppCatalog.put("xapp-ns-1", new XApp("xapp-ns-1", "Slicer Pro", RANFunction.NETWORK_SLICER,
            1.2, 2.5, 12.0, 0.96, 1.2));

        LOG.info("Initialized xApp catalog with {} xApps", xAppCatalog.size());
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
