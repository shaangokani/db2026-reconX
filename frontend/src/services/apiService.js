// TICKET-ADV112-related — fetch wrapper that attaches Bearer JWT from sessionStorage.
const BASE = '/api';

function authHeaders() {
  // DONE: TODO(TICKET-ADV112): read 'reconx-token' from sessionStorage and return
  //                     { Authorization: `Bearer <token>` }. Return {} when
  //                     no token is set (login + signup endpoints).
  const token = sessionStorage.getItem('reconx-token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request(method, path, body) {
  // DONE: TODO(TICKET-ADV112): fetch(`${BASE}${path}`, { method, headers, body }).
  //   - headers must include Content-Type: application/json and ...authHeaders()
  //   - serialise `body` via JSON.stringify when present
  //   - on !res.ok throw new Error(`HTTP ${res.status}: ${detail}`)
  //   - status 204 -> return null, otherwise return await res.json()
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    const detail = await res.text().catch(() => '');
    throw new Error(`HTTP ${res.status}: ${detail}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  login: (email, password)   => {
    // TODO(TICKET-ADV072): POST /auth/login with { email, password }.
    return request('POST', '/auth/login', { email, password });
  },
  listTrades: (params = '')  => {
    return request('GET', `/v1/trades${params}`);
  },
  createTrade: (req)         => {
    // yup's date() schema casts the <input type="date"> string into a real
    // Date on validation; the backend's tradeDate is a plain LocalDate and
    // expects yyyy-MM-dd, not a full ISO instant string.
    const tradeDate = req.tradeDate instanceof Date
      ? req.tradeDate.toISOString().slice(0, 10)
      : req.tradeDate;
    return request('POST', '/v1/trades', { ...req, tradeDate });
  },
  updateStatus: (id, status) => {
    return request('PATCH', `/v1/trades/${id}/status`, { status });
  },
  deleteTrade: (id)          => {
    return request('DELETE', `/v1/trades/${id}`);
  },
  runRecon: (req)            => {
    return request('POST', '/v1/recon/run', req);
  },
  reconResults: (jobId)      => {
    return request('GET', `/v1/recon/jobs/${jobId}/results`);
  },
  audit: (tradeRef)          => {
    return request('GET', `/v1/audit/trades/${tradeRef}`);
  },
};
