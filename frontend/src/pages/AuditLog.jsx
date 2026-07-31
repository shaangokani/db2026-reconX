import React, { useState } from 'react';
import { api } from '@services/apiService.js';
import { useAuth } from '@context/AuthContext.jsx';

export default function AuditLog() {
  const [tradeRef, setTradeRef] = useState('');
  const [events, setEvents] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!tradeRef) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.listAuditEvents(tradeRef);
      setEvents(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2 style={{ fontSize: '2rem', marginBottom: 'var(--space-4)' }}>Audit Log</h2>
      <p style={{ color: 'var(--color-text-muted)', marginBottom: 'var(--space-5)' }}>
        View the immutable, event-sourced history of a trade from the Kafka audit topic.
      </p>

      <form onSubmit={handleSearch} style={{ display: 'flex', gap: 'var(--space-3)', marginBottom: 'var(--space-5)', maxWidth: '500px' }}>
        <input
          type="text"
          value={tradeRef}
          onChange={(e) => setTradeRef(e.target.value)}
          placeholder="Enter Trade Reference (e.g., TRD-12345)"
          style={{ flex: 1, padding: '0.75rem 1rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--color-border)', background: 'var(--color-surface)', color: 'var(--color-text)' }}
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Searching...' : 'Search'}
        </button>
      </form>

      {error && <div className="error-fallback">{error}</div>}

      {events.length > 0 && (
        <div className="data-table">
          <div className="data-table__header" style={{ gridTemplateColumns: '1fr 2fr 1fr 1fr 1fr' }}>
            <div className="data-table__th">Time</div>
            <div className="data-table__th">Event ID</div>
            <div className="data-table__th">Type</div>
            <div className="data-table__th">Actor</div>
            <div className="data-table__th">State Change</div>
          </div>
          {events.map((evt) => (
            <div key={evt.eventId} className="data-table__row" style={{ gridTemplateColumns: '1fr 2fr 1fr 1fr 1fr' }}>
              <div>{new Date(evt.timestamp).toLocaleString()}</div>
              <div style={{ fontFamily: 'monospace', fontSize: '0.85em', color: 'var(--color-text-muted)' }}>{evt.eventId}</div>
              <div>
                <span style={{ padding: '4px 8px', background: 'var(--color-primary-transparent)', color: 'var(--color-primary)', borderRadius: '4px', fontSize: '0.8em', fontWeight: 600 }}>
                  {evt.eventType}
                </span>
              </div>
              <div>{evt.actor}</div>
              <div>
                {evt.previousState ? `${evt.previousState} ➔ ${evt.newState}` : evt.newState}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
