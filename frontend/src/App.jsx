import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from './api'

const CATEGORIES = ['ALL', 'ELECTRONICS', 'APPAREL', 'HOME']

function money(n) {
  return `$${Number(n).toFixed(2)}`
}

function heat(product) {
  if (product.stockLevel === 0) return 'heat-out'
  if (product.stockLevel < product.reorderThreshold) return 'heat-low'
  if (product.stockLevel < product.reorderThreshold * 1.5) return 'heat-mid'
  return 'heat-ok'
}

function triggerLabel(reason) {
  if (reason === 'INVENTORY_LOW') return 'Inventory low'
  if (reason === 'DEMAND_SPIKE') return 'Demand spike'
  if (reason === 'MANUAL') return 'Manual'
  return reason
}

export default function App() {
  const [dashboard, setDashboard] = useState(null)
  const [category, setCategory] = useState('ALL')
  const [selected, setSelected] = useState('PRD-003')
  const [history, setHistory] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [streamText, setStreamText] = useState('')
  const [loading, setLoading] = useState(true)

  const load = useCallback(async (silent = false) => {
    try {
      if (!silent) setLoading(true)
      const data = await api.dashboard()
      setDashboard(data)
      setError('')
    } catch (e) {
      setError(e.message || 'Backend unreachable. Start Spring Boot on :8080.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    const t = setInterval(() => load(true), 3000)
    return () => clearInterval(t)
  }, [load])

  useEffect(() => {
    if (!selected) return
    api.priceHistory(selected).then(setHistory).catch(() => setHistory([]))
  }, [selected, dashboard])

  const products = dashboard?.products || []
  const pendingPricing = dashboard?.pendingPricing || []
  const pendingReorder = dashboard?.pendingReorder || []
  const strategy = dashboard?.strategy?.active || 'rule-based'

  const visible = useMemo(
    () => products.filter((p) => category === 'ALL' || p.category === category),
    [products, category]
  )

  const selectedProduct = products.find((p) => p.id === selected) || visible[0]

  async function run(fn) {
    setBusy(true)
    setError('')
    try {
      await fn()
      await load(true)
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  const pendingCount = pendingPricing.length + pendingReorder.length

  return (
    <div className="page">
      <header className="masthead">
        <div>
          <div className="kicker">ShopStream · Merchandising console</div>
          <h1>Stock<em>Pulse</em></h1>
          <p className="lede">
            Inventory signals queue price and reorder advice. Merchandising publishes — the system never writes a live price on its own.
          </p>
        </div>
        <div className="mast-meta">
          <label>
            Engine
            <select
              value={strategy}
              disabled={busy}
              onChange={(e) => run(() => api.setStrategy(e.target.value))}
            >
              {(dashboard?.strategy?.available || ['rule-based', 'ai-based']).map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </label>
          <button className="ghost" onClick={() => load()} disabled={loading}>
            {loading ? 'Refreshing' : 'Refresh'}
          </button>
        </div>
      </header>

      {error && <div className="banner error">{error}</div>}

      <div className="score-strip">
        <div className="score-cell">
          <div className="score-num">{products.length}</div>
          <div className="score-label">SKUs</div>
        </div>
        <div className="score-cell">
          <div className="score-num accent">{pendingCount}</div>
          <div className="score-label">Pending advice</div>
        </div>
        <div className="score-cell">
          <div className="score-num teal">{products.filter((p) => p.stockLevel < p.reorderThreshold).length}</div>
          <div className="score-label">Below threshold</div>
        </div>
        <div className="score-cell">
          <div className="score-num">{strategy}</div>
          <div className="score-label">Active strategy</div>
        </div>
      </div>

      <div className="layout">
        <section className="board">
          <div className="section-rule">
            <span>Catalog board</span>
            <div className="line" />
            <div className="filters">
              {CATEGORIES.map((c) => (
                <button key={c} className={category === c ? 'pill on' : 'pill'} onClick={() => setCategory(c)}>
                  {c}
                </button>
              ))}
            </div>
          </div>

          <div className="heatmap">
            {visible.map((p) => (
              <button
                key={p.id}
                className={`sku ${heat(p)} ${selectedProduct?.id === p.id ? 'selected' : ''}`}
                onClick={() => setSelected(p.id)}
              >
                <span className="sku-name">{p.name}</span>
                <span className="sku-meta">{p.category} · {p.stockLevel} units</span>
              </button>
            ))}
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Stock</th>
                  <th>Price</th>
                  <th>Margin</th>
                  <th>Velocity</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {visible.map((p) => {
                  const margin = p.costPrice != null
                    ? ((p.currentPrice - p.costPrice) / p.currentPrice) * 100
                    : null
                  return (
                    <tr key={p.id} className={selectedProduct?.id === p.id ? 'row-on' : ''} onClick={() => setSelected(p.id)}>
                      <td>
                        <strong>{p.name}</strong>
                        <div className="muted">{p.sku}</div>
                      </td>
                      <td className={p.stockLevel < p.reorderThreshold ? 'warn' : ''}>
                        {p.stockLevel} <span className="muted">/ {p.reorderThreshold}</span>
                      </td>
                      <td>{money(p.currentPrice)}</td>
                      <td>{margin == null ? '—' : `${margin.toFixed(0)}%`}</td>
                      <td>{p.demandVelocity}</td>
                      <td><span className={`badge ${p.status}`}>{p.status.replaceAll('_', ' ')}</span></td>
                      <td>
                        <button
                          className="tiny"
                          disabled={busy || p.stockLevel < 1}
                          onClick={(e) => {
                            e.stopPropagation()
                            run(() => api.order(p.id, 1))
                          }}
                        >
                          Simulate sale
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </section>

        <aside className="queue">
          <div className="section-rule"><span>Approval queue</span><div className="line" /></div>
          {pendingCount === 0 && (
            <p className="empty">No pending suggestions. Simulate a sale on Organic Cotton T-Shirt (PRD-003) to fire the inventory-low loop.</p>
          )}
          {pendingPricing.map((s) => (
            <article key={s.id} className="advice price">
              <div className="advice-top">
                <span>Pricing</span>
                <span className={`badge trigger ${s.triggerReason}`}>{triggerLabel(s.triggerReason)}</span>
              </div>
              <h3>{s.product?.name}</h3>
              <div className="delta">
                <s>{money(s.currentPrice)}</s>
                <span>{money(s.recommendedPrice)}</span>
                <em>{s.direction}</em>
              </div>
              <p className="reason">{s.reasoning}</p>
              <div className="conf">
                Confidence {(s.confidence * 100).toFixed(0)}%
                <i style={{ width: `${s.confidence * 100}%` }} />
              </div>
              <div className="actions">
                <button className="ok" disabled={busy} onClick={() => run(() => api.resolvePricing(s.id, true))}>Accept</button>
                <button className="no" disabled={busy} onClick={() => run(() => api.resolvePricing(s.id, false))}>Reject</button>
              </div>
            </article>
          ))}
          {pendingReorder.map((s) => (
            <article key={s.id} className="advice reorder">
              <div className="advice-top">
                <span>Reorder</span>
                <span className={`badge trigger ${s.triggerReason}`}>{triggerLabel(s.triggerReason)}</span>
              </div>
              <h3>{s.product?.name}</h3>
              <p className="delta">Inbound <strong>{s.recommendedQuantity}</strong> units · {s.suggestedLeadTimeDays || 7}d lead</p>
              <p className="reason">{s.reasoning}</p>
              <div className="conf">
                Confidence {(s.confidence * 100).toFixed(0)}%
                <i style={{ width: `${s.confidence * 100}%` }} />
              </div>
              <div className="actions">
                <button className="ok" disabled={busy} onClick={() => run(() => api.resolveReorder(s.id, true))}>Accept inbound</button>
                <button className="no" disabled={busy} onClick={() => run(() => api.resolveReorder(s.id, false))}>Reject</button>
              </div>
            </article>
          ))}
        </aside>
      </div>

      {selectedProduct && (
        <section className="detail">
          <div className="section-rule"><span>{selectedProduct.name}</span><div className="line" /></div>
          <div className="detail-grid">
            <div>
              <p className="muted">{selectedProduct.id} · {selectedProduct.sku} · {selectedProduct.category}</p>
              <p>Stock {selectedProduct.stockLevel} · threshold {selectedProduct.reorderThreshold} · velocity {selectedProduct.demandVelocity}</p>
              <p>Live price {money(selectedProduct.currentPrice)}
                {selectedProduct.costPrice != null && ` · cost ${money(selectedProduct.costPrice)}`}
              </p>
              <div className="actions">
                <button disabled={busy} onClick={() => run(() => api.order(selectedProduct.id, 1))}>Simulate sale</button>
                <button disabled={busy} onClick={() => run(() => api.suggestPricing(selectedProduct.id))}>Suggest price</button>
                <button disabled={busy} onClick={() => run(() => api.suggestReorder(selectedProduct.id))}>Suggest reorder</button>
                <button
                  disabled={busy}
                  onClick={() => run(async () => {
                    setStreamText('')
                    await api.streamPricing(selectedProduct.id, (token) => {
                      setStreamText((prev) => (prev ? `${prev} ${token}` : token))
                    })
                  })}
                >
                  Stream reasoning
                </button>
              </div>
              {streamText && <p className="stream">{streamText}</p>}
            </div>
            <div>
              <div className="muted">Price history</div>
              <div className="bars">
                {history.length === 0 && <span className="muted">No accepted changes yet</span>}
                {history.map((h) => {
                  const max = Math.max(...history.map((x) => Number(x.newPrice)), Number(selectedProduct.currentPrice))
                  return (
                    <div key={h.id} className="bar-row">
                      <span>{money(h.oldPrice)} → {money(h.newPrice)}</span>
                      <b style={{ width: `${(Number(h.newPrice) / max) * 100}%` }} />
                    </div>
                  )
                })}
              </div>
            </div>
          </div>
        </section>
      )}
    </div>
  )
}
