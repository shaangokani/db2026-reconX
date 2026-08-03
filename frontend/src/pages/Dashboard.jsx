// TICKET-ADV120 — useMemo for portfolio-value calc.
// TICKET-ADV116 — useTradeStream live feed.
import React, { useMemo, useState, useEffect, useCallback } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import { useTradeStream } from '@hooks/useTradeStream.js';
import { useAuth } from '@context/AuthContext.jsx';
import { api } from '@services/apiService.js';

const STATUS_COLOUR = {
  MATCHED:   'var(--color-success)',
  PENDING:   'var(--color-warning)',
  UNMATCHED: 'var(--color-danger)',
  DISPUTED:  'var(--color-primary)',
  CANCELLED: 'var(--color-text-muted)',
};

function StatCard({ label, value, hint }) {
  return (
    <article className="stat-card">
      <h3>{label}</h3>
      <p>{value}</p>
      {hint && (
        <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>{hint}</span>
      )}
    </article>
  );
}

function Panel({ title, subtitle, children, style }) {
  return (
    <div style={{
      background: 'var(--color-surface)', borderRadius: 'var(--radius)',
      padding: 'var(--space-4)', border: '1px solid var(--color-border)',
      boxShadow: '0 4px 6px -1px var(--color-shadow)', ...style,
    }}>
      <h3 style={{ margin: 0, fontSize: '1.25rem' }}>{title}</h3>
      {subtitle && (
        <p style={{ margin: '2px 0 var(--space-4)', fontSize: '0.78rem', color: 'var(--color-text-muted)' }}>
          {subtitle}
        </p>
      )}
      {!subtitle && <div style={{ height: 'var(--space-4)' }} />}
      {children}
    </div>
  );
}

