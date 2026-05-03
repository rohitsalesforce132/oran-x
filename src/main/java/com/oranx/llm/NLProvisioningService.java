package com.oranx.llm;

import com.oranx.model.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Natural language provisioning service using LangChain4j.
 * Translates natural language requests into structured orchestration requests.
 */
@ApplicationScoped
public class NLProvisioningService {

    private static final Logger LOG = LoggerFactory.getLogger(NLProvisioningService.class);

    private final ChatLanguageModel chatModel;
    private final SLAExtractor slaExtractor;
    private final XAppRecommender xAppRecommender;

    public NLProvisioningService() {
        // Initialize OpenAI model (API key from environment or config)
        String apiKey = System.getenv().getOrDefault("OPENAI_API_KEY", "dummy-key");
        this.chatModel = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4")
            .temperature(0.1)  // Low temperature for consistent parsing
            .build();

        this.slaExtractor = new SLAExtractor(chatModel);
        this.xAppRecommender = new XAppRecommender(chatModel);
    }

    /**
     * Parses a natural language request into an OrchestrationRequest.
     *
     * Example: "I need ultra-low latency traffic forecasting with 95% accuracy, budget constrained to 8GB RAM"
     */
    public OrchestrationRequest parseRequest(String naturalLanguageRequest) {
        LOG.info("Parsing natural language request: {}", naturalLanguageRequest);

        try {
            // Extract service type and SLA parameters using LLM
            SLAParameters sla = slaExtractor.extract(naturalLanguageRequest);

            // Build orchestration request
            OrchestrationRequest request = new OrchestrationRequest();
            request.setRequestId("nl-" + System.currentTimeMillis());

            // Create service from extracted parameters
            Service service = new Service();
            service.setId(sla.getServiceType() + "-" + System.currentTimeMillis());
            service.setName(sla.getServiceType());
            service.setConfigurationType(deduceConfigurationType(sla.getServiceType()));
            service.setQualityTarget(sla.getQualityTarget());
            service.setLatencyTargetMs(sla.getLatencyTargetMs());

            request.addService(service);

            // Set resource budget if specified
            if (sla.getCpuCores() > 0 || sla.getMemoryGB() > 0 || sla.getDiskGB() > 0) {
                ResourceBudget budget = new ResourceBudget(
                    sla.getCpuCores() > 0 ? sla.getCpuCores() : 16,
                    sla.getMemoryGB() > 0 ? sla.getMemoryGB() : 32,
                    sla.getDiskGB() > 0 ? sla.getDiskGB() : 100
                );
                request.setResourceBudget(budget);
            } else {
                // Default budget
                request.setResourceBudget(new ResourceBudget(16, 32, 100));
            }

            // Set optimization goal based on request
            request.setOptimizationGoal(deduceOptimizationGoal(naturalLanguageRequest));

            // Set SLA parameters
            request.setQualityThreshold(sla.getQualityTarget() > 0 ? sla.getQualityTarget() : 0.8);
            request.setLatencyThreshold(sla.getLatencyTargetMs() > 0 ? sla.getLatencyTargetMs() : 10.0);

            LOG.info("Parsed request: service={}, Q={}, L={}, budget={}",
                sla.getServiceType(), sla.getQualityTarget(), sla.getLatencyTargetMs(), request.getResourceBudget());

            return request;

        } catch (Exception e) {
            LOG.error("Failed to parse natural language request", e);
            throw new RuntimeException("Failed to parse request: " + e.getMessage(), e);
        }
    }

    /**
     * Recommends xApp configurations based on natural language preferences.
     */
    public XAppRecommendation recommendXApps(String naturalLanguageRequest, OrchestrationRequest request) {
        LOG.info("Generating xApp recommendations for request: {}", request.getRequestId());

        return xAppRecommender.recommend(naturalLanguageRequest, request);
    }

