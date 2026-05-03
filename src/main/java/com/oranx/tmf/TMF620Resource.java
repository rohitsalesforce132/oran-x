package com.oranx.tmf;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * TMF620: Product Catalog API
 * Exposes available xApp services and their characteristics as a product catalog.
 *
 * TMF620 defines APIs for:
 * - Querying product catalog
 * - Managing product offerings
 * - Product specification and characteristics
 */
@Path("/tmf-api/productCatalogManagement/v4")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TMF620Resource {

    private static final Logger LOG = LoggerFactory.getLogger(TMF620Resource.class);

    // In-memory catalog storage
    private static final Map<String, ProductCatalog> catalogs = new HashMap<>();
    private static final Map<String, ProductOffering> offerings = new HashMap<>();
    private static final Map<String, ProductSpecification> specifications = new HashMap<>();

    static {
        // Initialize sample catalog
        initializeCatalog();
    }

    /**
     * List all product catalogs.
     * GET /productCatalog
     */
    @GET
    @Path("/productCatalog")
    public Response listCatalogs() {
        LOG.info("Listing product catalogs");
        return Response.ok(new ArrayList<>(catalogs.values())).build();
    }

    /**
     * Get a specific catalog by ID.
     * GET /productCatalog/{id}
     */
    @GET
    @Path("/productCatalog/{id}")
    public Response getCatalog(@PathParam("id") String id) {
        LOG.info("Getting catalog: {}", id);

        ProductCatalog catalog = catalogs.get(id);
        if (catalog == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("CATALOG_NOT_FOUND", "Catalog not found: " + id))
                .build();
        }

        return Response.ok(catalog).build();
    }

    /**
     * List product offerings.
     * GET /productOffering
     */
    @GET
    @Path("/productOffering")
    public Response listOfferings(
            @QueryParam("catalogId") String catalogId,
            @QueryParam("category") String category) {

        LOG.info("Listing product offerings: catalogId={}, category={}", catalogId, category);

        List<ProductOffering> filtered = new ArrayList<>(offerings.values());

        if (catalogId != null) {
            filtered = filtered.stream()
                .filter(o -> catalogId.equals(o.getCatalogId()))
                .toList();
        }

        if (category != null) {
            filtered = filtered.stream()
                .filter(o -> category.equals(o.getCategory()))
                .toList();
        }

        return Response.ok(filtered).build();
    }

    /**
     * Get a specific product offering.
     * GET /productOffering/{id}
     */
    @GET
    @Path("/productOffering/{id}")
    public Response getOffering(@PathParam("id") String id) {
        LOG.info("Getting product offering: {}", id);

        ProductOffering offering = offerings.get(id);
        if (offering == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("OFFERING_NOT_FOUND", "Offering not found: " + id))
                .build();
        }

        return Response.ok(offering).build();
    }

    /**
     * List product specifications.
     * GET /productSpecification
     */
    @GET
    @Path("/productSpecification")
    public Response listSpecifications() {
        LOG.info("Listing product specifications");
        return Response.ok(new ArrayList<>(specifications.values())).build();
    }

    /**
     * Get a specific product specification.
     * GET /productSpecification/{id}
     */
    @GET
    @Path("/productSpecification/{id}")
    public Response getSpecification(@PathParam("id") String id) {
        LOG.info("Getting product specification: {}", id);

        ProductSpecification spec = specifications.get(id);
        if (spec == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("SPEC_NOT_FOUND", "Specification not found: " + id))
                .build();
        }

        return Response.ok(spec).build();
    }

    /**
     * Search products by characteristics.
     * GET /findProduct
     */
    @GET
    @Path("/findProduct")
    public Response findProduct(
            @QueryParam("name") String name,
            @QueryParam("minQuality") Double minQuality,
            @QueryParam("maxLatency") Double maxLatency) {

        LOG.info("Finding products: name={}, minQuality={}, maxLatency={}",
            name, minQuality, maxLatency);

        List<ProductOffering> results = new ArrayList<>(offerings.values());

        // Filter by name
        if (name != null && !name.isEmpty()) {
            results = results.stream()
                .filter(o -> o.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
        }

        // Filter by characteristics
        if (minQuality != null || maxLatency != null) {
            results = results.stream()
                .filter(o -> matchesCharacteristics(o, minQuality, maxLatency))
                .toList();
        }

        return Response.ok(results).build();
    }

    private boolean matchesCharacteristics(ProductOffering offering, Double minQuality, Double maxLatency) {
        if (offering.getProductSpecCharacteristic() == null) {
            return true;
        }

        boolean qualityMatch = true;
        boolean latencyMatch = true;

        for (ProductSpecCharacteristic characteristic : offering.getProductSpecCharacteristic()) {
            if ("quality".equals(characteristic.getName()) && minQuality != null) {
                qualityMatch = characteristic.getDefaultValue() >= minQuality;
            }
            if ("latency".equals(characteristic.getName()) && maxLatency != null) {
                latencyMatch = characteristic.getDefaultValue() <= maxLatency;
            }
        }

        return qualityMatch && latencyMatch;
    }

    /**
     * Initialize the sample product catalog.
     */
    private static void initializeCatalog() {
        // Create main catalog
        ProductCatalog catalog = new ProductCatalog();
        catalog.setId("xapp-catalog-1");
        catalog.setName("O-RAN xApp Services Catalog");
        catalog.setDescription("Catalog of ML-powered xApps for 5G O-RAN networks");
        catalog.setLifecycleStatus("Active");
        catalog.setValidFor(new ValidFor());
        catalogs.put(catalog.getId(), catalog);

        // Create product specifications
        // Traffic Forecasting Service
        ProductSpecification forecastSpec = new ProductSpecification();
        forecastSpec.setId("spec-traffic-forecasting");
        forecastSpec.setName("Traffic Forecasting Service");
        forecastSpec.setDescription("AI-powered traffic prediction for 5G networks");
        forecastSpec.setLifecycleStatus("Active");
        forecastSpec.setProductSpecCharacteristic(Arrays.asList(
            new ProductSpecCharacteristic("quality", "Output quality score (0-1)", 0.95),
            new ProductSpecCharacteristic("latency", "Processing latency in ms", 1.5),
            new ProductSpecCharacteristic("cpu", "CPU cores required", 1.0),
            new ProductSpecCharacteristic("memory", "Memory required in GB", 3.0)
        ));
        specifications.put(forecastSpec.getId(), forecastSpec);

        // Traffic Classification Service
        ProductSpecification classifySpec = new ProductSpecification();
        classifySpec.setId("spec-traffic-classification");
        classifySpec.setName("Traffic Classification Service");
        classifySpec.setDescription("AI-powered traffic categorization");
        classifySpec.setLifecycleStatus("Active");
        classifySpec.setProductSpecCharacteristic(Arrays.asList(
            new ProductSpecCharacteristic("quality", "Output quality score (0-1)", 0.94),
            new ProductSpecCharacteristic("latency", "Processing latency in ms", 1.0),
            new ProductSpecCharacteristic("cpu", "CPU cores required", 1.0),
            new ProductSpecCharacteristic("memory", "Memory required in GB", 2.0)
        ));
        specifications.put(classifySpec.getId(), classifySpec);

        // Network Slicing Service
        ProductSpecification sliceSpec = new ProductSpecification();
        sliceSpec.setId("spec-network-slicing");
        sliceSpec.setName("Network Slicing Service");
        sliceSpec.setDescription("Intelligent network slice management");
        sliceSpec.setLifecycleStatus("Active");
        sliceSpec.setProductSpecCharacteristic(Arrays.asList(
            new ProductSpecCharacteristic("quality", "Output quality score (0-1)", 0.96),
            new ProductSpecCharacteristic("latency", "Processing latency in ms", 1.2),
            new ProductSpecCharacteristic("cpu", "CPU cores required", 1.2),
            new ProductSpecCharacteristic("memory", "Memory required in GB", 2.5)
        ));
        specifications.put(sliceSpec.getId(), sliceSpec);

        // Create product offerings
        ProductOffering forecastOffering = new ProductOffering();
        forecastOffering.setId("offering-traffic-forecasting");
        forecastOffering.setName("Traffic Forecasting");
        forecastOffering.setDescription("Deploy AI-powered traffic forecasting xApps");
        forecastOffering.setCatalogId(catalog.getId());
        forecastOffering.setProductSpecificationId(forecastSpec.getId());
        forecastOffering.setCategory("RAN Intelligence");
        forecastOffering.setLifecycleStatus("Active");
        forecastOffering.setProductSpecCharacteristic(forecastSpec.getProductSpecCharacteristic());
        offerings.put(forecastOffering.getId(), forecastOffering);

        ProductOffering classifyOffering = new ProductOffering();
        classifyOffering.setId("offering-traffic-classification");
        classifyOffering.setName("Traffic Classification");
        classifyOffering.setDescription("Deploy AI-powered traffic classification xApps");
        classifyOffering.setCatalogId(catalog.getId());
        classifyOffering.setProductSpecificationId(classifySpec.getId());
        classifyOffering.setCategory("RAN Intelligence");
        classifyOffering.setLifecycleStatus("Active");
        classifyOffering.setProductSpecCharacteristic(classifySpec.getProductSpecCharacteristic());
        offerings.put(classifyOffering.getId(), classifyOffering);

        ProductOffering sliceOffering = new ProductOffering();
        sliceOffering.setId("offering-network-slicing");
        sliceOffering.setName("Network Slicing");
        sliceOffering.setDescription("Deploy intelligent network slicing xApps");
        sliceOffering.setCatalogId(catalog.getId());
        sliceOffering.setProductSpecificationId(sliceSpec.getId());
        sliceOffering.setCategory("RAN Intelligence");
        sliceOffering.setLifecycleStatus("Active");
        sliceOffering.setProductSpecCharacteristic(sliceSpec.getProductSpecCharacteristic());
        offerings.put(sliceOffering.getId(), sliceOffering);
    }

    private ErrorResponse createErrorResponse(String code, String message) {
        ErrorResponse error = new ErrorResponse();
        error.setCode(code);
        error.setMessage(message);
        return error;
    }

    // TMF Data Models
    public static class ProductCatalog {
        private String id;
        private String name;
        private String description;
        private String lifecycleStatus;
        private ValidFor validFor;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getLifecycleStatus() { return lifecycleStatus; }
        public void setLifecycleStatus(String lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }
        public ValidFor getValidFor() { return validFor; }
        public void setValidFor(ValidFor validFor) { this.validFor = validFor; }
    }

    public static class ProductOffering {
        private String id;
        private String name;
        private String description;
        private String catalogId;
        private String productSpecificationId;
        private String category;
        private String lifecycleStatus;
        private List<ProductSpecCharacteristic> productSpecCharacteristic;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCatalogId() { return catalogId; }
        public void setCatalogId(String catalogId) { this.catalogId = catalogId; }
        public String getProductSpecificationId() { return productSpecificationId; }
        public void setProductSpecificationId(String productSpecificationId) { this.productSpecificationId = productSpecificationId; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getLifecycleStatus() { return lifecycleStatus; }
        public void setLifecycleStatus(String lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }
        public List<ProductSpecCharacteristic> getProductSpecCharacteristic() { return productSpecCharacteristic; }
        public void setProductSpecCharacteristic(List<ProductSpecCharacteristic> productSpecCharacteristic) { this.productSpecCharacteristic = productSpecCharacteristic; }
    }

    public static class ProductSpecification {
        private String id;
        private String name;
        private String description;
        private String lifecycleStatus;
        private List<ProductSpecCharacteristic> productSpecCharacteristic;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getLifecycleStatus() { return lifecycleStatus; }
        public void setLifecycleStatus(String lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }
        public List<ProductSpecCharacteristic> getProductSpecCharacteristic() { return productSpecCharacteristic; }
        public void setProductSpecCharacteristic(List<ProductSpecCharacteristic> productSpecCharacteristic) { this.productSpecCharacteristic = productSpecCharacteristic; }
    }

    public static class ProductSpecCharacteristic {
        private String name;
        private String description;
        private Double defaultValue;

        public ProductSpecCharacteristic() {}
        public ProductSpecCharacteristic(String name, String description, double defaultValue) {
            this.name = name;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Double getDefaultValue() { return defaultValue; }
        public void setDefaultValue(Double defaultValue) { this.defaultValue = defaultValue; }
    }

    public static class ValidFor {
        private String startDateTime;
        private String endDateTime;

        public ValidFor() {
            this.startDateTime = "2024-01-01T00:00:00Z";
        }

        public String getStartDateTime() { return startDateTime; }
        public void setStartDateTime(String startDateTime) { this.startDateTime = startDateTime; }
        public String getEndDateTime() { return endDateTime; }
        public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }
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
