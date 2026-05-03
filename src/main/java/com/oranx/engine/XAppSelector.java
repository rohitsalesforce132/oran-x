package com.oranx.engine;

import com.oranx.model.RANFunction;
import com.oranx.model.XApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Selects the optimal xApp for a given RAN function based on quality, resource, and latency requirements.
 */
public class XAppSelector {

    private static final Logger LOG = LoggerFactory.getLogger(XAppSelector.class);

    /**
     * Selects the best xApp from a list for a specific function.
     * Considers quality score, resource requirements, and latency (theta).
     */
    public XApp selectBestXApp(List<XApp> xApps, double minQuality, double maxLatencyMs) {
        if (xApps == null || xApps.isEmpty()) {
            return null;
        }

        // Filter xApps that meet constraints
        List<XApp> feasible = xApps.stream()
            .filter(x -> x.getQualityScore() >= minQuality)
            .filter(x -> x.getTheta() <= maxLatencyMs)
            .toList();

        if (feasible.isEmpty()) {
            LOG.warn("No xApp meets quality={} and latency={} constraints", minQuality, maxLatencyMs);
            // Return the best we can do
            return selectBestByQuality(xApps);
        }

        // Score each feasible xApp
        XApp best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (XApp xApp : feasible) {
            double score = calculateScore(xApp, minQuality, maxLatencyMs);
            if (score > bestScore) {
                bestScore = score;
                best = xApp;
            }
        }

        LOG.debug("Selected xApp: {} with score={}", best, bestScore);
        return best;
    }

    /**
     * Calculates a composite score for an xApp.
     * Higher quality and lower resources are better.
     */
    private double calculateScore(XApp xApp, double minQuality, double maxLatencyMs) {
        // Quality score (normalized)
        double qualityScore = xApp.getQualityScore();

        // Resource cost (lower is better)
        double resourceCost = xApp.getCpuCores() * 10 + xApp.getMemoryGB() * 5 + xApp.getDiskGB();

        // Latency cost (lower is better)
        double latencyCost = xApp.getTheta();

        // Composite score: prioritize quality, penalize resources and latency
        return qualityScore * 100 - resourceCost - latencyCost * 10;
    }

    /**
     * Selects the best xApp by quality score when no constraints are met.
     */
    private XApp selectBestByQuality(List<XApp> xApps) {
        return xApps.stream()
            .max(Comparator.comparingDouble(XApp::getQualityScore))
            .orElse(null);
    }

    /**
     * Selects the most resource-efficient xApp (minimum resource usage).
     */
    public XApp selectMostEfficient(List<XApp> xApps) {
        if (xApps == null || xApps.isEmpty()) {
            return null;
        }

        return xApps.stream()
            .min(Comparator.comparingDouble(this::getResourceCost))
            .orElse(null);
    }

    /**
     * Calculates resource cost for an xApp.
     */
    private double getResourceCost(XApp xApp) {
        return xApp.getCpuCores() * 10 + xApp.getMemoryGB() * 5 + xApp.getDiskGB();
    }

    /**
     * Selects xApps that can be shared across services (semantic equivalence).
     * Two xApps are semantically equivalent if they implement the same function
     * and have compatible quality/latency profiles.
     */
    public List<XApp> selectEquivalentXApps(XApp xApp, List<XApp> candidates) {
        if (xApp == null || candidates == null) {
            return Collections.emptyList();
        }

        return candidates.stream()
            .filter(x -> x.getFunction() == xApp.getFunction())
            .filter(x -> !x.equals(xApp))
            .filter(x -> Math.abs(x.getQualityScore() - xApp.getQualityScore()) < 0.1)
            .filter(x -> Math.abs(x.getTheta() - xApp.getTheta()) < 1.0)
            .toList();
    }
}
