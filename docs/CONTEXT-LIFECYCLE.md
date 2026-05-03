# Context Development Lifecycle

## Overview

Context is a first-class artifact, just like code. It has a lifecycle: **Generate → Evaluate → Distribute → Observe**, supported by team practices that make context a shared, repeatable, and improvable part of software delivery.

## The Four Phases

```
┌───────────┐     ┌───────────┐     ┌─────────────┐     ┌───────────┐
│ GENERATE  │────►│ EVALUATE  │────►│ DISTRIBUTE   │────►│ OBSERVE   │
│           │     │           │     │              │     │           │
│ Create    │     │ Quality   │     │ Deliver to   │     │ Monitor   │
│ context   │     │ check     │     │ right place  │     │ & improve │
└───────────┘     └───────────┘     └─────────────┘     └───────────┘
       ▲                                                        │
       └────────────── Feedback loop ──────────────────────────┘
```

### 1. GENERATE — Create Context

Produce the context that feeds AI systems, teams, and decisions.

| Source | What Gets Generated | Example |
|--------|-------------------|---------|
| O-RAN Specifications | Structured knowledge chunks, embeddings | `oran-specs/` directory |
| Incident History | Root cause patterns, fix recipes | Case store with learned patterns |
| Network KPIs | Baselines, thresholds, anomaly signatures | Rolling baselines per cell |
| xApp Catalog | Capability profiles, resource requirements | xApp registry with quality profiles |
| Team Decisions | ADRs, runbooks, design docs | `CLAUDE.md`, `copilot-instructions.md` |

**Key practice:** Every engineer who debugs an incident, tunes a threshold, or writes a runbook is generating context. Capture it at the source.

### 2. EVALUATE — Quality Check Context

Measure whether context is accurate, current, and useful.

| Check | Question | Failure Mode |
|-------|----------|--------------|
| **Freshness** | When was this last updated? | Stale spec → wrong xApp code |
| **Accuracy** | Does this match production? | Drifted baseline → false alerts |
| **Coverage** | Are there gaps? | Missing root cause → RCA fails |
| **Relevance** | Is this the right context for this query? | Wrong spec chunks retrieved |
| **Redundancy** | Are there duplicates/conflicts? | Conflicting runbooks |

**In this project:**
- RAG retrieval quality — are top-K chunks relevant to the query?
- xApp profile accuracy — do resource requirements match reality?
- TMF API conformance — do request/response models match spec?

### 3. DISTRIBUTE — Get Context Where It's Needed

Deliver the right context, to the right agent/person, at the right time.

```
┌─────────────────────────────────────────────────────┐
│                  Context Registry                     │
│  (single source of truth — versioned, searchable)    │
└──────────┬──────────┬──────────┬──────────┬──────────┘
           │          │          │          │
      ┌────▼───┐ ┌───▼────┐ ┌──▼───┐ ┌───▼────┐
      │ Edge   │ │ Core   │ │Cloud │ │Human   │
      │ Agent  │ │ Agent  │ │ LLM  │ │Engineer│
      └────────┘ └────────┘ └──────┘ └────────┘
```

**Distribution patterns:**
- **Push:** Model artifacts pushed to edge nodes on update
- **Pull:** RAG retrieves relevant chunks on query
- **Event-driven:** Anomaly detected → context injected into remediation pipeline
- **Human-in-the-loop:** Dashboard shows context before action

### 4. OBSERVE — Monitor Context in Production

Track how context performs and feed back improvements.

| Metric | What It Measures | Target |
|--------|-----------------|--------|
| Context hit rate | % queries with relevant results | > 90% |
| Staleness | Age since last update | < 30 days |
| Drift | Deviation from baseline | < 5% |
| Impact | Did context lead to correct outcomes? | > 85% |
| Usage | Which context is actually used | Top 20% covers 80% queries |

---

## Team Practices

### Context as Code
Store context in version-controlled files, not in people's heads.
- `CLAUDE.md` — coding context, versioned
- `copilot-instructions.md` — project context, versioned
- `DEPLOYMENT.md` — infrastructure context, versioned

### Context Reviews
Review context changes like code changes.
- Updated a threshold? Review why.
- Added a new pattern? Validate against incidents.
- Re-indexed specs? Verify chunk quality.

### Context Ownership
Every piece of context has an owner who keeps it fresh.
- xApp catalog → RAN engineering team
- Spec library → Standards & architecture team
- Case store → SRE team

### Context Debt
Track stale/missing context like technical debt.
- "O2 interface specs not indexed yet" = context debt
- "Interference patterns not validated in 6 months" = context debt
- `CLAUDE.md` not updated after architecture change = context debt

### Context Rituals
Build context maintenance into team ceremonies.
- **Sprint planning:** Review context debt, prioritize updates
- **Post-incident:** Extract new patterns into knowledge base
- **Weekly:** Check retrieval quality metrics
- **Monthly:** Audit baselines and thresholds for drift

---

## Interview Answer

> "We treat context as a first-class artifact with its own lifecycle. We generate it from specs, incidents, and operational data. We evaluate it for freshness, accuracy, and coverage. We distribute it through push, pull, and event-driven patterns depending on the latency requirement. And we observe it in production — tracking retrieval hit rates, drift, and whether it actually leads to correct outcomes. The team practices that make this work: context is version-controlled like code, reviewed like code, owned by specific teams, and tracked like technical debt when it's stale or missing. This isn't optional — bad context produces bad AI outputs, and in telecom, bad AI outputs mean network outages."
