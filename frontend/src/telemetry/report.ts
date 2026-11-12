import { getToken } from '../auth/session';

export type ClientErrorSource = 'error-boundary' | 'window-error' | 'unhandled-rejection';

const MAX_MESSAGE = 1000;
const MAX_ROUTE = 300;

/**
 * Sends a caught error to the API so it appears in the logs and in the frontend.errors metric.
 * Reporting must never break the app, so every failure is swallowed. Anonymous reports are refused by the API
 * and are simply dropped.
 */
export function reportClientError(source: ClientErrorSource, error: unknown): void {
  try {
    const message = error instanceof Error ? error.message : String(error);
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    const body = JSON.stringify({
      source,
      message: message.slice(0, MAX_MESSAGE),
      route: window.location.pathname.slice(0, MAX_ROUTE),
    });
    void fetch('/api/client-errors', { method: 'POST', headers, body, keepalive: true }).catch(() => undefined);
  } catch {
    // Reporting is best effort.
  }
}

/** Reports uncaught errors and unhandled promise rejections anywhere in the app. */
export function installGlobalErrorReporting(): void {
  window.addEventListener('error', (event) => {
    reportClientError('window-error', event.error ?? event.message);
  });
  window.addEventListener('unhandledrejection', (event) => {
    reportClientError('unhandled-rejection', event.reason);
  });
}
