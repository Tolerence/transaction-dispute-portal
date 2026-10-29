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
      <header className="topbar">
        <span className="brand">Transaction Disputes</span>
        <nav>
          <NavLink to="/transactions">Transactions</NavLink>
          <NavLink to="/disputes">Dispute history</NavLink>
          {isReviewer() && <NavLink to="/review">Review queue</NavLink>}
        </nav>
        <button type="button" className="link" onClick={signOut}>
          Sign out
        </button>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
