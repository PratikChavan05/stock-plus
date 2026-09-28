const API = ''

async function request(path, options = {}) {
  const res = await fetch(`${API}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  })
  if (!res.ok) {
    let message = res.statusText
    try {
      const body = await res.json()
      message = body.error || message
    } catch {
      // ignore
    }
    throw new Error(message)
  }
  if (res.status === 204) return null
  const text = await res.text()
  return text ? JSON.parse(text) : null
}

export const api = {
  dashboard: () => request('/commerce/dashboard'),
  products: (params = {}) => {
    const q = new URLSearchParams()
    if (params.status) q.set('status', params.status)
    if (params.category) q.set('category', params.category)
    const suffix = q.toString() ? `?${q}` : ''
    return request(`/products${suffix}`)
  },
  order: (id, quantity = 1) =>
    request(`/products/${id}/orders`, { method: 'POST', body: JSON.stringify({ quantity }) }),
  setStock: (id, stockLevel) =>
    request(`/products/${id}/stock`, { method: 'PATCH', body: JSON.stringify({ stockLevel }) }),
  suggestPricing: (id) => request(`/products/${id}/suggest-pricing`, { method: 'POST' }),
  suggestReorder: (id) => request(`/products/${id}/suggest-reorder`, { method: 'POST' }),
  resolvePricing: (id, accept) =>
    request(`/pricing-suggestions/${id}`, { method: 'PATCH', body: JSON.stringify({ accept }) }),
  resolveReorder: (id, accept) =>
    request(`/reorder-suggestions/${id}`, { method: 'PATCH', body: JSON.stringify({ accept }) }),
  setStrategy: (strategy) =>
    request('/commerce/strategy', { method: 'PUT', body: JSON.stringify({ strategy }) }),
  priceHistory: (id) => request(`/products/${id}/price-history`),
  streamPricing: async (id, onToken) => {
    const res = await fetch(`/products/${id}/suggest-pricing/stream`, { method: 'POST' })
    if (!res.ok || !res.body) throw new Error('Stream failed')
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let suggestion = null
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const chunks = buffer.split('\n\n')
      buffer = chunks.pop() || ''
      for (const chunk of chunks) {
        const event = chunk.match(/^event: (.+)$/m)?.[1]
        const data = chunk.match(/^data: ([\s\S]+)$/m)?.[1]
        if (event === 'token' && data) onToken(data)
        if (event === 'suggestion' && data) suggestion = JSON.parse(data)
      }
    }
    return suggestion
  }
}
