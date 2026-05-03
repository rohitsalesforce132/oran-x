package com.oranx.llm;

import com.oranx.model.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates human-readable explanations of deployment plans.
 * Provides insights into why specific xApps were selected and trade-offs made.
 */
public class DeploymentExplainer {

    private static final Logger LOG = LoggerFactory.getLogger(DeploymentExplainer.class);

    private final ChatLanguageModel chatModel;

    public DeploymentExplainer() {
        String apiKey = System.getenv().getOrDefault("OPENAI_API_KEY", "dummy-key");
        this.chatModel = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4")
            .temperature(0.3)
            .build();
    }

    /**
     * Generates a natural language explanation of the orchestration result.
     */
    public String explain(OrchestrationResult result, OrchestrationRequest request) {
        LOG.info("Generating explanation for result: {}", result.getRequestId());

        StringBuilder explanation = new StringBuilder();

        // Executive summary
        explanation.append("## Orchestration Summary\n\n");
        explanation.append(generateExecutiveSummary(result, request));

        // Detailed analysis
        explanation.append("\n## Detailed Analysis\n\n");
        explanation.append(generateDetailedAnalysis(result, request));

        // Recommendations
        explanation.append("\n## Recommendations\n\n");
        explanation.append(generateRecommendations(result, request));

        return explanation.toString();
    }

    /**
     * Generates an executive summary of the orchestration.
     */
    private String generateExecutiveSummary(OrchestrationResult result, OrchestrationRequest request) {
        StringBuilder summary = new StringBuilder();

        int deployed = result.getTotalServicesDeployed();
        int requested = result.getTotalServicesRequested();
        double successRate = requested > 0 ? (double) deployed / requested * 100 : 0;

        summary.append(String.format("Successfully deployed **%d of %d** services (%.1f%% success rate).\n\n",
            deployed, requested, successRate));

        summary.append("**Resource Utilization:**\n");
        summary.append(String.format("- CPU: %.2f cores used\n", result.getTotalCpuUsed()));
        summary.append(String.format("- Memory: %.2f GB used\n", result.getTotalMemoryUsedGB()));
        summary.append(String.format("- Disk: %.2f GB used\n\n", result.getTotalDiskUsedGB()));

        summary.append("**Performance:**\n");
        summary.append(String.format("- Average quality: %.1f%%\n", result.getAverageQuality() * 100));
        summary.append(String.format("- Average latency: %.2f ms\n\n", result.getAverageLatencyMs()));

        summary.append("**Status:** ");
        switch (result.getStatus()) {
            case SUCCESS:
                summary.append("✅ All services deployed successfully");
                break;
            case PARTIAL_SUCCESS:
                summary.append("⚠️ Partial success - some services could not be deployed");
                break;
            case FAILURE:
                summary.append("❌ Orchestration failed");
                break;
            default:
                summary.append("🔄 In progress");
        }

        return summary.toString();
    }

    /**
     * Generates detailed analysis of the deployment.
     */
    private String generateDetailedAnalysis(OrchestrationResult result, OrchestrationRequest request) {
        StringBuilder analysis = new StringBuilder();

        // Service-level breakdown
        analysis.append("### Service Breakdown\n\n");

        for (ServiceConfiguration config : result.getDeployedConfigurations()) {
            analysis.append(String.format("**Service %s**\n", config.getServiceId()));
            analysis.append(String.format("- Configuration: %s\n", config.getConfigurationType().getName()));
            analysis.append(String.format("- Quality achieved: %.1f%% (target: %.1f%%)\n",
                config.getTotalQuality() * 100, request.getQualityThreshold() * 100));
            analysis.append(String.format("- Latency achieved: %.2f ms (target: %.2f ms)\n",
                config.getTotalLatencyMs(), request.getLatencyThreshold()));
            analysis.append(String.format("- Resource cost: %.2f\n\n", config.getCost()));

            analysis.append("**xApps deployed:**\n");
            for (Map.Entry<RANFunction, XApp> entry : config.getXAppAssignments().entrySet()) {
                XApp xApp = entry.getValue();
                analysis.append(String.format("- **%s**: %s\n", entry.getKey().getName(), xApp.getName()));
                analysis.append(String.format("  - Quality: %.1f%%, Latency: %.2f ms\n",
                    xApp.getQualityScore() * 100, xApp.getTheta()));
                analysis.append(String.format("  - Resources: %.1f CPU, %.1f GB RAM, %.1f GB disk\n",
                    xApp.getCpuCores(), xApp.getMemoryGB(), xApp.getDiskGB()));
            }
            analysis.append("\n");
        }

        // Failed services analysis
        if (!result.getFailedServices().isEmpty()) {
            analysis.append("### Failed Services\n\n");
            for (Service service : result.getFailedServices()) {
                analysis.append(String.format("- **%s** (priority=%.2f)\n", service.getName(), service.getPriority()));
                analysis.append(String.format("  Required: Q=%.1f%%, L=%.2f ms\n\n",
                    service.getQualityTarget() * 100, service.getLatencyTargetMs()));
            }
        }

        return analysis.toString();
    }

