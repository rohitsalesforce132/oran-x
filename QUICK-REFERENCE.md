# ORAN-X Quick Reference

## 🚀 Start the Application

```bash
cd /home/rohit/.openclaw/workspace/oran-x
mvn quarkus:dev
```

Access at: http://localhost:8080/swagger-ui

## 🎯 Key Endpoints

### Orchestration
- `POST /api/v1/orchestrate` - Execute orchestration
- `POST /api/v1/orchestrate/validate` - Validate constraints

### Natural Language
- `POST /api/v1/provision` - Deploy with plain English

### Catalog Management
- `GET /api/v1/xapps` - List xApps
- `GET /api/v1/services` - List services

### TMF APIs
- `POST /tmf-api/productOrdering/v4/productOrder` - Create order
- `GET /tmf-api/productCatalogManagement/v4/productCatalog` - Browse catalog

### System
- `GET /api/v1/dashboard` - System overview
- `GET /api/v1/dashboard/health` - Health check

## 📝 Example Requests

### Natural Language Provisioning
```bash
curl -X POST http://localhost:8080/api/v1/provision \
  -H "Content-Type: application/json" \
  -d '{"request": "I need traffic forecasting with 95% accuracy and low latency"}'
```

### List xApps
```bash
curl http://localhost:8080/api/v1/xapps
```

### Health Check
```bash
curl http://localhost:8080/api/v1/dashboard/health
```

## 🏗️ Project Structure

```
src/main/java/com/oranx/
├── model/           # Service, XApp, OrchestrationRequest/Result
├── engine/          # LagrangianOrchestrator, XAppSelector
├── llm/             # NLProvisioningService, SLAExtractor
├── tmf/             # TMF622, TMF620, TMF640, TMF688
├── resource/        # OrchestrationResource, XAppResource, etc.
└── OranXApplication.java

src/test/java/com/oranx/
├── LagrangianOrchestratorTest.java
├── SemanticEquivalenceTest.java
└── FeasibilityCheckerTest.java

docs/
├── QA-BANK.md       # 60+ Q&A
└── BUILD-SUMMARY.md # Complete build details

STAR.md                    # Interview summary
README.md                  # Full documentation
```

## 🔑 Key Concepts

### OREO Algorithm
- **Lagrangian Decomposition**: Breaks optimization into per-function subproblems
- **Semantic Equivalence**: Share xApps when quality ±10%, latency ±1ms
- **Greedy Selection**: Prioritize by weighted value (priority × frequency)
- **Result**: +35% service density, -30% xApps needed

### Service Configurations
- `FORECASTER_ONLY` - Traffic prediction
- `CLASSIFICATOR_ONLY` - Traffic categorization
- `SLICER_ONLY` - Network slicing
- `FULL_PIPELINE` - All three functions
- `FORECASTER_SLICER` - Predict + slice
- `CLASSIFICATOR_SLICER` - Classify + slice

### TMF Mapping
- TMF622 → Orchestration requests
- TMF620 → xApp catalog
- TMF640 → xApp deployment lifecycle
- TMF688 → Health/performance events

## 🧪 Run Tests

```bash
mvn test
```

## 📚 Documentation

- `README.md` - Start here
- `STAR.md` - Interview prep
- `docs/QA-BANK.md` - Deep dive
- `BUILD-SUMMARY.md` - Build details

## ⚙️ Configuration

Edit `src/main/resources/application.properties`:
```properties
quarkus.http.port=8080
quarkus.log.category."com.oranx".level=INFO
```

Set environment variables:
```bash
export OPENAI_API_KEY=your-key
```

## 🎯 Success Metrics

- ✅ 35% service density improvement
- ✅ <500ms orchestration latency
- ✅ 25+ REST endpoints
- ✅ 4 TMF Open APIs
- ✅ LLM-powered NL provisioning

## 🆘 Troubleshooting

**Port 8080 in use?**
```properties
quarkus.http.port=8081
```

**LLM not working?**
Set `OPENAI_API_KEY` or system uses regex-only mode.

**Tests failing?**
Ensure Java 21 and Maven 3.8+ are installed.

## 📖 Learn More

1. Read `STAR.md` for interview prep
2. Study `docs/QA-BANK.md` for deep knowledge
3. Explore `/swagger-ui` for interactive API docs
4. Run tests to see examples in action

---
Built with Quarkus 3.x + LangChain4j + TMF Open APIs