    /**
     * Generates a human-readable explanation of the deployment plan.
     */
    public String explainDeployment(OrchestrationResult result, OrchestrationRequest request) {
        LOG.info("Generating deployment explanation for result: {}", result.getRequestId());

        StringBuilder explanation = new StringBuilder();
        explanation.append("## Deployment Plan Summary\n\n");

        // Overview
        explanation.append(String.format("**Services Deployed:** %d / %d\n\n",
            result.getTotalServicesDeployed(), result.getTotalServicesRequested()));

        // Resource usage
        explanation.append("**Resource Usage:**\n");
        explanation.append(String.format("- CPU: %.2f cores\n", result.getTotalCpuUsed()));
        explanation.append(String.format("- Memory: %.2f GB\n", result.getTotalMemoryUsedGB()));
        explanation.append(String.format("- Disk: %.2f GB\n\n", result.getTotalDiskUsedGB()));

        // Performance metrics
        explanation.append("**Performance:**\n");
        explanation.append(String.format("- Average Quality: %.1f%%\n", result.getAverageQuality() * 100));
        explanation.append(String.format("- Average Latency: %.2f ms\n\n", result.getAverageLatencyMs()));

        // Detailed service breakdown
        explanation.append("**Service Details:**\n");
        for (ServiceConfiguration config : result.getDeployedConfigurations()) {
            explanation.append(String.format("\n**Service %s** (%s configuration):\n",
                config.getServiceId(), config.getConfigurationType().getName()));
            explanation.append(String.format("- Quality: %.1f%%\n", config.getTotalQuality() * 100));
            explanation.append(String.format("- Latency: %.2f ms\n", config.getTotalLatencyMs()));
            explanation.append("- xApps deployed:\n");

            for (Map.Entry<RANFunction, XApp> entry : config.getXAppAssignments().entrySet()) {
                XApp xApp = entry.getValue();
                explanation.append(String.format("  - %s: %s (Q=%.1f%%, theta=%.2fms, cpu=%.1f, mem=%.1fGB)\n",
                    entry.getKey().getName(),
                    xApp.getName(),
                    xApp.getQualityScore() * 100,
                    xApp.getTheta(),
                    xApp.getCpuCores(),
                    xApp.getMemoryGB()));
            }
        }

        // Feasibility status
        if (result.getFeasibilityReport() != null) {
            explanation.append("\n**Feasibility:** ");
            explanation.append(result.getFeasibilityReport().isFeasible() ? "✅ Passed" : "⚠️ Issues detected");

            if (!result.getFeasibilityReport().getConstraintViolations().isEmpty()) {
                explanation.append("\n\n**Issues:**\n");
                for (FeasibilityReport.ConstraintViolation violation : result.getFeasibilityReport().getConstraintViolations()) {
                    explanation.append(String.format("- %s: %s\n", violation.getSeverity(), violation.getDescription()));
                }
            }

            if (!result.getFeasibilityReport().getRecommendations().isEmpty()) {
                explanation.append("\n**Recommendations:**\n");
                for (String rec : result.getFeasibilityReport().getRecommendations()) {
                    explanation.append(String.format("- %s\n", rec));
                }
            }
        }

        return explanation.toString();
    }

    /**
     * Deduces the service configuration type from the service name.
     */
    private ServiceConfigurationType deduceConfigurationType(String serviceType) {
        if (serviceType.toLowerCase().contains("forecast")) {
            return ServiceConfigurationType.FORECASTER_ONLY;
        } else if (serviceType.toLowerCase().contains("classif")) {
            return ServiceConfigurationType.CLASSIFICATOR_ONLY;
        } else if (serviceType.toLowerCase().contains("slice")) {
            return ServiceConfigurationType.SLICER_ONLY;
        } else if (serviceType.toLowerCase().contains("full") || serviceType.toLowerCase().contains("complete")) {
            return ServiceConfigurationType.FULL_PIPELINE;
        }
        return ServiceConfigurationType.FULL_PIPELINE;  // Default
    }

    /**
     * Deduces the optimization goal from the natural language request.
     */
    private OptimizationGoal deduceOptimizationGoal(String request) {
        String lower = request.toLowerCase();

        if (lower.contains("maximize service") || lower.contains("most services")) {
            return OptimizationGoal.MAXIMIZE_SERVICES;
        } else if (lower.contains("minimize resource") || lower.contains("save resource")) {
            return OptimizationGoal.MINIMIZE_RESOURCES;
        } else if (lower.contains("best quality") || lower.contains("highest quality")) {
            return OptimizationGoal.MAXIMIZE_QUALITY;
        } else if (lower.contains("low latency") || lower.contains("fastest")) {
            return OptimizationGoal.MINIMIZE_LATENCY;
        } else if (lower.contains("balance") || lower.contains("balanced")) {
            return OptimizationGoal.BALANCED;
        }

        return OptimizationGoal.MAXIMIZE_SERVICES;  // Default
    }
}
