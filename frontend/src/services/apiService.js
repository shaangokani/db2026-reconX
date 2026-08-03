// TICKET-ADV112-related — fetch wrapper that attaches Bearer JWT from sessionStorage.
const BASE = '/api';

function authHeaders() {
  // DONE: TODO(TICKET-ADV112): read 'reconx-token' from sessionStorage and return
  //                     { Authorization: `Bearer <token>` }. Return {} when
  //                     no token is set (login + signup endpoints).
  const token = sessionStorage.getItem('reconx-token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

/**
 * The token is only valid for an hour and the backend answers 401 for both an
 * expired and an absent one. Without clearing it here, an expired session keeps
 * the UI looking signed-in while every request fails — so a 401 on an
 * authenticated call ends the session and sends the user back to /login.
 * A 401 on the login call itself is just a wrong password: let it through so
 * the form can show the error instead of reloading the page.
 */
function handleUnauthorized(sentAuth) {
  if (!sentAuth) return;
  sessionStorage.removeItem('reconx-token');
  sessionStorage.removeItem('reconx-role');
  if (window.location.pathname !== '/login') window.location.assign('/login');
}

async function request(method, path, body) {
  // DONE: TODO(TICKET-ADV112): fetch(`${BASE}${path}`, { method, headers, body }).
  //   - headers must include Content-Type: application/json and ...authHeaders()
  //   - serialise `body` via JSON.stringify when present
  //   - on !res.ok throw new Error(`HTTP ${res.status}: ${detail}`)
  //   - status 204 -> return null, otherwise return await res.json()
  const auth = authHeaders();
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', ...auth },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    if (res.status === 401) handleUnauthorized(Boolean(auth.Authorization));
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
  // Whole-book counts by status. The dashboard's other numbers come from the
  // SSE feed and only cover the current session, so this is what gives it any
  // sense of the book as a whole.
  tradeSummary: ()           => {
    return request('GET', '/v1/trades/summary');
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
  listAuditEvents: (tradeRef) => {
    return request('GET', `/v1/audit/trades/${tradeRef}/events`);
  },
  listReconBreaks: () => {
    return request('GET', '/v1/recon/jobs/latest/results');
  },
  resolveReconBreak: (id, note) => {
    return request('PUT', `/v1/recon/results/${id}/resolve`, { note });
  },
  listDlq: () => {
    return request('GET', '/v1/admin/dlq');
  },
  replayDlq: (eventId) => {
    return request('POST', `/v1/admin/dlq/replay?eventId=${eventId}`);
  },
};
