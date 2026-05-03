# Deployment Strategy — Cloud, Edge, or On-Prem?

## TL;DR: Three-Tier Split

**Train in the cloud. Infer at the edge. Control plane in the core data center.**

In telecom AI, the deployment decision isn't cloud vs on-prem — it's about matching compute to the data gravity and latency requirements of each component.

## Architecture

```
Edge (Cell Site / Near-RT RIC)     Core DC (On-Prem)           Cloud (AWS/Azure/GCP)
┌──────────────────────────┐   ┌────────────────────┐   ┌──────────────────────────┐
│ xApp deployment decisions │   │ Orchestration API   │   │ Lagrangian optimization   │
│ Real-time SLA monitoring  │   │ TMF Open API gateway│   │ Model training/experiment │
│ Local remediation exec    │   │ Knowledge curator   │   │ LLM inference (LangChain) │
│ ML model inference (<10ms)│   │ Dashboard / Analytics│  │ CI/CD pipelines           │
│ Safety guardrails (local) │   │ Model registry      │   │ Data lake (historical)    │
└──────────────────────────┘   └────────────────────┘   └──────────────────────────┘
        │ < 10ms                       │ < 100ms                      │ batch/async
        └──────────────────────────────┴──────────────────────────────┘
                    Model artifacts flow: Cloud → Core DC → Edge
```

## Why This Architecture?

### 1. Data Sovereignty
Cell-site KPI data, subscriber locations, call metadata — this is regulated. In India, DoT requires certain data stays in-country. Many operators mandate on-prem for anything touching live network traffic. Inference must be on-prem or at the edge.

### 2. Latency Requirements
- **Edge (Near-RT RIC):** Sub-10ms for xApp selection and deployment decisions. Can't round-trip to a cloud region.
- **Core DC:** Sub-100ms for TMF API calls, orchestration dashboard, model registry lookups.
- **Cloud:** Seconds to minutes for training, batch analytics, LLM-based provisioning.

### 3. Cost Economics
- The Lagrangian optimization engine is compute-heavy but runs infrequently (on-demand). Cloud GPUs/TPUs make sense.
- The xApp selection model (small, ~5MB) runs 24/7 on every cell site. Local inference is the only economically viable path — no cloud API call per KPI per cell.
- LLM inference (NL provisioning) is bursty (engineers query during planning). Cloud-hosted with auto-scaling.

### 4. Model Lifecycle
**Train in cloud → Deploy to edge → Monitor centrally.**

1. Cloud: Train Lagrangian model, experiment with configurations, validate against historical data
2. Model Registry (Core DC): Versioned model artifacts (serialized Java/ONNX), A/B testing configs
3. Edge: Pull latest model, run inference locally, report metrics back
4. If accuracy degrades → automated retraining trigger → new model pushed via CI/CD

## ORAN-X Specific Deployment

| Component | Where | Why |
|-----------|-------|-----|
| Lagrangian Optimizer | Cloud | Compute-intensive, runs on-demand during planning |
| xApp Selection Engine | Edge/Near-RT RIC | Sub-10ms decisions, must be local |
| TMF622/620/640/688 APIs | Core DC / On-Prem | Standard BSS/OSS integration point |
| NL Provisioning (LangChain4j) | Cloud | LLM inference, not latency-sensitive |
| Dashboard / Health | Core DC | Operations team access, internal network |
| Configuration Store | Core DC | Centralized config, pushed to edge nodes |

## Scaling Strategy

- **Horizontal at edge:** One instance per Near-RT RIC (one per cell site cluster)
- **Vertical at core:** Scale TMF API gateway based on request volume
- **Elastic at cloud:** Auto-scale training jobs, serverless LLM endpoints
- **Rolling model updates:** Blue-green deployment of model artifacts to edge nodes

## Interview Answer

> "For ORAN-X, I designed a three-tier deployment: the Lagrangian optimization trains in the cloud where compute is elastic, the trained model artifact gets pushed to edge nodes at the Near-RT RIC for sub-10ms xApp selection decisions, and the TMF Open API integration layer sits in the operator's core data center as the BSS/OSS bridge. This respects data sovereignty — cell-site KPIs never leave the operator's network — while keeping cloud economics for the heavy compute."
