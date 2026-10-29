import { clearToken, getToken } from '../auth/session';
import type {
  CreateDisputeRequest,
  Dispute,
  DisputeQuery,
  Page,
  ReviewDecisionRequest,
  Transaction,
  TransactionQuery,
} from './types';

/** Base URL is relative: Vite's dev proxy and the nginx container both forward /api to the backend. */
const API_BASE = '/api';

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

interface ProblemDetail {
  detail?: string;
  errors?: Record<string, string>;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  const token = getToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });

  if (response.status === 401) {
    clearToken();
    if (window.location.pathname !== '/login') {
      window.location.assign('/login');
    }
    throw new ApiError(401, 'Your session has expired. Please sign in again.');
  }

  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as ProblemDetail | null;
    throw new ApiError(
      response.status,
      problem?.detail ?? `Request failed with status ${response.status}`,
      problem?.errors ?? {},
    );
  }

  return (await response.json()) as T;
}

function toQueryString(params: Record<string, string | number | undefined | ''>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      search.set(key, String(value));
    }
  }
  const query = search.toString();
  return query ? `?${query}` : '';
}

export const api = {
  issueDevToken(customerId: string, roles: string[] = []) {
    return request<{ accessToken: string; expiresAt: string }>('/dev/token', {
      method: 'POST',
      body: JSON.stringify({ customerId, roles }),
    });
  },

  listTransactions(query: TransactionQuery) {
    return request<Page<Transaction>>(`/transactions${toQueryString({ ...query })}`);
  },

  getTransaction(id: number) {
    return request<Transaction>(`/transactions/${id}`);
  },

  submitDispute(transactionId: number, body: CreateDisputeRequest) {
    return request<Dispute>(`/transactions/${transactionId}/disputes`, {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  listDisputes(query: DisputeQuery) {
    return request<Page<Dispute>>(`/disputes${toQueryString({ ...query })}`);
  },

  getDispute(id: number) {
    return request<Dispute>(`/disputes/${id}`);
  },

  withdrawDispute(id: number) {
    return request<Dispute>(`/disputes/${id}/withdraw`, { method: 'POST' });
  },

  listReviewQueue(query: DisputeQuery) {
    return request<Page<Dispute>>(`/review/disputes${toQueryString({ ...query })}`);
  },

  getReviewDispute(id: number) {
    return request<Dispute>(`/review/disputes/${id}`);
  },

  startReview(id: number) {
    return request<Dispute>(`/review/disputes/${id}/start`, { method: 'POST' });
  },

  decideDispute(id: number, body: ReviewDecisionRequest) {
    return request<Dispute>(`/review/disputes/${id}/decision`, {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },
};
