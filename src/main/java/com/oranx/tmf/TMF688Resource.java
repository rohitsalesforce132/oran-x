package com.oranx.tmf;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TMF688: Event Management API
 * Manages and publishes events related to xApp health, performance, and lifecycle.
 *
 * TMF688 defines APIs for:
 * - Event registration
 * - Event query and retrieval
 * - Event notification and subscription
 */
@Path("/tmf-api/eventManagement/v4")
@Produces(MediaType.APPLICATION.Json)
@Consumes(MediaType.APPLICATION_JSON)
public class TMF688Resource {

    private static final Logger LOG = LoggerFactory.getLogger(TMF688Resource.class);

    // In-memory event storage
    private static final Map<String, Event> events = new ConcurrentHashMap<>();
    private static final Map<String, EventSubscription> subscriptions = new ConcurrentHashMap<>();
    private static final AtomicLong eventIdCounter = new AtomicLong(1);
    private static final AtomicLong subscriptionIdCounter = new AtomicLong(1);

    /**
     * Create a new event.
     * POST /event
     */
    @POST
    @Path("/event")
    public Response createEvent(Event event) {
        LOG.info("Creating event: type={}", event.getEventType());

        try {
            // Generate event ID if not provided
            if (event.getId() == null || event.getId().isEmpty()) {
                event.setId("EVT-" + eventIdCounter.getAndIncrement());
            }

            // Set timestamp if not provided
            if (event.getEventTime() == null) {
                event.setEventTime(LocalDateTime.now());
            }

            // Store event
            events.put(event.getId(), event);

            // Notify subscribers
            notifySubscribers(event);

            LOG.info("Event created: {}", event.getId());
            return Response.status(Response.Status.CREATED).entity(event).build();

        } catch (Exception e) {
            LOG.error("Failed to create event", e);
            return Response.serverError()
                .entity(createErrorResponse("EVENT_CREATION_FAILED", e.getMessage()))
                .build();
        }
    }

    /**
     * Retrieve an event by ID.
     * GET /event/{id}
     */
    @GET
    @Path("/event/{id}")
    public Response getEvent(@PathParam("id") String id) {
        LOG.info("Retrieving event: {}", id);

        Event event = events.get(id);
        if (event == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("EVENT_NOT_FOUND", "Event not found: " + id))
                .build();
        }