function Dashboard() {
  const { trades, isConnected } = useTradeStream();
  const { user } = useAuth();
  // Mirrors SecurityConfig: /v1/admin/** is ADMIN, /v1/recon/** is
  // RECON_ANALYST or ADMIN, /v1/trades/** is any signed-in role.
  const isAdmin = user?.role === 'ADMIN';
  const canSeeRecon = isAdmin || user?.role === 'RECON_ANALYST';

  // Whole-book figures, polled from the REST API. Everything derived from
  // `trades` below is SSE-only and therefore session-scoped, so these are kept
  // visually separate rather than mixed into the same row.
  const [book, setBook] = useState({ summary: null, breaks: [], dlq: null, error: null });

  const refresh = useCallback(async () => {
    try {
      // Only request what this role is allowed to see. A 401 is treated by the
      // api layer as an expired session and signs the user out, so calling a
      // forbidden endpoint here would boot a TRADER out of the app on load.
      const [summary, breaks, dlq] = await Promise.all([
        api.tradeSummary(),
        canSeeRecon ? api.listReconBreaks() : Promise.resolve(null),
        isAdmin ? api.listDlq() : Promise.resolve(null),
      ]);
      setBook({
        summary,
        breaks: breaks ?? [],
        dlq: dlq ? dlq.length : null,
        error: null,
      });
    } catch (e) {
      setBook((b) => ({ ...b, error: e.message }));
    }
  }, [isAdmin, canSeeRecon]);

  useEffect(() => {
    let alive = true;
    const tick = () => { if (alive) refresh(); };
    tick();
    const id = setInterval(tick, 8000);
    return () => { alive = false; clearInterval(id); };
  }, [refresh]);

  const byStatus = book.summary?.byStatus ?? {};
  const bookTotal = book.summary?.total ?? 0;
  const openBreaks = book.breaks.filter((b) => b.status === 'OPEN').length;
  const bookMatchRate = bookTotal ? Math.round(((byStatus.MATCHED ?? 0) / bookTotal) * 100) : 0;

  const breaksByType = useMemo(() => {
    const counts = {};
    for (const b of book.breaks) {
      const k = b.discrepancyType || 'UNKNOWN';
      counts[k] = (counts[k] || 0) + 1;
    }
    return Object.entries(counts).sort((a, b) => b[1] - a[1]);
  }, [book.breaks]);

  // TICKET-ADV120: use useMemo to compute `portfolioValue`
  const portfolioValue = useMemo(() => {
    return trades.reduce((sum, t) => sum + (t.qty || 0) * (t.price || 0), 0);
  }, [trades]);

  // TICKET-ADV120: derive `matched` and `breaks` counts.
  const matched = useMemo(() => trades.filter(t => t.status === 'MATCHED').length, [trades]);
  const breaks = useMemo(() => trades.filter(t => ['UNMATCHED', 'DISPUTED'].includes(t.status)).length, [trades]);

  return (
    <section>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-4)' }}>
        <h2 style={{ fontSize: '2rem', margin: 0 }}>Dashboard</h2>
        <div 
          role="status" 
          aria-live="polite" 
          style={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: '8px',
            padding: '4px 12px',
            borderRadius: '999px',
            background: isConnected ? 'hsla(142, 71%, 45%, 0.1)' : 'hsla(348, 83%, 47%, 0.1)',
            color: isConnected ? 'var(--color-success)' : 'var(--color-danger)',
            fontWeight: 600,
            fontSize: '0.85rem'
          }}
        >
          <span style={{ 
            display: 'block', 
            width: '8px', 
            height: '8px', 
            borderRadius: '50%', 
            background: isConnected ? 'var(--color-success)' : 'var(--color-danger)',
            boxShadow: isConnected ? '0 0 8px var(--color-success)' : '0 0 8px var(--color-danger)',
            animation: isConnected ? 'pulse 2s infinite' : 'none'
          }}></span>
          {isConnected ? 'LIVE' : 'DISCONNECTED'}
        </div>
      </div>

      {book.error && (
        <div role="alert" className="form-error" style={{ marginBottom: 'var(--space-4)' }}>
          Could not load book totals: {book.error}
        </div>
      )}

      <h3 style={{ margin: '0 0 var(--space-2)', fontSize: '0.85rem', letterSpacing: '0.06em',
                   textTransform: 'uppercase', color: 'var(--color-text-muted)' }}>
        Whole book
      </h3>
      <div className="stat-grid">
        <StatCard label="Trades in Book" value={bookTotal.toLocaleString()} hint="all time, excludes deleted rows" />
        <StatCard
          label="Open Recon Breaks"
          value={canSeeRecon ? openBreaks : '—'}
          hint={canSeeRecon ? `${book.breaks.length} total incl. resolved` : 'needs recon analyst or admin'}
        />
        <StatCard label="Match Rate" value={`${bookMatchRate}%`} hint="matched ÷ all trades" />
        <StatCard
          label="DLQ Parked"
          value={isAdmin ? (book.dlq ?? '—') : '—'}
          hint={isAdmin ? 'messages awaiting replay' : 'needs admin'}
        />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-5)', marginTop: 'var(--space-5)' }}>
        <Panel title="Trades by Status" subtitle="whole book, refreshed every 8s">
          {bookTotal === 0 ? (
            <p style={{ color: 'var(--color-text-muted)' }}>No trades yet.</p>
          ) : (
            <>
              <div style={{ display: 'flex', height: '14px', borderRadius: '7px', overflow: 'hidden', marginBottom: 'var(--space-4)' }}>
                {Object.entries(byStatus).filter(([, n]) => n > 0).map(([s, n]) => (
                  <div
                    key={s}
                    title={`${s}: ${n}`}
                    style={{ width: `${(n / bookTotal) * 100}%`, background: STATUS_COLOUR[s] || 'var(--color-text-muted)', transition: 'width 0.5s ease' }}
                  />
                ))}
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
                {Object.entries(byStatus).map(([s, n]) => (
                  <div key={s} style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)', fontSize: '0.9rem' }}>
                    <span style={{ width: '10px', height: '10px', borderRadius: '2px', background: STATUS_COLOUR[s] || 'var(--color-text-muted)', flexShrink: 0 }} />
                    <span style={{ flex: 1 }}>{s}</span>
                    <span style={{ fontWeight: 600 }}>{n.toLocaleString()}</span>
                    <span style={{ width: '48px', textAlign: 'right', color: 'var(--color-text-muted)' }}>
                      {bookTotal ? Math.round((n / bookTotal) * 100) : 0}%
                    </span>
                  </div>
                ))}
              </div>
            </>
          )}
        </Panel>

        <Panel title="Recon Breaks by Type" subtitle="grouped by discrepancy">
          {!canSeeRecon ? (
            <p style={{ color: 'var(--color-text-muted)' }}>
              Requires the recon analyst or admin role.
            </p>
          ) : breaksByType.length === 0 ? (
            <p style={{ color: 'var(--color-text-muted)' }}>No breaks recorded.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
              {breaksByType.map(([type, n]) => (
                <div key={type}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px', fontSize: '0.85rem' }}>
                    <span>{type}</span>
                    <span style={{ fontWeight: 600 }}>{n}</span>
                  </div>
                  <div style={{ height: '8px', background: 'var(--color-bg)', borderRadius: '4px', overflow: 'hidden' }}>
                    <div style={{ height: '100%', background: 'var(--color-danger)', transition: 'width 0.5s ease',
                                  width: `${(n / Math.max(...breaksByType.map((x) => x[1]))) * 100}%` }} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </Panel>
      </div>

      <h3 style={{ margin: 'var(--space-5) 0 var(--space-2)', fontSize: '0.85rem', letterSpacing: '0.06em',
                   textTransform: 'uppercase', color: 'var(--color-text-muted)' }}>
        This session <span style={{ textTransform: 'none', letterSpacing: 0 }}>— live feed only, resets on refresh</span>
      </h3>
      <div className="stat-grid">
        <StatCard label="Streamed Value" value={`$${portfolioValue.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}`} />
        <StatCard label="Trades Streamed" value={trades.length} />
        <StatCard label="Matched" value={matched} />
        <StatCard label="Breaks" value={breaks} />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 300px', gap: 'var(--space-5)', marginTop: 'var(--space-5)' }}>
        <div style={{ background: 'var(--color-surface)', borderRadius: 'var(--radius)', padding: 'var(--space-4)', border: '1px solid var(--color-border)', boxShadow: '0 4px 6px -1px var(--color-shadow)' }}>
          <h3 style={{ margin: '0 0 var(--space-4) 0', fontSize: '1.25rem' }}>Live Trade Feed</h3>
          <div style={{ height: '300px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
            {trades.slice(-20).reverse().map((t, i) => (
              <div key={i} style={{ 
                display: 'flex', 
                justifyContent: 'space-between', 
                padding: 'var(--space-3)', 
                background: 'var(--color-bg)', 
                borderRadius: 'var(--radius-sm)',
                borderLeft: `4px solid ${t.status === 'MATCHED' ? 'var(--color-success)' : t.status === 'PENDING' ? 'var(--color-warning)' : 'var(--color-danger)'}`,
                animation: 'slideIn 0.3s ease-out'
              }}>
                <div>
                  <div style={{ fontWeight: 600 }}>{t.tradeRef || 'N/A'}</div>
                  {/* the SSE payload field is `symbol` — `instrument` rendered as undefined */}
                  <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>{t.symbol} • {t.qty} @ ${t.price}</div>
                </div>
                <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>
                  {t.status}
                </div>
              </div>
            ))}
            {trades.length === 0 && <div style={{ color: 'var(--color-text-muted)', textAlign: 'center', marginTop: 'var(--space-5)' }}>Waiting for trades...</div>}
          </div>
        </div>

        <div style={{ background: 'var(--color-surface)', borderRadius: 'var(--radius)', padding: 'var(--space-4)', border: '1px solid var(--color-border)', boxShadow: '0 4px 6px -1px var(--color-shadow)' }}>
          <h3 style={{ margin: '0 0 var(--space-4) 0', fontSize: '1.25rem' }}>Health Overview</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px', fontSize: '0.85rem' }}>
                <span>Match Rate</span>
                <span style={{ fontWeight: 600 }}>{trades.length ? Math.round((matched / trades.length) * 100) : 0}%</span>
              </div>
              <div style={{ height: '8px', background: 'var(--color-bg)', borderRadius: '4px', overflow: 'hidden' }}>
                <div style={{ height: '100%', background: 'var(--color-success)', width: `${trades.length ? (matched / trades.length) * 100 : 0}%`, transition: 'width 0.5s ease' }}></div>
              </div>
            </div>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px', fontSize: '0.85rem' }}>
                <span>Break Rate</span>
                <span style={{ fontWeight: 600 }}>{trades.length ? Math.round((breaks / trades.length) * 100) : 0}%</span>
              </div>
              <div style={{ height: '8px', background: 'var(--color-bg)', borderRadius: '4px', overflow: 'hidden' }}>
                <div style={{ height: '100%', background: 'var(--color-danger)', width: `${trades.length ? (breaks / trades.length) * 100 : 0}%`, transition: 'width 0.5s ease' }}></div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <style>{`
        @keyframes pulse {
          0% { box-shadow: 0 0 0 0 hsla(142, 71%, 45%, 0.4); }
          70% { box-shadow: 0 0 0 10px hsla(142, 71%, 45%, 0); }
          100% { box-shadow: 0 0 0 0 hsla(142, 71%, 45%, 0); }
        }
        @keyframes slideIn {
          from { opacity: 0; transform: translateY(-10px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </section>
  );
}

export default withAuth(Dashboard);
