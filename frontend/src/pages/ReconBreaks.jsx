import React, { useState, useEffect } from 'react';
import { api } from '@services/apiService.js';
import { withAuth } from '@components/withAuth.jsx';

function ReconBreaks() {
  const [breaks, setBreaks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchBreaks = async () => {
    try {
      const data = await api.listReconBreaks();
      setBreaks(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBreaks();
  }, []);

  const handleResolve = async (id) => {
    const note = prompt('Resolution note:');
    if (note === null) return;
    try {
      await api.resolveReconBreak(id, note);
      fetchBreaks();
    } catch (err) {
      alert(`Failed to resolve: ${err.message}`);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-4)' }}>
        <h2 style={{ fontSize: '2rem', margin: 0 }}>Reconciliation Breaks</h2>
        <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
          <div className="stat-card" style={{ margin: 0, padding: 'var(--space-2) var(--space-4)' }}>
            <h3 style={{ fontSize: '0.75rem' }}>Active Breaks</h3>
            <p style={{ fontSize: '1.5rem', margin: 0 }}>{breaks.filter(b => b.status === 'OPEN').length}</p>
          </div>
          <button onClick={fetchBreaks}>Refresh</button>
        </div>
      </div>

      <p style={{ color: 'var(--color-text-muted)', marginBottom: 'var(--space-5)' }}>
        Review and resolve discrepancies identified during the latest reconciliation run.
      </p>

      {error && <div className="error-fallback" style={{ marginBottom: 'var(--space-4)' }}>{error}</div>}

      {loading ? (
        <div className="loader">Loading breaks...</div>
      ) : breaks.length === 0 ? (
        <div className="stat-card" style={{ textAlign: 'center', padding: 'var(--space-5)' }}>
          <h3 style={{ fontSize: '1.5rem', color: 'var(--color-success)', marginBottom: 'var(--space-2)' }}>All Clear!</h3>
          <p style={{ fontSize: '1rem', color: 'var(--color-text-muted)' }}>No reconciliation breaks found in the latest job.</p>
        </div>
      ) : (
        <div className="data-table">
          <div className="data-table__header" style={{ gridTemplateColumns: '1.5fr 1fr 1fr 2fr 1fr' }}>
            <div className="data-table__th">Trade Ref</div>
            <div className="data-table__th">Discrepancy</div>
            <div className="data-table__th">Status</div>
            <div className="data-table__th">Details</div>
            <div className="data-table__th">Actions</div>
          </div>
          {breaks.map((b) => (
            <div key={b.id} className="data-table__row" style={{ gridTemplateColumns: '1.5fr 1fr 1fr 2fr 1fr' }}>
              <div style={{ fontWeight: 600 }}>{b.tradeRef}</div>
              <div>
                <span style={{ padding: '4px 8px', background: 'var(--color-warning)', color: '#000', borderRadius: '4px', fontSize: '0.8em', fontWeight: 600 }}>
                  {b.discrepancyType}
                </span>
              </div>
              <div>
                <span style={{ 
                  padding: '4px 8px', 
                  background: b.status === 'RESOLVED' ? 'var(--color-success)' : 'var(--color-danger)', 
                  color: '#fff', 
                  borderRadius: '4px', 
                  fontSize: '0.8em', 
                  fontWeight: 600 
                }}>
                  {b.status}
                </span>
              </div>
              <div style={{ fontSize: '0.85em', color: 'var(--color-text-muted)' }}>{b.details}</div>
              <div>
                {b.status === 'OPEN' && (
                  <button onClick={() => handleResolve(b.id)} style={{ padding: '4px 12px', fontSize: '0.8rem' }}>
                    Resolve
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default withAuth(ReconBreaks);
