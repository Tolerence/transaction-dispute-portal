import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { completeLogin } from './oidc';

/** Receives the provider redirect at /callback, exchanges the code, then opens the app. */
export function CallbackPage() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    completeLogin(new URL(window.location.href))
      .then(() => {
        if (!cancelled) navigate('/transactions', { replace: true });
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Sign-in failed.');
      });
    return () => {
      cancelled = true;
    };
  }, [navigate]);

  if (error) {
    return (
      <main className="login">
        <h1>Sign-in failed</h1>
        <p role="alert" className="error">{error}</p>
        <a href="/login">Back to sign-in</a>
      </main>
    );
  }
  return <main className="login"><p>Signing you in…</p></main>;
}
