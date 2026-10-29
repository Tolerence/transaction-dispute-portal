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

  return (
    <main className="login">
      <h1>Transaction Disputes</h1>
      <p>View your transactions and raise a dispute if something looks wrong.</p>

      {isOidcConfigured() && (
        <button type="button" onClick={signInWithProvider} disabled={busy}>
          {busy ? 'Redirecting…' : 'Sign in'}
        </button>
      )}

      {devLoginEnabled && (
        <button type="button" className="secondary" onClick={() => signInAsDemoCustomer(DEMO_CUSTOMER_ID)} disabled={busy}>
          Sign in as demo customer (local only)
        </button>
      )}

      {devLoginEnabled && (
        <button
          type="button"
          className="secondary"
          onClick={() => signInAsDemoCustomer(DEMO_REVIEWER_ID, ['DISPUTE_REVIEWER'])}
          disabled={busy}
        >
          Sign in as demo reviewer (local only)
        </button>
      )}

      {!isOidcConfigured() && !devLoginEnabled && (
        <p className="muted">Sign-in is not configured for this build.</p>
      )}

      {error && <p role="alert" className="error">{error}</p>}
    </main>
  );
}
