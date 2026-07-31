// TICKET-ADV120 — useMemo for portfolio-value calc.
// TICKET-ADV116 — useTradeStream live feed.
import React, { useMemo } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import { useTradeStream } from '@hooks/useTradeStream.js';

function StatCard({ label, value }) {
  return (
    <article className="stat-card">
      <h3>{label}</h3>
      <p>{value}</p>
    </article>
  );
}

function Dashboard() {
  const { trades, isConnected } = useTradeStream();

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

      <div className="stat-grid">
        <StatCard label="Portfolio Value" value={`$${portfolioValue.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}`} />
        <StatCard label="Trades Streamed" value={trades.length} />
        <StatCard label="Matched" value={matched} />
        <StatCard label="Open Breaks" value={breaks} />
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
                  <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>{t.instrument} • {t.qty} @ ${t.price}</div>
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
