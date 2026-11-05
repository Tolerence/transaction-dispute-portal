import type { Page, Transaction } from '../api/types';

export function transaction(overrides: Partial<Transaction> = {}): Transaction {
  return {
    id: 1,
    reference: 'TXN-0001',
    merchantName: 'Woolworths Sandton',
    amount: 842.5,
    currency: 'ZAR',
    status: 'POSTED',
    bookedAt: '2026-10-20T10:00:00Z',
    disputable: true,
    ineligibilityReason: null,
    ...overrides,
  };
}

export function page<T>(content: T[], number = 0, totalPages = 1): Page<T> {
  return {
    content,
    page: { size: 20, number, totalElements: content.length, totalPages },
  };
}