    /**
     * Generates actionable recommendations.
     */
    private String generateRecommendations(OrchestrationResult result, OrchestrationRequest request) {
        StringBuilder recommendations = new StringBuilder();

        if (result.getFeasibilityReport() == null || result.getFeasibilityReport().isFeasible()) {
            recommendations.append("✅ All constraints met. The deployment is feasible and ready for activation.\n\n");
        } else {
            recommendations.append("### Feasibility Issues\n\n");

            for (FeasibilityReport.ConstraintViolation violation : result.getFeasibilityReport().getConstraintViolations()) {
                recommendations.append(String.format("- **%s**: %s\n",
                    violation.getSeverity(), violation.getDescription()));
            }
            recommendations.append("\n");
        }

        // Optimization suggestions
        if (result.getDeploymentRate() < 1.0) {
            recommendations.append("### Optimization Suggestions\n\n");
            recommendations.append("- Consider increasing resource budget to deploy more services\n");
            recommendations.append("- Review service priorities and SLA requirements\n");
            recommendations.append("- Enable semantic equivalence sharing to reduce resource usage\n");

            if (!request.isSharedXAppsAllowed()) {
                recommendations.append("- **Enable xApp sharing** to allow multiple services to share the same xApp instance\n");
            }
        }

        // Performance optimization
        if (result.getAverageLatencyMs() > request.getLatencyThreshold() * 0.8) {
            recommendations.append("\n### Latency Optimization\n\n");
            recommendations.append("- Average latency is close to threshold. Consider:\n");
            recommendations.append("  - Selecting xApps with lower theta values\n");
            recommendations.append("  - Using simpler service configurations\n");
            recommendations.append("  - Deploying on higher-performance nodes\n");
        }

        return recommendations.toString();
    }

    /**
     * Generates a comparison between two orchestration results.
     */
    public String compareResults(OrchestrationResult result1, OrchestrationResult result2, String label1, String label2) {
        StringBuilder comparison = new StringBuilder();

        comparison.append("## Orchestration Comparison\n\n");
        comparison.append(String.format("| Metric | %s | %s | Difference |\n", label1, label2));
        comparison.append("|--------|-----|-----|------------|\n");

        comparison.append(String.format("| Services Deployed | %d | %d | %+d |\n",
            result1.getTotalServicesDeployed(),
            result2.getTotalServicesDeployed(),
            result2.getTotalServicesDeployed() - result1.getTotalServicesDeployed()));

        comparison.append(String.format("| CPU Used | %.2f | %.2f | %+.2f |\n",
            result1.getTotalCpuUsed(), result2.getTotalCpuUsed(),
            result2.getTotalCpuUsed() - result1.getTotalCpuUsed()));

        comparison.append(String.format("| Memory Used | %.2f GB | %.2f GB | %+.2f GB |\n",
            result1.getTotalMemoryUsedGB(), result2.getTotalMemoryUsedGB(),
            result2.getTotalMemoryUsedGB() - result1.getTotalMemoryUsedGB()));

        comparison.append(String.format("| Average Quality | %.1f%% | %.1f%% | %+.1f%% |\n",
            result1.getAverageQuality() * 100, result2.getAverageQuality() * 100,
            (result2.getAverageQuality() - result1.getAverageQuality()) * 100));

        comparison.append(String.format("| Average Latency | %.2f ms | %.2f ms | %+.2f ms |\n",
            result1.getAverageLatencyMs(), result2.getAverageLatencyMs(),
            result2.getAverageLatencyMs() - result1.getAverageLatencyMs()));

        return comparison.toString();
    }
}
