import { useState } from 'react';
import { ApiError, api } from '../api/client';
import { DEMO_CUSTOMER_ID, DEMO_REVIEWER_ID, devLoginEnabled } from '../api/config';
import { useNavigate } from 'react-router-dom';
import { setToken } from './session';
import { beginLogin, isOidcConfigured } from './oidc';

export function LoginPage() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function signInWithProvider() {
    setBusy(true);
    setError(null);
    try {
      await beginLogin();
    } catch (err) {
      setBusy(false);
      setError(err instanceof Error ? err.message : 'Sign-in is unavailable. Please try again later.');
    }
  }

  async function signInAsDemoCustomer(customerId: string, roles: string[] = []) {
    setBusy(true);
    setError(null);
    try {
      const { accessToken } = await api.issueDevToken(customerId, roles);
      setToken(accessToken);
      navigate('/transactions', { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Sign-in failed. Is the backend running?');
    } finally {
      setBusy(false);
    }
  }

  const hasAnyOption = isOidcConfigured() || devLoginEnabled;

  return (
    <main className="auth">
      <section className="auth-hero" aria-labelledby="auth-title">
        <span className="brand-mark" aria-hidden="true">TD</span>
        <h2 id="auth-title">Your money, with a clear way to challenge it.</h2>
        <p>Review every transaction on your account and raise a dispute when something looks wrong.</p>
        <ul className="feature-list">
          <li>See every transaction, filtered by status and date</li>
          <li>Raise a dispute in a few guided steps</li>
          <li>Follow each case in a transparent audit timeline</li>
        </ul>
      </section>

      <section className="auth-card" aria-label="Sign in">
        <h1>Welcome back</h1>
        <p className="muted">Sign in to continue to your transactions.</p>

        {isOidcConfigured() && (
          <button type="button" onClick={signInWithProvider} disabled={busy}>
            {busy ? 'Redirecting…' : 'Sign in'}
          </button>
        )}

        {devLoginEnabled && (
          <>
            {isOidcConfigured() && (
              <div className="auth-divider">or</div>
            )}
            <button
              type="button"
              className="secondary"
              onClick={() => signInAsDemoCustomer(DEMO_CUSTOMER_ID)}
              disabled={busy}
            >
              Sign in as demo customer (local only)
            </button>
            <button
              type="button"
              className="secondary"
              onClick={() => signInAsDemoCustomer(DEMO_REVIEWER_ID, ['DISPUTE_REVIEWER'])}
              disabled={busy}
            >
              Sign in as demo reviewer (local only)
            </button>
          </>
        )}

        {!hasAnyOption && <p className="muted">Sign-in is not configured for this build.</p>}

        {error && <p role="alert">{error}</p>}
      </section>
    </main>
  );
}
