package com.oranx.tmf;

import com.oranx.model.*;
import com.oranx.engine.OrchestrationEngine;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * TMF622: Product Ordering API
 * Exposes xApp orchestration as product orders following TMF Open API patterns.
 *
 * TMF622 defines APIs for:
 * - Creating product orders
 * - Managing product order items
 * - Tracking order status
 * - Managing order cancellation and modification
 */
@Path("/tmf-api/productOrdering/v4")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TMF622Resource {

    private static final Logger LOG = LoggerFactory.getLogger(TMF622Resource.class);

    @Inject
    OrchestrationEngine orchestrationEngine;

    // In-memory storage for demo purposes (use database in production)
    private static final Map<String, ProductOrder> orders = new HashMap<>();

    /**
     * Create a product order for xApp deployment.
     * POST /productOrder
     */
    @POST
    @Path("/productOrder")
    public Response createProductOrder(ProductOrderRequest request) {
        LOG.info("Creating product order: {}", request);

        try {
            // Generate order ID
            String orderId = "PO-" + UUID.randomUUID().toString().substring(0, 8);

            // Convert TMF order to orchestration request
            OrchestrationRequest orchRequest = convertToOrchestrationRequest(request);

            // Execute orchestration (for demo, use a sample xApp catalog)
            List<XApp> xAppCatalog = createSampleXAppCatalog();
            OrchestrationResult result = orchestrationEngine.orchestrate(orchRequest, xAppCatalog);

            // Create product order
            ProductOrder order = new ProductOrder();
            order.setId(orderId);
            order.setExternalId(request.getExternalId());
            order.setRequestedStartDate(LocalDateTime.now());
            order.setRequestedCompletionDate(LocalDateTime.now().plusHours(1));
            order.setOrderItem(convertToOrderItems(result, request));
            order.setCategory("xApp Deployment");
            order.setState(mapResultToOrderState(result));

            orders.put(orderId, order);

            LOG.info("Product order created: {}", orderId);
            return Response.status(Response.Status.CREATED).entity(order).build();

        } catch (Exception e) {
            LOG.error("Failed to create product order", e);
            return Response.serverError()
                .entity(createErrorResponse("ORDER_CREATION_FAILED", e.getMessage()))
                .build();
        }
    }

    /**
     * Retrieve a product order by ID.
     * GET /productOrder/{id}
     */
    @GET
    @Path("/productOrder/{id}")
    public Response getProductOrder(@PathParam("id") String id) {
        LOG.info("Retrieving product order: {}", id);

        ProductOrder order = orders.get(id);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("ORDER_NOT_FOUND", "Order not found: " + id))
                .build();
        }

        return Response.ok(order).build();
    }

    /**
     * List all product orders.
     * GET /productOrder
     */
    @GET
    @Path("/productOrder")
    public Response listProductOrders(
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        List<ProductOrder> allOrders = new ArrayList<>(orders.values());
        int total = allOrders.size();

        // Apply pagination
        List<ProductOrder> page = allOrders.stream()
            .skip(offset)
            .limit(limit)
            .toList();

        return Response.ok(page)
            .header("X-Total-Count", total)
            .header("X-Result-Count", page.size())
            .build();
    }

    /**
     * Cancel a product order.
     * PATCH /productOrder/{id}
     */
    @PATCH
    @Path("/productOrder/{id}")
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    public Response cancelProductOrder(@PathParam("id") String id, List<Map<String, Object>> patches) {
        LOG.info("Cancelling product order: {}", id);

        ProductOrder order = orders.get(id);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("ORDER_NOT_FOUND", "Order not found: " + id))
                .build();
        }

        // Check if order can be cancelled
        if (order.getState() == OrderState.COMPLETED || order.getState() == OrderState.FAILED) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(createErrorResponse("ORDER_NOT_CANCELLABLE", "Order cannot be cancelled in state: " + order.getState()))
                .build();
        }

        // Apply cancellation
        order.setState(OrderState.CANCELLED);
        order.setOrderItem(Collections.emptyList());

        return Response.ok(order).build();
    }

    /**
     * Converts TMF product order request to orchestration request.
     */
    private OrchestrationRequest convertToOrchestrationRequest(ProductOrderRequest request) {
        OrchestrationRequest orchRequest = new OrchestrationRequest();
        orchRequest.setRequestId(request.getExternalId());
        orchRequest.setSharedXAppsAllowed(true);

        // Extract services from order items
        for (ProductOrderItem item : request.getOrderItem()) {
            Service service = new Service();
            service.setId(item.getProduct().getId());
            service.setName(item.getProduct().getName());
            service.setPriority(item.getPriority() != null ? item.getPriority() : 1.0);
            service.setFrequency(item.getQuantity() != null ? item.getQuantity() : 1.0);

            // Set SLA from product characteristics
            if (item.getProduct().getProductCharacteristic() != null) {
                for (ProductCharacteristic characteristic : item.getProduct().getProductCharacteristic()) {
                    switch (characteristic.getName()) {
                        case "quality":
                            service.setQualityTarget(Double.parseDouble(characteristic.getValue()));
                            break;
                        case "latency":
                            service.setLatencyTargetMs(Double.parseDouble(characteristic.getValue()));
                            break;
                        case "configuration":
                            service.setConfigurationType(ServiceConfigurationType.fromString(characteristic.getValue()));
                            break;
                    }
                }
            }

            orchRequest.addService(service);
        }

        // Set resource budget
        ResourceBudget budget = new ResourceBudget(16, 32, 100);
        orchRequest.setResourceBudget(budget);

        return orchRequest;
    }

    /**
     * Converts orchestration result to TMF order items.
     */
    private List<ProductOrderItem> convertToOrderItems(OrchestrationResult result, ProductOrderRequest request) {
        List<ProductOrderItem> items = new ArrayList<>();

        for (ServiceConfiguration config : result.getDeployedConfigurations()) {
            ProductOrderItem item = new ProductOrderItem();
            item.setId("item-" + UUID.randomUUID().toString().substring(0, 8));
            item.setState(mapResultToItemState(result));
            item.setQuantity(1);

            ProductRef product = new ProductRef();
            product.setId(config.getServiceId());
            product.setName("xApp Service: " + config.getConfigurationType().getName());
            item.setProduct(product);

            // Add characteristics
            List<ProductCharacteristic> characteristics = new ArrayList<>();
            characteristics.add(new ProductCharacteristic("quality", String.valueOf(config.getTotalQuality())));
            characteristics.add(new ProductCharacteristic("latency", String.valueOf(config.getTotalLatencyMs())));
            item.getProduct().setProductCharacteristic(characteristics);

            items.add(item);
        }

        return items;
    }

    /**
     * Maps orchestration result to TMF order state.
     */
    private OrderState mapResultToOrderState(OrchestrationResult result) {
        switch (result.getStatus()) {
            case SUCCESS:
                return OrderState.COMPLETED;
            case PARTIAL_SUCCESS:
                return OrderState.PARTIAL;
            case FAILURE:
                return OrderState.FAILED;
            case IN_PROGRESS:
                return OrderState.IN_PROGRESS;
            default:
                return OrderState.ACKNOWLEDGED;
        }
    }

    /**
     * Maps orchestration result to TMF item state.
     */
    private ItemState mapResultToItemState(OrchestrationResult result) {
        switch (result.getStatus()) {
            case SUCCESS:
                return ItemState.COMPLETED;
            case PARTIAL_SUCCESS:
                return ItemState.PARTIAL;
            case FAILURE:
                return ItemState.FAILED;
            default:
                return ItemState.IN_PROGRESS;
        }
    }

    /**
     * Creates a sample xApp catalog for demo.
     */
    private List<XApp> createSampleXAppCatalog() {
        List<XApp> catalog = new ArrayList<>();

        // Traffic forecaster xApps
        catalog.add(new XApp("xapp-tf-0", "Forecaster Lite", RANFunction.TRAFFIC_FORECASTER,
            0.5, 1.0, 5.0, 0.85, 0.8));
        catalog.add(new XApp("xapp-tf-1", "Forecaster Pro", RANFunction.TRAFFIC_FORECASTER,
            1.5, 3.0, 10.0, 0.95, 1.5));

        // Traffic classificator xApps
        catalog.add(new XApp("xapp-tc-0", "Classificator Lite", RANFunction.TRAFFIC_CLASSIFICATOR,
            0.3, 0.8, 3.0, 0.82, 0.5));
        catalog.add(new XApp("xapp-tc-1", "Classificator Pro", RANFunction.TRAFFIC_CLASSIFICATOR,
            1.0, 2.0, 8.0, 0.94, 1.0));

        // Network slicer xApps
        catalog.add(new XApp("xapp-ns-0", "Slicer Lite", RANFunction.NETWORK_SLICER,
            0.4, 1.5, 4.0, 0.88, 0.6));
        catalog.add(new XApp("xapp-ns-1", "Slicer Pro", RANFunction.NETWORK_SLICER,
            1.2, 2.5, 12.0, 0.96, 1.2));

        return catalog;
    }

    private ErrorResponse createErrorResponse(String code, String message) {
        ErrorResponse error = new ErrorResponse();
        error.setCode(code);
        error.setMessage(message);
        error.setReason(message);
        return error;
    }

    // TMF Data Models
    public static class ProductOrderRequest {
        private String externalId;
        private List<ProductOrderItem> orderItem;

        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public List<ProductOrderItem> getOrderItem() { return orderItem; }
        public void setOrderItem(List<ProductOrderItem> orderItem) { this.orderItem = orderItem; }
    }

    public static class ProductOrder {
        private String id;
        private String externalId;
        private LocalDateTime requestedStartDate;
        private LocalDateTime requestedCompletionDate;
        private List<ProductOrderItem> orderItem;
        private String category;
        private OrderState state;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public LocalDateTime getRequestedStartDate() { return requestedStartDate; }
        public void setRequestedStartDate(LocalDateTime requestedStartDate) { this.requestedStartDate = requestedStartDate; }
        public LocalDateTime getRequestedCompletionDate() { return requestedCompletionDate; }
        public void setRequestedCompletionDate(LocalDateTime requestedCompletionDate) { this.requestedCompletionDate = requestedCompletionDate; }
        public List<ProductOrderItem> getOrderItem() { return orderItem; }
        public void setOrderItem(List<ProductOrderItem> orderItem) { this.orderItem = orderItem; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public OrderState getState() { return state; }
        public void setState(OrderState state) { this.state = state; }
    }

    public static class ProductOrderItem {
        private String id;
        private ProductRef product;
        private ItemState state;
        private Double quantity;
        private Double priority;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public ProductRef getProduct() { return product; }
        public void setProduct(ProductRef product) { this.product = product; }
        public ItemState getState() { return state; }
        public void setState(ItemState state) { this.state = state; }
        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }
        public Double getPriority() { return priority; }
        public void setPriority(Double priority) { this.priority = priority; }
    }

    public static class ProductRef {
        private String id;
        private String name;
        private List<ProductCharacteristic> productCharacteristic;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<ProductCharacteristic> getProductCharacteristic() { return productCharacteristic; }
        public void setProductCharacteristic(List<ProductCharacteristic> productCharacteristic) { this.productCharacteristic = productCharacteristic; }
    }

    public static class ProductCharacteristic {
        private String name;
        private String value;

        public ProductCharacteristic() {}
        public ProductCharacteristic(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class ErrorResponse {
        private String code;
        private String message;
        private String reason;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public enum OrderState {
        ACKNOWLEDGED, ACCEPTED, REJECTED, IN_PROGRESS, COMPLETED, PARTIAL, FAILED, CANCELLED
    }

    public enum ItemState {
        ACKNOWLEDGED, ACCEPTED, REJECTED, IN_PROGRESS, COMPLETED, PARTIAL, FAILED, CANCELLED
    }
}
