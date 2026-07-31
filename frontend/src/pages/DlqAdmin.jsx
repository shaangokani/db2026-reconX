import React, { useState, useEffect } from 'react';
import { api } from '@services/apiService.js';
import { withAuth } from '@components/withAuth.jsx';

function DlqAdmin() {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchMessages = async () => {
    setLoading(true);
    try {
      const data = await api.listDlq();
      setMessages(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMessages();
  }, []);

  const handleReplay = async (eventId) => {
    if (!window.confirm('Are you sure you want to replay this event back to its original topic?')) return;
    try {
      await api.replayDlq(eventId);
      fetchMessages();
    } catch (err) {
      alert(`Replay failed: ${err.message}`);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-4)' }}>
        <h2 style={{ fontSize: '2rem', margin: 0, color: 'var(--color-danger)' }}>Dead Letter Queue</h2>
        <button onClick={fetchMessages}>Refresh Queue</button>
      </div>
      <p style={{ color: 'var(--color-text-muted)', marginBottom: 'var(--space-5)' }}>
        Manage failed Kafka messages that could not be processed after retries.
      </p>

      {error && <div className="error-fallback" style={{ marginBottom: 'var(--space-4)' }}>{error}</div>}

      {loading ? (
        <div className="loader">Loading DLQ...</div>
      ) : messages.length === 0 ? (
        <div className="stat-card" style={{ textAlign: 'center', padding: 'var(--space-5)' }}>
          <h3 style={{ fontSize: '1.5rem', color: 'var(--color-success)', marginBottom: 'var(--space-2)' }}>Queue Empty</h3>
          <p style={{ fontSize: '1rem', color: 'var(--color-text-muted)' }}>There are currently no messages in the DLQ.</p>
        </div>
      ) : (
        <div className="data-table">
          <div className="data-table__header" style={{ gridTemplateColumns: '1.5fr 1fr 2fr 1fr 1fr' }}>
            <div className="data-table__th">Event ID</div>
            <div className="data-table__th">Failed At</div>
            <div className="data-table__th">Reason</div>
            <div className="data-table__th">Original Topic</div>
            <div className="data-table__th">Actions</div>
          </div>
          {messages.map((msg) => (
            <div key={msg.id} className="data-table__row" style={{ gridTemplateColumns: '1.5fr 1fr 2fr 1fr 1fr', alignItems: 'start' }}>
              <div style={{ fontFamily: 'monospace', fontSize: '0.85em', color: 'var(--color-text-muted)' }}>
                {msg.eventId}
              </div>
              <div>{new Date(msg.createdAt).toLocaleString()}</div>
              <div style={{ fontSize: '0.85em', color: 'var(--color-danger)' }}>{msg.reason}</div>
              <div>
                <span style={{ padding: '4px 8px', background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: '4px', fontSize: '0.8em' }}>
                  {msg.originalTopic}
                </span>
              </div>
              <div>
                <button 
                  onClick={() => handleReplay(msg.eventId)} 
                  style={{ padding: '4px 12px', fontSize: '0.8rem', background: 'var(--color-warning)', color: '#000' }}
                >
                  Replay
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default withAuth(DlqAdmin);
