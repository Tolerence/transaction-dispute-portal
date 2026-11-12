import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { clearToken, setToken } from '../auth/session';
import { reportClientError } from './report';

describe('reportClientError', () => {
  beforeEach(() => {
    clearToken();
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(null, { status: 202 }))));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    clearToken();
  });

  it('posts the source, message and route to the API', () => {
    reportClientError('window-error', new Error('boom'));

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toBe('/api/client-errors');
    expect(init?.method).toBe('POST');
    expect(JSON.parse(init?.body as string)).toEqual({
      source: 'window-error',
      message: 'boom',
      route: window.location.pathname,
    });
  });

  it('sends the bearer token when the user is signed in', () => {
    setToken('abc123');
    reportClientError('unhandled-rejection', 'plain string reason');

    const [, init] = vi.mocked(fetch).mock.calls[0];
    const headers = init?.headers as Record<string, string>;
    expect(headers.Authorization).toBe('Bearer abc123');
    expect(JSON.parse(init?.body as string).message).toBe('plain string reason');
  });

  it('truncates long messages', () => {
    reportClientError('window-error', new Error('x'.repeat(5000)));

    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect(JSON.parse(init?.body as string).message).toHaveLength(1000);
  });

  it('never throws when the network is down', () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new TypeError('offline'))));
    expect(() => reportClientError('window-error', new Error('x'))).not.toThrow();
  });
});
