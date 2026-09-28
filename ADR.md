# Architecture Decision Record — StockPulse

Format for each entry: **Context → Options → Decision → Tradeoffs**.

---

## 1. Where does commerce logic live?

**Context** — Stock updates, order simulation, event publishing, persistence, pricing math, and LLM calls cannot live in one service without that class becoming untestable.

**Options**
1. Put pricing and reorder rules inside `ProductService`.
2. A dedicated `CommerceAdvisor` contract with rule-based and AI implementations, called from HTTP and from the agentic loop.

**Decision** — Commerce reasoning lives in `CommerceAdvisor` (`RuleBasedCommerceAdvisor`, `AICommerceAdvisor`). `ProductService` owns inventory mutations and publishes signals. `AgenticRecommendationLoop` owns observe → reason → queue. `SuggestionService` owns the human checkpoint (accept/reject side effects).

**Tradeoffs** — More types to follow in a walkthrough. The payoff is that a sprint-2 `CompetitorAwareCommerceAdvisor` registers as another bean and never touches `ProductService`.

---

## 2. Unified AI call vs separate pricing/reorder calls?

**Context** — One trigger often wants both a price and a reorder quantity. A single prompt is cheaper; two calls isolate failure.

**Options**
1. One LLM call returning both JSON objects.
2. Split Java methods (`suggestPricing` / `suggestReorder`) with independent prompts and independent fallbacks.

**Decision** — Split contracts. Inventory-low pricing is a raise-vs-clearance judgment; demand-spike pricing is a modest capitalize-or-hold judgment; replenishment is a quantity problem. Those are different merchandising decisions (see `AdvisorPromptFactory`). If pricing JSON is absurd, reorder still lands.

**Tradeoffs** — Two LLM round-trips per auto trigger when the AI strategy is on. Reliability and prompt quality beat latency for a human-in-the-loop advisor.

---

## 3. How does runtime strategy switching work?

**Context** — HTTP on-demand endpoints and async event handlers must use the same active strategy. Sprint 2's third strategy must plug in without editing callers. Switching should not require a process restart.

**Options**
1. `if (strategy.equals("ai"))` in controllers.
2. Spring Cloud `@RefreshScope`.
3. A `CommerceAdvisorRegistry` map keyed by `getName()`, defaulted from `commerce.strategy`, mutable via `PUT /commerce/strategy`.

**Decision** — Option 3. Registry injects every `CommerceAdvisor`. `AgenticRecommendationLoop` and `SuggestionController` both call `getActiveAdvisor()`. A competitor strategy in sprint 2 is: implement the interface, add `@Component`, done.

**Tradeoffs** — In-memory switch is per JVM (lost on restart, which re-reads `commerce.strategy`). Sufficient for the demo; config-server refresh is overkill.

---

## 4. LLM failure handling

**Context** — Timeouts, missing API keys, malformed JSON, prices at $0 or 50× current, non-integer reorder qty.

**Options**
1. Drop the suggestion (silent).
2. Fail the HTTP request and skip the async path.
3. Validate strictly; on any failure delegate to the rule-based advisor and persist that, annotated in `reasoning`.

**Decision** — Option 3 in `AICommerceAdvisor` + `LlmResponseParser`. Bounds: price must be positive and within 0.1×–10× current; reorder qty must be a positive integer. Empty `LLM_API_KEY` skips the network call immediately. The async path never silent-drops: rules always produce a row.

**Tradeoffs** — Merchandising may see a rule-based recommendation labeled as fallback. That is honest and safer than an empty queue while stock is critically low.

---

## 5. Agentic loop trigger and decoupling

**Context** — `PATCH /stock` and `POST /orders` must return immediately. Recommendations should fire because inventory or velocity *changed*, not because a cron ticked. Repeated sales on the same SKU must not flood duplicate PENDING rows.

**Options**
1. Call the advisor inline in `ProductService` (blocks).
2. A scheduled poller over low-stock SKUs.
3. Domain events + `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`, with idempotency on `(product, PENDING, triggerReason, suggestion type)`.

**Decision** — Option 3. `InventoryLowEvent` when stock &lt; reorder threshold. `DemandSpikeEvent` when velocity &gt; configurable `commerce.demand-spike-multiplier` × category average (default 3×). Two handlers, one `processSignal` — both triggers can queue for the same SKU (different `triggerReason`). Prices do **not** change until merchandising accepts.

**Tradeoffs** — Harder to trace in a debugger than a synchronous call. After-commit avoids the async thread reading uncommitted stock. `fallbackExecution = true` keeps the loop alive if an event is published outside a transaction (tests).

---

## 6. Extensibility and exclusions

**Context** — Sprint 2 needs competitor prices, margin floors, and suppliers without rewriting the loop.

**Decision — extension seam in code**
- Nullable `costPrice`, `marginFloor`, `supplierId` on `Product` (seeded now for margin display).
- `CommerceAdvisor` + registry: next bean is `CompetitorAwareCommerceAdvisor`.
- `InventorySnapshot` and `PriceHistory` already record the trail a cooldown or auto-apply rule would read.

**Deliberate exclusions (priority, not leftovers)**
- No storefront, cart, or payments — out of sprint-1 scope.
- No auto-apply of high-confidence prices — human checkpoint is the product.
- No supplier purchase-order API on reorder accept — accept simulates inbound stock only.
- Postgres is supported as a driver but the default is H2 so the README path stays under five minutes.

**Tradeoffs** — Margin floor is displayed and mentioned in prompts but not yet a hard constraint in the parser. That constraint is the first sprint-2 line in `LlmResponseParser.parsePricing`.
