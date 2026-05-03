# ORAN-X Copilot Instructions

## Architecture
Quarkus 3.x microservice for O-RAN xApp orchestration with Lagrangian optimization, TMF Open API integration, and LangChain4j-powered natural language provisioning.

## Key Concepts
- **xApp**: ML-powered application in O-RAN Near-RT RIC for real-time RAN control (e.g., traffic forecasting, classification, slicing)
- **Service Configuration**: DAG of RAN functions that fulfill a telecom service (e.g., {forecaster+classificator+slicer}, {forecaster+slicer}, {slicer alone})
- **Semantic Equivalence**: xApps can be shared across services when they implement the same function with compatible quality/latency profiles (±10% quality, ±1ms latency)
- **Lagrangian Decomposition**: Optimization technique that decomposes the multi-service xApp selection problem into per-function subproblems, achieving 35% service density improvement

## Package Structure
- `com.oranx.model` - Domain entities (Service, XApp, RANFunction, ServiceConfiguration, OrchestrationRequest/Result, ResourceBudget, etc.)
- `com.oranx.engine` - Orchestration engine (LagrangianOrchestrator, XAppSelector, SemanticEquivalenceResolver, FeasibilityChecker, CostCalculator)
- `com.oranx.llm` - LangChain4j integration (NLProvisioningService, SLAExtractor, XAppRecommender, DeploymentExplainer)
- `com.oranx.tmf` - TMF Open API resources (TMF622 Product Ordering, TMF620 Product Catalog, TMF640 Service Activation, TMF688 Event Management)
- `com.oranx.resource` - REST API endpoints (OrchestrationResource, XAppResource, ServiceResource, NLResource, DashboardResource)

## Conventions
- Use Quarkus reactive patterns (Uni/Multi) for async operations (though current implementation uses blocking for simplicity)
- TMF APIs follow standard REST patterns with TMF data models (Product, Service, Event, Order)
- All orchestration results include feasibility report with constraint violations and recommendations
- LLM interactions are mocked in tests using LangChain4j test utilities
- Use @ApplicationScoped for CDI beans (orchestration engine, LLM services)
- Response times should be <500ms for orchestration, <1s for NL provisioning

## Code Style
- Java 21 with records for immutable data where appropriate
- Builder pattern for complex object construction
- Fluent API for configuration (OrchestrationRequest.builder()...)
- SLF4J for logging with DEBUG level in dev, INFO in prod
- JUnit 5 + AssertJ for testing
- OpenAPI annotations (@Schema) for TMF API documentation

## TMF Open API Integration
- **TMF622 (Product Ordering)**: Map orchestration requests to ProductOrder, xApp deployments to ProductOrderItems
- **TMF620 (Product Catalog)**: xApp catalog exposed as ProductOffering with ProductSpecification
- **TMF640 (Service Activation)**: ServiceConfigurations become ServiceActivations with lifecycle management
- **TMF688 (Event Management)**: Publish xApp health, performance, and lifecycle events

## OREO Algorithm Notes
- Quality combines multiplicatively across xApps in a service configuration (weakest link)
- Latency adds up (theta models processing time per xApp)
- Resource budgets are global constraints (cpu, memory, disk)
- Services are sorted by weighted value (priority × frequency) before greedy selection
- Semantic equivalence sharing gives a +0.5 score bonus in selection

## LLM Integration
- Use regex first for SLA extraction (fast, reliable), fall back to GPT-4 for complex requests
- Prompt engineering includes clear JSON structure requirements
- Mock LLM in tests by implementing ChatLanguageModel interface
- Set OPENAI_API_KEY environment variable for production

## Testing Strategy
- Unit tests for engine components (LagrangianOrchestratorTest, SemanticEquivalenceTest, FeasibilityCheckerTest)
- Integration tests for TMF API compliance
- Mock xApp catalog for consistent test results
- Test both success and failure scenarios (constraint violations, insufficient resources)

## Performance Targets
- Orchestration latency: <500ms for 50 services
- NL provisioning: <2s end-to-end (including LLM)
- TMF API response: <100ms for catalog queries
- Memory usage: <512MB per orchestration engine instance
- Scale: 100+ concurrent orchestration requests

## Deployment Notes
- Quarkus fast-jar or native image for deployment
- Kubernetes-ready with health checks (DashboardResource.health)
- Configurable via application.properties or environment variables
- Horizontal scaling supported (stateless orchestration engine)
- Connects to external LLM service (OpenAI API)

## TODO for Future Enhancements
- Implement iterative Lagrangian multiplier updates (currently greedy approximation)
- Add database persistence for orchestration state
- Implement TMF629 Product Inventory for tracking deployed instances
- Add multi-tenant support with TMF633 Party Role
- Implement A1 interface for policy-based xApp management
- Add O-RAN E2 termination for direct RIC interaction
- Implement canary deployments and A/B testing for xApps
- Add real-time monitoring with Prometheus metrics
