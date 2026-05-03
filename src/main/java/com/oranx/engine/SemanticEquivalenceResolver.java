package com.oranx.engine;

import com.oranx.model.RANFunction;
import com.oranx.model.XApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Determines which xApps can be shared across services based on semantic equivalence.
 * Semantic equivalence allows multiple services to share the same xApp instance
 * when they implement the same function with compatible quality/latency profiles.
 */
public class SemanticEquivalenceResolver {

    private static final Logger LOG = LoggerFactory.getLogger(SemanticEquivalenceResolver.class);

    // Tolerance for considering two xApps semantically equivalent
    private static final double QUALITY_TOLERANCE = 0.1;  // 10% quality difference
    private static final double LATENCY_TOLERANCE = 1.0;  // 1ms latency difference

    /**
     * Groups xApps into semantic equivalence classes.
     * xApps in the same class can be shared across services.
     */
    public Map<String, List<XApp>> resolveEquivalenceClasses(List<XApp> xApps) {
        Map<String, List<XApp>> classes = new HashMap<>();
        List<XApp> unassigned = new ArrayList<>(xApps);

        int classId = 0;
        while (!unassigned.isEmpty()) {
            XApp seed = unassigned.get(0);
            List<XApp> equivalent = new ArrayList<>();
            equivalent.add(seed);

            // Find all xApps equivalent to the seed
            Iterator<XApp> iter = unassigned.iterator();
            while (iter.hasNext()) {
                XApp candidate = iter.next();
                if (candidate != seed && areEquivalent(seed, candidate)) {
                    equivalent.add(candidate);
                    iter.remove();
                }
            }

            String className = String.format("class-%s-%d",
                seed.getFunction().getName(), classId++);
            classes.put(className, equivalent);

            unassigned.remove(seed);
        }

        LOG.debug("Resolved {} equivalence classes", classes.size());
        return classes;
    }

    /**
     * Determines if two xApps are semantically equivalent.
     */
    public boolean areEquivalent(XApp x1, XApp x2) {
        // Must implement the same function
        if (x1.getFunction() != x2.getFunction()) {
            return false;
        }

        // Quality scores must be similar
        double qualityDiff = Math.abs(x1.getQualityScore() - x2.getQualityScore());
        if (qualityDiff > QUALITY_TOLERANCE) {
            return false;
        }

        // Latency (theta) must be similar
        double latencyDiff = Math.abs(x1.getTheta() - x2.getTheta());
        if (latencyDiff > LATENCY_TOLERANCE) {
            return false;
        }

        return true;
    }

    /**
     * Finds the best shared xApp for a set of services.
     * Returns an xApp that can satisfy all services in the set.
     */
    public XApp findBestSharedXApp(List<XApp> candidates, double minQuality, double maxLatency) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // Filter candidates that meet all constraints
        List<XApp> feasible = candidates.stream()
            .filter(x -> x.getQualityScore() >= minQuality)
            .filter(x -> x.getTheta() <= maxLatency)
            .toList();

        if (feasible.isEmpty()) {
            return null;
        }

        // Select the one with highest quality
        return feasible.stream()
            .max(Comparator.comparingDouble(XApp::getQualityScore))
            .orElse(null);
    }

    /**
     * Builds a sharing plan: maps each xApp to the list of services that will use it.
     */
    public Map<XApp, List<String>> buildSharingPlan(
            Map<String, List<XApp>> serviceXAppOptions,
            boolean allowSharing) {

        Map<XApp, List<String>> sharingPlan = new HashMap<>();

        if (!allowSharing) {
            // No sharing: each service gets its own xApp instances
            for (Map.Entry<String, List<XApp>> entry : serviceXAppOptions.entrySet()) {
                String serviceId = entry.getKey();
                for (XApp xApp : entry.getValue()) {
                    sharingPlan.computeIfAbsent(xApp, k -> new ArrayList<>()).add(serviceId);
                }
            }
            return sharingPlan;
        }

        // With sharing: group semantically equivalent xApps
        List<XApp> allXApps = serviceXAppOptions.values().stream()
            .flatMap(List::stream)
            .distinct()
            .toList();

        Map<String, List<XApp>> equivalenceClasses = resolveEquivalenceClasses(allXApps);

        // For each equivalence class, select the best xApp to share
        for (List<XApp> eqClass : equivalenceClasses.values()) {
            XApp best = eqClass.stream()
                .max(Comparator.comparingDouble(XApp::getQualityScore))
                .orElse(null);

            if (best != null) {
                // Find all services that can use this xApp
                List<String> servicesUsing = new ArrayList<>();
                for (Map.Entry<String, List<XApp>> entry : serviceXAppOptions.entrySet()) {
                    String serviceId = entry.getKey();
                    for (XApp option : entry.getValue()) {
                        if (areEquivalent(best, option)) {
                            servicesUsing.add(serviceId);
                            break;
                        }
                    }
                }
                sharingPlan.put(best, servicesUsing);
            }
        }

        return sharingPlan;
    }

    /**
     * Calculates the resource savings from semantic equivalence sharing.
     */
    public double calculateSavings(
            Map<XApp, List<String>> sharingPlan,
            double perXAppCost) {

        int totalServiceUsages = sharingPlan.values().stream()
            .mapToInt(List::size)
            .sum();

        int sharedXApps = sharingPlan.size();

        // Savings = (usages without sharing) - (actual shared xApps)
        int savings = totalServiceUsages - sharedXApps;

        return savings * perXAppCost;
    }
}
