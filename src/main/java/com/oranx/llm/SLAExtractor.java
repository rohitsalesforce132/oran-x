package com.oranx.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts SLA parameters from natural language requests using LLM.
 */
public class SLAExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(SLAExtractor.class);

    private final ChatLanguageModel chatModel;

    // Regex patterns for common SLA expressions
    private static final Pattern QUALITY_PATTERN = Pattern.compile(
        "(?:quality|accuracy|precision)[:\\s]*(\\d+(?:\\.\\d+)?)%?",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LATENCY_PATTERN = Pattern.compile(
        "(?:latency|delay)[:\\s]*(\\d+(?:\\.\\d+)?)\\s*(?:ms|millisecond)?",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MEMORY_PATTERN = Pattern.compile(
        "(?:memory|ram)[:\\s]*(\\d+(?:\\.\\d+)?)\\s*(?:gb|gigabyte)?",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CPU_PATTERN = Pattern.compile(
        "(?:cpu|core)[:\\s]*(\\d+(?:\\.\\d+)?)\\s*(?:core)?",
        Pattern.CASE_INSENSITIVE
    );

    public SLAExtractor(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Extracts SLA parameters from natural language text.
     */
    public SLAParameters extract(String text) {
        LOG.debug("Extracting SLA from: {}", text);

        SLAParameters sla = new SLAParameters();

        // Try regex extraction first (faster, more reliable for common patterns)
        extractWithRegex(text, sla);

        // If regex didn't find everything, fall back to LLM
        if (sla.getServiceType() == null || sla.getQualityTarget() == 0) {
            extractWithLLM(text, sla);
        }

        // Set defaults if not found
        if (sla.getServiceType() == null) {
            sla.setServiceType("traffic_forecasting");
        }
        if (sla.getQualityTarget() == 0) {
            sla.setQualityTarget(0.95);  // 95% default
        }
        if (sla.getLatencyTargetMs() == 0) {
            sla.setLatencyTargetMs(5.0);  // 5ms default
        }

        return sla;
    }

    /**
     * Extracts parameters using regex patterns.
     */
    private void extractWithRegex(String text, SLAParameters sla) {
        // Extract service type
        if (text.toLowerCase().contains("forecast")) {
            sla.setServiceType("traffic_forecasting");
        } else if (text.toLowerCase().contains("classif")) {
            sla.setServiceType("traffic_classification");
        } else if (text.toLowerCase().contains("slice")) {
            sla.setServiceType("network_slicing");
        }

        // Extract quality
        Matcher qualityMatcher = QUALITY_PATTERN.matcher(text);
        if (qualityMatcher.find()) {
            double quality = Double.parseDouble(qualityMatcher.group(1));
            sla.setQualityTarget(quality / 100.0);  // Convert percentage to 0-1
        }

        // Extract latency
        Matcher latencyMatcher = LATENCY_PATTERN.matcher(text);
        if (latencyMatcher.find()) {
            sla.setLatencyTargetMs(Double.parseDouble(latencyMatcher.group(1)));
        }

        // Extract memory
        Matcher memoryMatcher = MEMORY_PATTERN.matcher(text);
        if (memoryMatcher.find()) {
            sla.setMemoryGB(Double.parseDouble(memoryMatcher.group(1)));
        }

        // Extract CPU
        Matcher cpuMatcher = CPU_PATTERN.matcher(text);
        if (cpuMatcher.find()) {
            sla.setCpuCores(Double.parseDouble(cpuMatcher.group(1)));
        }
    }

    /**
     * Extracts parameters using LLM for more complex requests.
     */
    private void extractWithLLM(String text, SLAParameters sla) {
        String prompt = String.format(
            "Extract the following information from this request and return as JSON:\n" +
            "Request: %s\n\n" +
            "Return JSON with these fields:\n" +
            "- service_type: one of [traffic_forecasting, traffic_classification, network_slicing]\n" +
            "- quality_target: number 0-1 (e.g., 0.95 for 95%%)\n" +
            "- latency_target_ms: number in milliseconds\n" +
            "- cpu_cores: number of CPU cores (or 0 if not specified)\n" +
            "- memory_gb: memory in GB (or 0 if not specified)\n" +
            "- disk_gb: disk in GB (or 0 if not specified)\n\n" +
            "Return ONLY valid JSON, no other text.",
            text
        );

        try {
            String response = chatModel.generate(prompt);
            LOG.debug("LLM response: {}", response);

            // Parse JSON response (simplified - in production use proper JSON parser)
            if (response.contains("service_type")) {
                parseSLAFromJson(response, sla);
            }
        } catch (Exception e) {
            LOG.warn("LLM extraction failed, falling back to defaults: {}", e.getMessage());
        }
    }

    /**
     * Parses SLA parameters from JSON string.
     */
    private void parseSLAFromJson(String json, SLAParameters sla) {
        try {
            // Simple JSON parsing for the expected fields
            if (json.contains("\"service_type\"")) {
                String serviceType = extractJsonValue(json, "service_type");
                if (serviceType != null) sla.setServiceType(serviceType);
            }

            if (json.contains("\"quality_target\"")) {
                Double quality = extractJsonDouble(json, "quality_target");
                if (quality != null) sla.setQualityTarget(quality);
            }

            if (json.contains("\"latency_target_ms\"")) {
                Double latency = extractJsonDouble(json, "latency_target_ms");
                if (latency != null) sla.setLatencyTargetMs(latency);
            }

            if (json.contains("\"cpu_cores\"")) {
                Double cpu = extractJsonDouble(json, "cpu_cores");
                if (cpu != null) sla.setCpuCores(cpu);
            }

            if (json.contains("\"memory_gb\"")) {
                Double memory = extractJsonDouble(json, "memory_gb");
                if (memory != null) sla.setMemoryGB(memory);
            }

            if (json.contains("\"disk_gb\"")) {
                Double disk = extractJsonDouble(json, "disk_gb");
                if (disk != null) sla.setDiskGB(disk);
            }

        } catch (Exception e) {
            LOG.warn("Failed to parse SLA JSON: {}", e.getMessage());
        }
    }

    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Double extractJsonDouble(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+\\.?\\d*)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : null;
    }

    /**
     * Data class for SLA parameters.
     */
    public static class SLAParameters {
        private String serviceType;
        private double qualityTarget;
        private double latencyTargetMs;
        private double cpuCores;
        private double memoryGB;
        private double diskGB;

        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }

        public double getQualityTarget() { return qualityTarget; }
        public void setQualityTarget(double qualityTarget) { this.qualityTarget = qualityTarget; }

        public double getLatencyTargetMs() { return latencyTargetMs; }
        public void setLatencyTargetMs(double latencyTargetMs) { this.latencyTargetMs = latencyTargetMs; }

        public double getCpuCores() { return cpuCores; }
        public void setCpuCores(double cpuCores) { this.cpuCores = cpuCores; }

        public double getMemoryGB() { return memoryGB; }
        public void setMemoryGB(double memoryGB) { this.memoryGB = memoryGB; }

        public double getDiskGB() { return diskGB; }
        public void setDiskGB(double diskGB) { this.diskGB = diskGB; }

        @Override
        public String toString() {
            return String.format("SLA[service=%s, Q=%.2f, L=%.2fms, cpu=%.1f, mem=%.1fGB]",
                serviceType, qualityTarget, latencyTargetMs, cpuCores, memoryGB);
        }
    }
}
