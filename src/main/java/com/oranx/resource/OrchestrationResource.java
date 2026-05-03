package com.oranx.resource;

import com.oranx.model.*;
import com.oranx.engine.OrchestrationEngine;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API for xApp orchestration.
 * Provides endpoints for service orchestration, deployment planning, and analysis.
 */
@Path("/api/v1/orchestrate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrchestrationResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrchestrationResource.class);

    @Inject
    OrchestrationEngine orchestrationEngine;

    /**
     * Orchestrates xApp deployments for the given request.
     * POST /api/v1/orchestrate
     */
    @POST
    public Response orchestrate(OrchestrationRequest request) {
        LOG.info("Orchestration request: {}", request.getRequestId());

        try {
            // Get sample xApp catalog (in production, load from database)
            List<XApp> xAppCatalog = createSampleXAppCatalog();

            // Execute orchestration
            OrchestrationResult result = orchestrationEngine.orchestrate(request, xAppCatalog);

            LOG.info("Orchestration completed: {} services deployed",
                result.getTotalServicesDeployed());

            return Response.ok(result).build();

        } catch (Exception e) {
            LOG.error("Orchestration failed", e);
            return Response.serverError()
                .entity(createErrorResponse("ORCHESTRATION_FAILED", e.getMessage()))
                .build();
        }
    }

    /**
     * Validates an orchestration result.
     * POST /api/v1/orchestrate/validate
     */
    @POST
    @Path("/validate")
    public Response validate(OrchestrationResult result) {
        LOG.info("Validating orchestration result: {}", result.getRequestId());

        try {
            FeasibilityReport report = orchestrationEngine.validate(result);
            return Response.ok(report).build();

        } catch (Exception e) {
            LOG.error("Validation failed", e);
            return Response.serverError()
                .entity(createErrorResponse("VALIDATION_FAILED", e.getMessage()))
                .build();
        }
    }

    /**
     * Calculates optimization score for a result.
     * POST /api/v1/orchestrate/score
     */
    @POST
    @Path("/score")
    public Response calculateScore(ScoreRequest request) {
        LOG.info("Calculating optimization score for goal: {}", request.getGoal());

        try {
            double score = orchestrationEngine.calculateOptimizationScore(
                request.getResult(), request.getGoal());

            ScoreResponse response = new ScoreResponse();
            response.setScore(score);
            response.setGoal(request.getGoal());

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("Score calculation failed", e);
            return Response.serverError()
                .entity(createErrorResponse("SCORE_CALCULATION_FAILED", e.getMessage()))
                .build();
        }
    }

    /**
     * Creates a sample xApp catalog for demo purposes.
     */
    private List<XApp> createSampleXAppCatalog() {
        List<XApp> catalog = new ArrayList<>();

        // Traffic forecaster xApps (2 implementations with different profiles)
        catalog.add(new XApp("xapp-tf-0", "Forecaster Lite", RANFunction.TRAFFIC_FORECASTER,
            0.5, 1.0, 5.0, 0.85, 0.8));  // Low resource, medium quality
        catalog.add(new XApp("xapp-tf-1", "Forecaster Pro", RANFunction.TRAFFIC_FORECASTER,
            1.5, 3.0, 10.0, 0.95, 1.5));  // High resource, high quality

        // Traffic classificator xApps (2 implementations)
        catalog.add(new XApp("xapp-tc-0", "Classificator Lite", RANFunction.TRAFFIC_CLASSIFICATOR,
            0.3, 0.8, 3.0, 0.82, 0.5));  // Fast, lower accuracy
        catalog.add(new XApp("xapp-tc-1", "Classificator Pro", RANFunction.TRAFFIC_CLASSIFICATOR,
            1.0, 2.0, 8.0, 0.94, 1.0));  // Slower, higher accuracy

        // Network slicer xApps (2 implementations)
        catalog.add(new XApp("xapp-ns-0", "Slicer Lite", RANFunction.NETWORK_SLICER,
            0.4, 1.5, 4.0, 0.88, 0.6));  // Resource-efficient
        catalog.add(new XApp("xapp-ns-1", "Slicer Pro", RANFunction.NETWORK_SLICER,
            1.2, 2.5, 12.0, 0.96, 1.2));  // High-performance

        return catalog;
    }

    private ErrorResponse createErrorResponse(String code, String message) {
        ErrorResponse error = new ErrorResponse();
        error.setCode(code);
        error.setMessage(message);
        return error;
    }

    // Request/Response DTOs
    public static class ScoreRequest {
        private OrchestrationResult result;
        private OptimizationGoal goal;

        public OrchestrationResult getResult() { return result; }
        public void setResult(OrchestrationResult result) { this.result = result; }
        public OptimizationGoal getGoal() { return goal; }
        public void setGoal(OptimizationGoal goal) { this.goal = goal; }
    }

    public static class ScoreResponse {
        private double score;
        private OptimizationGoal goal;

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public OptimizationGoal getGoal() { return goal; }
        public void setGoal(OptimizationGoal goal) { this.goal = goal; }
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
