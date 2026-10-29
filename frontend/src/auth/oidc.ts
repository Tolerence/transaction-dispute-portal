import { oidcConfig } from '../api/config';
import { clearToken, setToken } from './session';

/**
 * Authorization code flow with PKCE (RFC 7636) against the configured OpenID Connect provider.
 * The access token is never placed in the URL, and the state and verifier live in sessionStorage
 * only for the duration of the redirect.
 */

const STATE_KEY = 'dispute-portal.oidc.state';
const VERIFIER_KEY = 'dispute-portal.oidc.verifier';
const SCOPE = 'openid profile';

interface Discovery {
  authorization_endpoint: string;
  token_endpoint: string;
  end_session_endpoint?: string;
}

let discoveryCache: Promise<Discovery> | null = null;

export function isOidcConfigured(): boolean {
  return oidcConfig.issuer !== '';
}

function discover(): Promise<Discovery> {
  if (!discoveryCache) {
    discoveryCache = fetch(`${oidcConfig.issuer}/.well-known/openid-configuration`, {
      headers: { Accept: 'application/json' },
    }).then(async (res) => {
      if (!res.ok) throw new Error(`Identity provider discovery failed (${res.status})`);
      return (await res.json()) as Discovery;
    });
    // Allow a retry after a transient failure.
    discoveryCache.catch(() => {
      discoveryCache = null;
    });
  }
  return discoveryCache;
}

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = '';
  bytes.forEach((b) => {
    binary += String.fromCharCode(b);
  });
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/** A high-entropy code verifier of 64 characters from the unreserved set (RFC 7636 section 4.1). */
export function createCodeVerifier(): string {
  const bytes = new Uint8Array(48);
  crypto.getRandomValues(bytes);
  return base64UrlEncode(bytes);
}

export async function createCodeChallenge(verifier: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return base64UrlEncode(new Uint8Array(digest));
}

function redirectUri(): string {
  return `${window.location.origin}/callback`;
}

/** Starts the sign-in redirect. */
export async function beginLogin(): Promise<void> {
  const { authorization_endpoint: authorizeUrl } = await discover();
  const state = createCodeVerifier();
  const verifier = createCodeVerifier();
  sessionStorage.setItem(STATE_KEY, state);
  sessionStorage.setItem(VERIFIER_KEY, verifier);

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: oidcConfig.clientId,
    redirect_uri: redirectUri(),
    scope: SCOPE,
    state,
    code_challenge: await createCodeChallenge(verifier),
    code_challenge_method: 'S256',
  });
  window.location.assign(`${authorizeUrl}?${params.toString()}`);
}

/**
 * Completes the redirect from the provider: checks state, exchanges the code and stores the access token.
 * Throws if the response is not the one we asked for.
 */
export async function completeLogin(callbackUrl: URL): Promise<void> {
  const error = callbackUrl.searchParams.get('error');
  if (error) throw new Error(`Sign-in was not completed (${error})`);

  const code = callbackUrl.searchParams.get('code');
  const state = callbackUrl.searchParams.get('state');
  const expectedState = sessionStorage.getItem(STATE_KEY);
  const verifier = sessionStorage.getItem(VERIFIER_KEY);
  sessionStorage.removeItem(STATE_KEY);
  sessionStorage.removeItem(VERIFIER_KEY);

  if (!code || !state || !expectedState || !verifier || state !== expectedState) {
    throw new Error('Sign-in response could not be verified. Please try again.');
  }

  const { token_endpoint: tokenUrl } = await discover();
  const response = await fetch(tokenUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', Accept: 'application/json' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: oidcConfig.clientId,
      code,
      redirect_uri: redirectUri(),
      code_verifier: verifier,
    }),
  });
  if (!response.ok) throw new Error('Sign-in failed. Please try again.');

  const tokens = (await response.json()) as { access_token?: string };
  if (!tokens.access_token) throw new Error('Sign-in failed. Please try again.');
  setToken(tokens.access_token);
}

/** Clears the local session and, when the provider supports it, ends the provider session too. */
export async function logout(): Promise<void> {
  clearToken();
  if (!isOidcConfigured()) return;
  try {
    const { end_session_endpoint: endUrl } = await discover();
    if (endUrl) {
      const params = new URLSearchParams({
        client_id: oidcConfig.clientId,
        post_logout_redirect_uri: `${window.location.origin}/login`,
      });
      window.location.assign(`${endUrl}?${params.toString()}`);
    }
  } catch {
    // Local session is already cleared; the user is returned to the login page.
  }
}
