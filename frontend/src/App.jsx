// TICKET-ADV122 — Lazy + Suspense for route-based code splitting
import React, { Suspense, lazy } from 'react';
import { Routes, Route, Link, Navigate } from 'react-router-dom';
import { withErrorBoundary } from '@components/withErrorBoundary.jsx';
import { useTheme } from '@context/ThemeContext.jsx';
import { useAuth } from '@context/AuthContext.jsx';

const Dashboard   = lazy(() => import('@pages/Dashboard.jsx'));
const Trades      = lazy(() => import('@pages/Trades.jsx'));
const AddTrade    = lazy(() => import('@pages/AddTrade.jsx'));
const Login       = lazy(() => import('@pages/Login.jsx'));
const AuditLog    = lazy(() => import('@pages/AuditLog.jsx'));
const ReconBreaks = lazy(() => import('@pages/ReconBreaks.jsx'));
const DlqAdmin    = lazy(() => import('@pages/DlqAdmin.jsx'));

function App() {
  const { theme, toggle } = useTheme();
  const { user, logout } = useAuth();

  return (
    <div className="layout">
      {user && (
        <aside className="layout__sidebar">
          <h1>ReconX</h1>
          <nav className="layout__nav">
            <Link to="/">Dashboard</Link>
            <Link to="/recon">Recon Breaks</Link>
            <Link to="/audit">Audit Log</Link>
            <Link to="/dlq">DLQ Admin</Link>
            <Link to="/trades">Trades</Link>
            <Link to="/trades/new">Add trade</Link>
          </nav>
          {/* Account/appearance controls live in their own footer rather than
              among the nav links — and grouping them fixes the two competing
              `margin-top: auto` rules that were splitting them apart. */}
          <div className="layout__footer">
            <button type="button" className="nav__signout" onClick={logout}>
              Sign out{user.role ? ` (${user.role})` : ''}
            </button>
            <button
              role="switch"
              aria-checked={theme === 'dark'}
              className="theme-switch"
              onClick={toggle}
              aria-label="Toggle dark mode"
            >
              <span className="theme-switch__thumb">
                {theme === 'dark' ? '🌙' : '☀️'}
              </span>
            </button>
          </div>
        </aside>
      )}
      <main className="layout__main">
        <Suspense fallback={<div className="loader">Loading…</div>}>
          <Routes>
            <Route path="/login"      element={<Login />} />
            <Route path="/"           element={<Dashboard />} />
            <Route path="/recon"      element={<ReconBreaks />} />
            <Route path="/audit"      element={<AuditLog />} />
            <Route path="/dlq"        element={<DlqAdmin />} />
            <Route path="/trades"     element={<Trades />} />
            <Route path="/trades/new" element={<AddTrade />} />
            <Route path="*"           element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </main>
    </div>
  );
}

export default withErrorBoundary(App);
