package com.oranx.llm;

import com.oranx.model.OrchestrationRequest;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recommends xApp configurations based on natural language preferences and requirements.
 */
public class XAppRecommender {

    private static final Logger LOG = LoggerFactory.getLogger(XAppRecommender.class);

    private final ChatLanguageModel chatModel;

    public XAppRecommender(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Recommends xApp configurations for the given request.
     */
    public XAppRecommendation recommend(String naturalLanguageRequest, OrchestrationRequest request) {
        LOG.info("Generating xApp recommendations");

        XAppRecommendation recommendation = new XAppRecommendation();

        // Analyze the request to determine preferences
        String preferences = analyzePreferences(naturalLanguageRequest);

        // Generate recommendations based on preferences
        recommendation.setStrategy(deduceStrategy(preferences));
        recommendation.setReasoning(explainStrategy(preferences, request));

        // Set preference flags
        recommendation.setPreferHighQuality(preferences.toLowerCase().contains("quality") ||
                                            preferences.toLowerCase().contains("accuracy"));
        recommendation.setPreferLowLatency(preferences.toLowerCase().contains("latency") ||
                                           preferences.toLowerCase().contains("fast") ||
                                           preferences.toLowerCase().contains("low latency"));
        recommendation.setPreferEfficiency(preferences.toLowerCase().contains("efficient") ||
                                          preferences.toLowerCase().contains("save") ||
                                          preferences.toLowerCase().contains("budget"));

        LOG.info("Recommendation: strategy={}, quality={}, latency={}",
            recommendation.getStrategy(), recommendation.isPreferHighQuality(), recommendation.isPreferLowLatency());

        return recommendation;
    }

    /**
     * Analyzes the natural language request to extract user preferences.
     */
    private String analyzePreferences(String request) {
        String prompt = String.format(
            "Analyze this xApp orchestration request and identify the user's priorities:\n" +
            "Request: %s\n\n" +
            "Identify:\n" +
            "- Quality priority: Do they care about accuracy/quality?\n" +
            "- Latency priority: Do they need low latency/fast processing?\n" +
            "- Resource efficiency: Do they care about saving CPU/memory?\n" +
            "- Budget constraints: Are there strict resource limits?\n\n" +
            "Return a brief summary of priorities.",
            request
        );

        try {
            return chatModel.generate(prompt);
        } catch (Exception e) {
            LOG.warn("LLM analysis failed, using defaults: {}", e.getMessage());
            return "Balanced priorities";
        }
    }

    /**
     * Deduces the selection strategy from preferences.
     */
    private SelectionStrategy deduceStrategy(String preferences) {
        String lower = preferences.toLowerCase();

        if (lower.contains("quality") || lower.contains("accuracy")) {
            return SelectionStrategy.QUALITY_FIRST;
        } else if (lower.contains("latency") || lower.contains("fast")) {
            return SelectionStrategy.LATENCY_FIRST;
        } else if (lower.contains("efficient") || lower.contains("budget") || lower.contains("save")) {
            return SelectionStrategy.RESOURCE_EFFICIENT;
        } else {
            return SelectionStrategy.BALANCED;
        }
    }

    /**
     * Explains the recommended strategy.
     */
    private String explainStrategy(String preferences, OrchestrationRequest request) {
        StringBuilder explanation = new StringBuilder();

        explanation.append("Based on your requirements, I recommend the following approach:\n\n");

        if (preferences.toLowerCase().contains("quality")) {
            explanation.append("- **Prioritize quality**: Select xApps with highest accuracy scores\n");
            explanation.append("- Trade-off: May use more resources\n");
        } else if (preferences.toLowerCase().contains("latency")) {
            explanation.append("- **Prioritize speed**: Select xApps with lowest theta (processing time)\n");
            explanation.append("- Trade-off: May sacrifice some accuracy\n");
        } else if (preferences.toLowerCase().contains("efficient") || preferences.toLowerCase().contains("budget")) {
            explanation.append("- **Prioritize efficiency**: Select xApps with lowest resource requirements\n");
            explanation.append("- Trade-off: May have lower quality or higher latency\n");
        } else {
            explanation.append("- **Balanced approach**: Optimize for the best combination of quality, latency, and resources\n");
        }

        // Add context about constraints
        explanation.append(String.format("\nYour constraints:\n"));
        explanation.append(String.format("- Quality threshold: %.1f%%\n", request.getQualityThreshold() * 100));
        explanation.append(String.format("- Latency threshold: %.2f ms\n", request.getLatencyThreshold()));
        if (request.getResourceBudget() != null) {
            explanation.append(String.format("- Resource budget: CPU=%.1f, Memory=%.1fGB, Disk=%.1fGB\n",
                request.getResourceBudget().getCpuCores(),
                request.getResourceBudget().getMemoryGB(),
                request.getResourceBudget().getDiskGB()));
        }

        return explanation.toString();
    }

    /**
     * Selection strategy for xApp selection.
     */
    public enum SelectionStrategy {
        QUALITY_FIRST,
        LATENCY_FIRST,
        RESOURCE_EFFICIENT,
        BALANCED
    }

    /**
     * Recommendation data class.
     */
    public static class XAppRecommendation {
        private SelectionStrategy strategy;
        private String reasoning;
        private boolean preferHighQuality;
        private boolean preferLowLatency;
        private boolean preferEfficiency;

        public SelectionStrategy getStrategy() { return strategy; }
        public void setStrategy(SelectionStrategy strategy) { this.strategy = strategy; }

        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }

        public boolean isPreferHighQuality() { return preferHighQuality; }
        public void setPreferHighQuality(boolean preferHighQuality) { this.preferHighQuality = preferHighQuality; }

        public boolean isPreferLowLatency() { return preferLowLatency; }
        public void setPreferLowLatency(boolean preferLowLatency) { this.preferLowLatency = preferLowLatency; }

        public boolean isPreferEfficiency() { return preferEfficiency; }
        public void setPreferEfficiency(boolean preferEfficiency) { this.preferEfficiency = preferEfficiency; }

        @Override
        public String toString() {
            return String.format("XAppRecommendation[strategy=%s, quality=%s, latency=%s, efficient=%s]",
                strategy, preferHighQuality, preferLowLatency, preferEfficiency);
        }
    }
}