        return Response.ok(event).build();
    }

    /**
     * List events with filtering.
     * GET /event
     */
    @GET
    @Path("/event")
    public Response listEvents(
            @QueryParam("eventType") String eventType,
            @QueryParam("severity") EventSeverity severity,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        LOG.info("Listing events: type={}, severity={}", eventType, severity);

        List<Event> filtered = new ArrayList<>(events.values());

        // Filter by event type
        if (eventType != null && !eventType.isEmpty()) {
            filtered = filtered.stream()
                .filter(e -> eventType.equals(e.getEventType()))
                .toList();
        }

        // Filter by severity
        if (severity != null) {
            filtered = filtered.stream()
                .filter(e -> severity == e.getSeverity())
                .toList();
        }

        // Sort by event time (newest first)
        filtered.sort((e1, e2) -> e2.getEventTime().compareTo(e1.getEventTime()));

        int total = filtered.size();
        List<Event> page = filtered.stream()
            .skip(offset)
            .limit(limit)
            .toList();

        return Response.ok(page)
            .header("X-Total-Count", total)
            .header("X-Result-Count", page.size())
            .build();
    }

    /**
     * Create an event subscription.
     * POST /hub
     */
    @POST
    @Path("/hub")
    public Response createSubscription(SubscriptionRequest request) {
        LOG.info("Creating subscription: callback={}", request.getCallback());

        try {
            String subscriptionId = "SUB-" + subscriptionIdCounter.getAndIncrement();

            EventSubscription subscription = new EventSubscription();
            subscription.setId(subscriptionId);
            subscription.setCallback(request.getCallback());
            subscription.setQuery(request.getQuery());
            subscription.setCreatedDateTime(LocalDateTime.now());
            subscription.setExpirationDateTime(LocalDateTime.now().plusDays(30));

            subscriptions.put(subscriptionId, subscription);

            LOG.info("Subscription created: {}", subscriptionId);
            return Response.status(Response.Status.CREATED).entity(subscription).build();

        } catch (Exception e) {
            LOG.error("Failed to create subscription", e);
            return Response.serverError()
                .entity(createErrorResponse("SUBSCRIPTION_FAILED", e.getMessage()))
                .build();
        }
    }

    /**
     * Retrieve a subscription.
     * GET /hub/{id}
     */
    @GET
    @Path("/hub/{id}")
    public Response getSubscription(@PathParam("id") String id) {
        LOG.info("Retrieving subscription: {}", id);

        EventSubscription subscription = subscriptions.get(id);
        if (subscription == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("SUBSCRIPTION_NOT_FOUND", "Subscription not found: " + id))
                .build();
        }

        return Response.ok(subscription).build();
    }

    /**
     * List all subscriptions.
     * GET /hub
     */
    @GET
    @Path("/hub")
    public Response listSubscriptions() {
        LOG.info("Listing subscriptions");
        return Response.ok(new ArrayList<>(subscriptions.values())).build();
    }

    /**
     * Delete a subscription.
     * DELETE /hub/{id}
     */
    @DELETE
    @Path("/hub/{id}")
    public Response deleteSubscription(@PathParam("id") String id) {
        LOG.info("Deleting subscription: {}", id);

        EventSubscription subscription = subscriptions.remove(id);
        if (subscription == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse("SUBSCRIPTION_NOT_FOUND", "Subscription not found: " + id))
                .build();
        }

        return Response.noContent().build();
    }

    /**
     * Notify all subscribers of a new event.
     */
    private void notifySubscribers(Event event) {
        for (EventSubscription subscription : subscriptions.values()) {
            if (matchesQuery(event, subscription.getQuery())) {
                LOG.debug("Notifying subscription {} of event {}", subscription.getId(), event.getId());
                // In production, this would make an HTTP callback to the subscriber's endpoint
                // For demo, we just log the notification
            }
        }
    }

    /**
     * Check if an event matches a subscription query.
     */
    private boolean matchesQuery(Event event, String query) {
        if (query == null || query.isEmpty()) {
            return true;  // No filter = match all
        }

        // Simple query parsing (in production, use a proper query language)
        if (query.contains("eventType=")) {
            String eventType = query.substring(query.indexOf("eventType=") + 11);
            if (eventType.contains("&")) {
                eventType = eventType.substring(0, eventType.indexOf("&"));
            }
            if (!eventType.equals(event.getEventType())) {
                return false;
            }
        }

        if (query.contains("severity=")) {
            String severity = query.substring(query.indexOf("severity=") + 9);
            if (severity.contains("&")) {
                severity = severity.substring(0, severity.indexOf("&"));
            }
            if (!severity.equals(event.getSeverity().name())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Generate a sample health check event.
     */
    @GET
    @Path("/sample/healthCheck")
    public Response generateHealthCheckEvent(
            @QueryParam("serviceId") String serviceId,
            @QueryParam("status") @DefaultValue("HEALTHY") String status) {

        Event event = new Event();
        event.setEventType("xapp.healthCheck");
        event.setSeverity("HEALTHY".equals(status) ? EventSeverity.INFO : EventSeverity.CRITICAL);
        event.setEventTime(LocalDateTime.now());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("serviceId", serviceId != null ? serviceId : "service-1");
        attributes.put("status", status);
        attributes.put("quality", 0.95);
        attributes.put("latency", 1.5);

        event.setEventAttributes(attributes);

        return createEvent(event);
    }

    /**
     * Generate a sample performance event.
     */
    @GET
    @Path("/sample/performance")
    public Response generatePerformanceEvent(
            @QueryParam("serviceId") String serviceId,
            @QueryParam("quality") @DefaultValue("0.95") double quality,
            @QueryParam("latency") @DefaultValue("1.5") double latency) {

        Event event = new Event();
        event.setEventType("xapp.performance");
        event.setSeverity(quality < 0.8 || latency > 5.0 ? EventSeverity.WARNING : EventSeverity.INFO);
        event.setEventTime(LocalDateTime.now());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("serviceId", serviceId != null ? serviceId : "service-1");
        attributes.put("quality", quality);
        attributes.put("latency", latency);
        attributes.put("cpu_utilization", 45.2);
        attributes.put("memory_utilization", 62.8);

        event.setEventAttributes(attributes);

        return createEvent(event);
    }

    private ErrorResponse createErrorResponse(String code, String message) {
        ErrorResponse error = new ErrorResponse();
        error.setCode(code);
        error.setMessage(message);
        return error;
    }

    // TMF Data Models
    public static class Event {
        private String id;
        private String eventType;
        private EventSeverity severity;
        private LocalDateTime eventTime;
        private Map<String, Object> eventAttributes;

        public Event() {
            this.eventAttributes = new HashMap<>();
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public EventSeverity getSeverity() { return severity; }
        public void setSeverity(EventSeverity severity) { this.severity = severity; }
        public LocalDateTime getEventTime() { return eventTime; }
        public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
        public Map<String, Object> getEventAttributes() { return eventAttributes; }
        public void setEventAttributes(Map<String, Object> eventAttributes) { this.eventAttributes = eventAttributes; }
    }

    public static class EventSubscription {
        private String id;
        private String callback;
        private String query;
        private LocalDateTime createdDateTime;
        private LocalDateTime expirationDateTime;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCallback() { return callback; }
        public void setCallback(String callback) { this.callback = callback; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public LocalDateTime getCreatedDateTime() { return createdDateTime; }
        public void setCreatedDateTime(LocalDateTime createdDateTime) { this.createdDateTime = createdDateTime; }
        public LocalDateTime getExpirationDateTime() { return expirationDateTime; }
        public void setExpirationDateTime(LocalDateTime expirationDateTime) { this.expirationDateTime = expirationDateTime; }
    }

    public static class SubscriptionRequest {
        private String callback;
        private String query;

        public String getCallback() { return callback; }
        public void setCallback(String callback) { this.callback = callback; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
    }

    public enum EventSeverity {
        CRITICAL, MAJOR, MINOR, WARNING, INFO
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
