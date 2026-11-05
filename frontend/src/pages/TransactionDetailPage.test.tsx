import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/client';
import { transaction } from '../test/fixtures';
import { TransactionDetailPage } from './TransactionDetailPage';

vi.mock('../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/client')>();
  return { ...actual, api: { ...actual.api, getTransaction: vi.fn(), submitDispute: vi.fn() } };
});

function renderAt(id: number) {
  return render(
    <MemoryRouter initialEntries={[`/transactions/${id}`]}>
      <Routes>
        <Route path="/transactions/:id" element={<TransactionDetailPage />} />
        <Route path="/disputes/:id" element={<p>Dispute opened</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('TransactionDetailPage', () => {
  beforeEach(() => {
    vi.mocked(api.getTransaction).mockReset();
    vi.mocked(api.submitDispute).mockReset();
  });

  it('blocks submission and explains the problems when the form is incomplete', async () => {
    vi.mocked(api.getTransaction).mockResolvedValue(transaction({ disputable: true }));

    renderAt(1);
    fireEvent.click(await screen.findByRole('button', { name: 'Submit dispute' }));

    expect(await screen.findByText('Choose a reason for the dispute')).toBeInTheDocument();
    expect(screen.getByText('Describe the problem in at least 10 characters')).toBeInTheDocument();
    expect(api.submitDispute).not.toHaveBeenCalled();
  });

  it('sends a valid dispute with the trimmed description', async () => {
    vi.mocked(api.getTransaction).mockResolvedValue(transaction({ id: 5, disputable: true }));
    vi.mocked(api.submitDispute).mockResolvedValue({ id: 99 } as never);

    renderAt(5);
    await screen.findByRole('heading', { name: 'Raise a dispute' });

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'DUPLICATE_CHARGE' } });
    fireEvent.change(screen.getByRole('textbox'), { target: { value: '   I was charged twice.   ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit dispute' }));

    await waitFor(() =>
      expect(api.submitDispute).toHaveBeenCalledWith(5, {
        reason: 'DUPLICATE_CHARGE',
        description: 'I was charged twice.',
      }),
    );
    expect(await screen.findByText('Dispute opened')).toBeInTheDocument();
  });

  it('explains why a transaction cannot be disputed and hides the form', async () => {
    vi.mocked(api.getTransaction).mockResolvedValue(
      transaction({ disputable: false, ineligibilityReason: 'Transaction is still pending' }),
    );

    renderAt(2);

    expect(await screen.findByText(/cannot be disputed/)).toHaveTextContent('transaction is still pending');
    expect(screen.queryByRole('button', { name: 'Submit dispute' })).not.toBeInTheDocument();
  });
});
