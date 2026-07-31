// TICKET-ADV112 — AuthContext used by withAuth HOC; JWT persisted in memory
// (refresh path lives in HttpOnly cookie — out of scope for this trainer copy).
import React, { createContext, useContext, useState } from 'react';

const AuthContext = createContext({ user: null, login: () => {}, logout: () => {} });

/**
 * True when the JWT's `exp` claim is in the past (or the token is unreadable).
 * Tokens last an hour, so without this check a stale token left in
 * sessionStorage keeps the UI "logged in" indefinitely while every API call
 * comes back 401 — which looks like a broken app, not an expired session.
 */
function isExpired(token) {
  try {
    const payload = token.split('.')[1];
    const { exp } = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    return !exp || exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

export function AuthProvider({ children }) {
  // DONE: TODO(TICKET-ADV112): lazy-init `user` from sessionStorage so a page
  //                     refresh doesn't blow the JWT away. Look for keys
  //                     'reconx-token' and 'reconx-role'.
  const [user, setUser] = useState(() => {
    const token = sessionStorage.getItem('reconx-token');
    const role = sessionStorage.getItem('reconx-role');
    if (!token || isExpired(token)) {
      sessionStorage.removeItem('reconx-token');
      sessionStorage.removeItem('reconx-role');
      return null;
    }
    return { token, role };
  });

  const login = (token, role) => {
    // DONE: TODO(TICKET-ADV112): persist token+role to sessionStorage and call setUser.
    sessionStorage.setItem('reconx-token', token);
    sessionStorage.setItem('reconx-role', role);
    setUser({ token, role });
  };

  const logout = () => {
    // DONE: TODO(TICKET-ADV112): clear sessionStorage and reset user state to null.
    sessionStorage.removeItem('reconx-token');
    sessionStorage.removeItem('reconx-role');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
