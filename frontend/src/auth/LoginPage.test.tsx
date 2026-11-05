import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/client';
import { LoginPage } from './LoginPage';
import { setToken } from './session';

vi.mock('../api/config', () => ({
  DEMO_CUSTOMER_ID: 'customer-1',
  DEMO_REVIEWER_ID: 'reviewer-1',
  devLoginEnabled: true,
}));

vi.mock('./oidc', () => ({
  beginLogin: vi.fn(),
  isOidcConfigured: () => false,
}));

vi.mock('./session', () => ({
  setToken: vi.fn(),
}));

vi.mock('../api/client', () => ({
  ApiError: class ApiError extends Error {},
  api: {
    issueDevToken: vi.fn(),
  },
}));

function renderLogin() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.mocked(api.issueDevToken).mockReset();
    vi.mocked(setToken).mockReset();
  });

  it('offers the local demo sign-ins when dev login is enabled', () => {
    renderLogin();

    expect(screen.getByRole('button', { name: 'Sign in as demo customer (local only)' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Sign in as demo reviewer (local only)' })).toBeEnabled();
    expect(screen.queryByRole('button', { name: 'Sign in' })).not.toBeInTheDocument();
  });

  it('signs in as the demo customer and stores the issued token', async () => {
    vi.mocked(api.issueDevToken).mockResolvedValue({ accessToken: 'token-123', expiresAt: '2030-01-01' });

    renderLogin();
    fireEvent.click(screen.getByRole('button', { name: 'Sign in as demo customer (local only)' }));

    await waitFor(() => expect(setToken).toHaveBeenCalledWith('token-123'));
    expect(api.issueDevToken).toHaveBeenCalledWith('customer-1', []);
  });

  it('shows an error when the demo sign-in fails', async () => {
    vi.mocked(api.issueDevToken).mockRejectedValue(new Error('network down'));

    renderLogin();
    fireEvent.click(screen.getByRole('button', { name: 'Sign in as demo customer (local only)' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Sign-in failed. Is the backend running?');
  });
});
