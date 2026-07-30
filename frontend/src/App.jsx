// TICKET-ADV122 — Lazy + Suspense for route-based code splitting
import React, { Suspense, lazy } from 'react';
import { Routes, Route, Link, Navigate } from 'react-router-dom';
import { withErrorBoundary } from '@components/withErrorBoundary.jsx';
import { useTheme } from '@context/ThemeContext.jsx';
import { useAuth } from '@context/AuthContext.jsx';

const Dashboard = lazy(() => import('@pages/Dashboard.jsx'));
const Trades    = lazy(() => import('@pages/Trades.jsx'));
const AddTrade  = lazy(() => import('@pages/AddTrade.jsx'));
const Login     = lazy(() => import('@pages/Login.jsx'));

function App() {
  const { theme, toggle } = useTheme();
  const { user } = useAuth();

  return (
    <div className="layout">
      {user && (
        <header className="layout__header">
          <h1>ReconX</h1>
          <nav className="layout__nav">
            <Link to="/">Dashboard</Link>
            <Link to="/trades">Trades</Link>
            <Link to="/trades/new">Add trade</Link>
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
          </nav>
        </header>
      )}
      <main className="layout__main">
        <Suspense fallback={<div className="loader">Loading…</div>}>
          <Routes>
            <Route path="/login"      element={<Login />} />
            <Route path="/"           element={<Dashboard />} />
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
