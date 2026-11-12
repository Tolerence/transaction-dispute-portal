import { useState } from 'react';
import { ApiError, api } from '../api/client';
import { DEMO_CUSTOMER_ID, DEMO_REVIEWER_ID, devLoginEnabled, ssoLoginHints } from '../api/config';
import { useNavigate } from 'react-router-dom';
import { setToken } from './session';
import { beginLogin, isOidcConfigured } from './oidc';
import './LoginPage.css';

const STEPS = [
  { title: 'Find the transaction', detail: 'Filter your account by status and date.' },
  { title: 'Raise a dispute', detail: 'Choose a reason and describe what happened.' },
  { title: 'Follow the case', detail: 'Every status change is recorded in the timeline.' },
];

export function LoginPage() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function signInWithProvider(loginHint?: string) {
    setBusy(true);
    setError(null);
    try {
      await beginLogin(loginHint || undefined);
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
    <div className="login-page">
      <aside className="login-showcase" aria-labelledby="login-title">
        <div className="login-brand">
          <span className="brand-mark" aria-hidden="true">TD</span>
          <span>Transaction Disputes</span>
        </div>

        <div className="login-showcase-body">
          <p className="eyebrow">Account activity</p>
          <h1 id="login-title">Challenge any transaction you don&rsquo;t recognise.</h1>

          <ol className="steps">
            {STEPS.map((step) => (
              <li key={step.title}>
                <div>
                  <strong>{step.title}</strong>
                  <span>{step.detail}</span>
                </div>
              </li>
            ))}
          </ol>
        </div>

        <p className="login-footnote">© {new Date().getFullYear()} Transaction Disputes</p>
      </aside>

      <main className="login-main">
        <section className="login-card" aria-labelledby="signin-title">
          <div>
            <h2 id="signin-title">Sign in</h2>
            <p className="login-card-lede">Continue to your transactions and disputes.</p>
          </div>

          {isOidcConfigured() && (
            <div className="sso-options">
              <button
                type="button"
                className="login-primary"
                onClick={() => signInWithProvider(ssoLoginHints.customer)}
                disabled={busy}
              >
                {busy ? 'Redirecting…' : 'Sign in as customer'}
              </button>
              <button
                type="button"
                className="secondary"
                onClick={() => signInWithProvider(ssoLoginHints.reviewer)}
                disabled={busy}
              >
                Sign in as reviewer
              </button>
            </div>
          )}

          {devLoginEnabled && (
            <div className="demo">
              {isOidcConfigured() && <div className="auth-divider">or</div>}
              <div className="demo-head">
                <span>Demo accounts</span>
                <span className="tag">Local only</span>
              </div>

              <div className="demo-option">
                <button
                  type="button"
                  className="secondary"
                  onClick={() => signInAsDemoCustomer(DEMO_CUSTOMER_ID)}
                  disabled={busy}
                >
                  Sign in as demo customer (local only)
                </button>
                <p className="demo-hint">Views transactions and raises disputes.</p>
              </div>

              <div className="demo-option">
                <button
                  type="button"
                  className="secondary"
                  onClick={() => signInAsDemoCustomer(DEMO_REVIEWER_ID, ['DISPUTE_REVIEWER'])}
                  disabled={busy}
                >
                  Sign in as demo reviewer (local only)
                </button>
                <p className="demo-hint">Works the review queue and decides cases.</p>
              </div>
            </div>
          )}

          {!hasAnyOption && <p className="muted">Sign-in is not configured for this build.</p>}

          {error && (
            <p role="alert" className="login-error">
              {error}
            </p>
          )}
        </section>
      </main>
    </div>
  );
}
