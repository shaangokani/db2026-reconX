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
      <h2>Dashboard</h2>
      <div className="stat-grid">
        <StatCard label="Portfolio Value" value={`$${portfolioValue.toFixed(2)}`} />
        <StatCard label="Trades Streamed" value={trades.length} />
        <StatCard label="Matched" value={matched} />
        <StatCard label="Open Breaks" value={breaks} />
      </div>
      <div role="status" aria-live="polite">
        SSE: {isConnected ? 'connected' : 'disconnected'}
      </div>
    </section>
  );
}

export default withAuth(Dashboard);
