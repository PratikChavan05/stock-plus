# StockPulse

AI inventory and dynamic pricing advisor for ShopStream. When stock crosses a reorder threshold or demand velocity spikes, the backend queues **pricing** and **reorder** suggestions asynchronously. Merchandising accepts or rejects in the console. Live price never changes without an accept.

## Prerequisites

- Java 21+ (runs on newer JDKs)
- Node 18+
- Optional: `LLM_API_KEY` for Gemini (without it, the AI strategy falls back to rules)

## Run in under 5 minutes

**Terminal 1 — API**

```bash
cd backend
./mvnw spring-boot:run
```

Windows: `mvnw.cmd spring-boot:run`

API: http://localhost:8080  
H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:stockpulse`)

**Terminal 2 — console**

```bash
cd frontend
npm install
npm run dev
```

UI: http://localhost:5173

## Demo path (inventory-low)

1. Open the console. Organic Cotton T-Shirt (`PRD-003`) is already below threshold (8 / 15).
2. Click **Simulate sale**. The HTTP response returns immediately.
3. Within a few seconds the approval queue shows auto-triggered **pricing** and **reorder** cards with `Inventory low` badges.
4. **Accept** pricing — `currentPrice` updates; price history appears on the detail pane.
5. **Accept** reorder — stock increases (simulated inbound).

## Demand-spike path

Hoodie (`PRD-008`) starts at velocity 15. Simulate several sales until velocity exceeds 3× apparel category average. Queue cards get `Demand spike` badges (idempotent: one PENDING pair per trigger type).

## Runtime strategy switch

`PUT /commerce/strategy` with `{ "strategy": "rule-based" }` or `"ai-based"`. Same control is in the header dropdown. No restart. HTTP and the agentic loop share `CommerceAdvisorRegistry`.

Optional env:

```
LLM_API_KEY=...
LLM_PROVIDER=gemini
LLM_MODEL=gemini-2.0-flash
COMMERCE_STRATEGY=ai-based
```

## Layout

- `/backend` — Spring Boot 3.4, JPA, H2
- `/frontend` — React 18 + Vite
- `ADR.md` — architecture decisions

## Endpoints (brief)

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/products` | Create SKU |
| GET | `/products?status=&category=` | Catalog |
| PATCH | `/products/{id}/stock` | May fire inventory-low loop |
| POST | `/products/{id}/orders` | Simulate sale |
| POST | `/products/{id}/suggest-pricing` | On-demand |
| POST | `/products/{id}/suggest-reorder` | On-demand |
| POST | `/products/{id}/suggest-pricing/stream` | SSE reasoning (bonus) |
| PATCH | `/pricing-suggestions/{id}` | `{ "accept": true\|false }` |
| PATCH | `/reorder-suggestions/{id}` | Accept increments stock |
| GET/PUT | `/commerce/strategy` | Runtime switch |
| GET | `/commerce/dashboard` | Console poll payload |
