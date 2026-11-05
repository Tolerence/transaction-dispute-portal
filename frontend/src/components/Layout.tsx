import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { logout } from '../auth/oidc';
import { isReviewer } from '../auth/session';

export function Layout() {
  const navigate = useNavigate();

  function signOut() {
    void logout().then(() => navigate('/login', { replace: true }));
  }

  return (
    <div className="app">
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <header className="topbar">
        <NavLink to="/transactions" className="brand" aria-label="Transaction Disputes home">
          <span className="brand-mark" aria-hidden="true">TD</span>
          <span>Transaction Disputes</span>
        </NavLink>
        <nav className="topnav" aria-label="Main">
          <NavLink to="/transactions">Transactions</NavLink>
          <NavLink to="/disputes">Dispute history</NavLink>
          {isReviewer() && <NavLink to="/review">Review queue</NavLink>}
        </nav>
        <div className="topbar-actions">
          {isReviewer() && <span className="role-chip">Reviewer</span>}
          <button type="button" className="ghost" onClick={signOut}>
            Sign out
          </button>
        </div>
      </header>
      <main id="main" className="content" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  );
}
