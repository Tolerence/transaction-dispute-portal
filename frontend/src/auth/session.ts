/**
 * Bearer token storage. sessionStorage is used so the token disappears when the tab closes.
 * Storage can throw (private mode, blocked site data), so every access is guarded and the app
 * degrades to "signed out" rather than crashing.
 */
const TOKEN_KEY = 'dispute-portal.access-token';

export function getToken(): string | null {
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string): void {
  try {
    sessionStorage.setItem(TOKEN_KEY, token);
  } catch {
    // Storage unavailable: the user will be asked to sign in again on the next page load.
  }
}

export function clearToken(): void {
  try {
    sessionStorage.removeItem(TOKEN_KEY);
  } catch {
    // Nothing to clear.
  }
}

export function isSignedIn(): boolean {
  return getToken() !== null;
}

/**
 * Reads the roles from the access token so the UI can show or hide reviewer screens.
 * This is for presentation only: the API enforces the role on every request.
 */
export function currentRoles(token: string | null = getToken()): string[] {
  if (!token) return [];
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const claims = JSON.parse(json) as {
      realm_access?: { roles?: unknown };
      roles?: unknown;
    };
    const realm = claims.realm_access?.roles;
    const flat = claims.roles;
    const roles: unknown[] = [
      ...(Array.isArray(realm) ? realm : []),
      ...(Array.isArray(flat) ? flat : []),
    ];
    return roles.filter((r): r is string => typeof r === 'string');
  } catch {
    return [];
  }
}

export function isReviewer(): boolean {
  return currentRoles().includes('DISPUTE_REVIEWER');
}
