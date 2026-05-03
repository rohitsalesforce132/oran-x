package com.oranx.resource;

import com.oranx.llm.NLProvisioningService;
import com.oranx.llm.DeploymentExplainer;
import com.oranx.model.OrchestrationRequest;
import com.oranx.model.OrchestrationResult;
import com.oranx.model.XApp;
import com.oranx.engine.OrchestrationEngine;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * REST API for natural language xApp provisioning.
 * Provides endpoints for NL→orchestration request translation and deployment explanations.
 */
@Path("/api/v1/provision")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NLResource {

    private static final Logger LOG = LoggerFactory.getLogger(NLResource.class);

    @Inject
    NLProvisioningService nlProvisioningService;

    @Inject
    OrchestrationEngine orchestrationEngine;

    @Inject
    DeploymentExplainer deploymentExplainer;

    /**
     * Provision xApps from natural language request.
     * POST /api/v1/provision
     *
     * Example request: "I need ultra-low latency traffic forecasting with 95% accuracy, budget constrained to 8GB RAM"
     */
    @POST
    public Response provision(NLProvisioningRequest request) {
        LOG.info("NL provisioning request: {}", request.getRequest());

        try {
            // Parse natural language to orchestration request
            OrchestrationRequest orchRequest = nlProvisioningService.parseRequest(request.getRequest());

            // Get xApp recommendations
            var recommendations = nlProvisioningService.recommendXApps(request.getRequest(), orchRequest);

            // Get sample xApp catalog
            List<XApp> xAppCatalog = createSampleXAppCatalog();

            // Execute orchestration
            OrchestrationResult result = orchestrationEngine.orchestrate(orchRequest, xAppCatalog);

            // Generate explanation
            String explanation = nlProvisioningService.explainDeployment(result, orchRequest);

            // Build response
            NLProvisioningResponse response = new NLProvisioningResponse();
            response.setOrchestrationRequest(orchRequest);
            response.setOrchestrationResult(result);
            response.setRecommendations(recommendations);
            response.setExplanation(explanation);

            LOG.info("NL provisioning completed: {} services deployed",
                result.getTotalServicesDeployed());

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("NL provisioning failed", e);
            return Response.serverError()
                .entity(createErrorResponse("NL_PROVISIONING_FAILED", e.getMessage()))
                .build();
        }
    }

    /**
     * Parse natural language to orchestration request (without execution).
     * POST /api/v1/provision/parse
     */
    @POST
    @Path("/parse")
    public Response parse(NLProvisioningRequest request) {
        LOG.info("Parsing NL request: {}", request.getRequest());

        try {
            OrchestrationRequest orchRequest = nlProvisioningService.parseRequest(request.getRequest());
            return Response.ok(orchRequest).build();

        } catch (Exception e) {
            LOG.error("NL parsing failed", e);
            return Response.serverError()
                .entity(createErrorResponse("NL_PARSING_FAILED", e.getMessage()))
                .build();
        }
    }

    /**
     * Generate explanation for an orchestration result.
     * POST /api/v1/provision/explain
     */
    @POST
    @Path("/explain")
    public Response explain(ExplainRequest request) {
        LOG.info("Generating explanation for result: {}", request.getResult().getRequestId());

        try {
            String explanation = deploymentExplainer.explain(request.getResult(), request.getRequest());

            ExplainResponse response = new ExplainResponse();
            response.setExplanation(explanation);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("Explanation generation failed", e);
            return Response.serverError()
                .entity(createErrorResponse("EXPLANATION_FAILED", e.getMessage()))
                .build();
        }
    }

    /**
     * Creates a sample xApp catalog.
     */
    private List<XApp> createSampleXAppCatalog() {
        List<XApp> catalog = new ArrayList<>();

        catalog.add(new XApp("xapp-tf-0", "Forecaster Lite", com.oranx.model.RANFunction.TRAFFIC_FORECASTER,
            0.5, 1.0, 5.0, 0.85, 0.8));
        catalog.add(new XApp("xapp-tf-1", "Forecaster Pro", com.oranx.model.RANFunction.TRAFFIC_FORECASTER,
            1.5, 3.0, 10.0, 0.95, 1.5));
        catalog.add(new XApp("xapp-tc-0", "Classificator Lite", com.oranx.model.RANFunction.TRAFFIC_CLASSIFICATOR,
            0.3, 0.8, 3.0, 0.82, 0.5));
        catalog.add(new XApp("xapp-tc-1", "Classificator Pro", com.oranx.model.RANFunction.TRAFFIC_CLASSIFICATOR,
            1.0, 2.0, 8.0, 0.94, 1.0));
        catalog.add(new XApp("xapp-ns-0", "Slicer Lite", com.oranx.model.RANFunction.NETWORK_SLICER,
            0.4, 1.5, 4.0, 0.88, 0.6));
        catalog.add(new XApp("xapp-ns-1", "Slicer Pro", com.oranx.model.RANFunction.NETWORK_SLICER,
            1.2, 2.5, 12.0, 0.96, 1.2));

        return catalog;
    }

    private ErrorResponse createErrorResponse(String code, String message) {
        ErrorResponse error = new ErrorResponse();
        error.setCode(code);
        error.setMessage(message);
        return error;
    }

    // Request/Response DTOs
    public static class NLProvisioningRequest {
        private String request;

        public String getRequest() { return request; }
        public void setRequest(String request) { this.request = request; }
    }

    public static class NLProvisioningResponse {
        private OrchestrationRequest orchestrationRequest;
        private OrchestrationResult orchestrationResult;
        private Object recommendations;
        private String explanation;

        public OrchestrationRequest getOrchestrationRequest() { return orchestrationRequest; }
        public void setOrchestrationRequest(OrchestrationRequest orchestrationRequest) { this.orchestrationRequest = orchestrationRequest; }
        public OrchestrationResult getOrchestrationResult() { return orchestrationResult; }
        public void setOrchestrationResult(OrchestrationResult orchestrationResult) { this.orchestrationResult = orchestrationResult; }
        public Object getRecommendations() { return recommendations; }
        public void setRecommendations(Object recommendations) { this.recommendations = recommendations; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }

    public static class ExplainRequest {
        private OrchestrationResult result;
        private OrchestrationRequest request;

        public OrchestrationResult getResult() { return result; }
        public void setResult(OrchestrationResult result) { this.result = result; }
        public OrchestrationRequest getRequest() { return request; }
        public void setRequest(OrchestrationRequest request) { this.request = request; }
    }

    public static class ExplainResponse {
        private String explanation;

        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
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
