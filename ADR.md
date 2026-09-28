# Architecture Decision Record (ADR) - StockPulse

## 1. Where does commerce logic live?
**Context** — What boundary prevents a service class accumulating pricing, reorder, events, and persistence?
**Options** — 
1. Embed inside `ProductService` or `SuggestionService`.
2. Dedicated `CommerceAdvisor` interface with specific implementation components.
**Decision** — Chose a dedicated `CommerceAdvisor` interface (`RuleBasedCommerceAdvisor`, `AICommerceAdvisor`). 
**Tradeoffs** — Added slight complexity with multiple beans and interfaces, but prevents the `ProductService` from becoming a God class. It cleanly separates the domain event handlers (which invoke the advisor) from the business reasoning of what the price should be.

## 2. Unified AI call vs separate pricing/reorder calls?
**Context** — One prompt returning both vs separate calls.
**Options** — 
1. Unified API prompt.
2. Separate API calls for pricing and reorder suggestions.
**Decision** — We created a split contract at the Java Interface level (`suggestPricing` and `suggestReorder`), but practically the implementation does separate calls. A unified interface was considered but separate ones provide better fallback isolation. If pricing fails parsing, reorder shouldn't fail.
**Tradeoffs** — Latency and cost are higher with two separate LLM calls per trigger, but reliability and error boundaries are significantly better.

## 3. How does runtime strategy switching work?
**Context** — Need to switch between rule-based and AI without code change or restart.
**Options** — 
1. Simple IF statements in the controller.
2. Strategy Pattern using `CommerceAdvisorRegistry` injecting all `CommerceAdvisor` implementations.
**Decision** — Implemented a `CommerceAdvisorRegistry` that collects a map of advisors by name (`rule-based`, `ai-based`). The active strategy is retrieved via an `application.properties` value, and can be updated at runtime.
**Tradeoffs** — Requires maintaining bean names and mapping logic, but allows trivial addition of the sprint 2 `CompetitorAwareStrategy` without touching existing loops.

## 4. LLM failure handling
**Context** — LLM can timeout, return malformed JSON, or absurd prices.
**Options** — 
1. Drop the suggestion completely.
2. Silent rule-based fallback.
**Decision** — The `AICommerceAdvisor` wraps the LLM call in a `try-catch` block. If parsing fails, it delegates immediately to `ruleBasedFallback.suggestPricing(product)`. 
**Tradeoffs** — Users might not realize they got a rule-based fallback instead of AI unless they check the "reasoning" string, but silent dropping is much worse for inventory management.

## 5. Agentic loop trigger and decoupling
**Context** — Stock updates and order events must fire async recommendation without blocking response.
**Options** — 
1. Direct method calls inside `ProductService`.
2. Spring `@EventListener` and `@Async`.
**Decision** — Used Spring's `ApplicationEventPublisher` in `ProductService` to publish `InventoryLowEvent` and `DemandSpikeEvent`. The `AgenticRecommendationLoop` listens with `@Async @EventListener`.
**Tradeoffs** — Makes tracking execution flows harder in standard logs, but strictly separates the REST HTTP response time from the heavy AI processing time. Also enforces idempotency checking in the loop before generating duplicate suggestions.

## 6. Extensibility and exclusions
**Context** — Preparing for Sprint 2 competitor integrations.
**Options** — N/A
**Decision** — We explicitly added `costPrice` and `supplierId` to the `Product` entity (nullable). 
**Exclusions:** Excluded full SSE streaming and manual storefront views due to time constraints, prioritizing a rock-solid decoupled agentic loop and clean UI floor.
