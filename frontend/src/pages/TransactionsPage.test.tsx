import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/client';
import { page, transaction } from '../test/fixtures';
import { TransactionsPage } from './TransactionsPage';

vi.mock('../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/client')>();
  return { ...actual, api: { ...actual.api, listTransactions: vi.fn() } };
});

function renderPage() {
  return render(
    <MemoryRouter>
      <TransactionsPage />
    </MemoryRouter>,
  );
}

describe('TransactionsPage', () => {
  beforeEach(() => {
    vi.mocked(api.listTransactions).mockReset();
  });

  it('lists the transactions returned by the API with links to their detail pages', async () => {
    vi.mocked(api.listTransactions).mockResolvedValue(
      page([
        transaction({ id: 7, merchantName: 'Woolworths Sandton' }),
        transaction({ id: 8, reference: 'TXN-0002', merchantName: 'Netflix', status: 'REVERSED' }),
      ]),
    );

    renderPage();

    expect(await screen.findByRole('link', { name: 'Woolworths Sandton' })).toHaveAttribute(
      'href',
      '/transactions/7',
    );
    const table = screen.getByRole('table');
    expect(within(table).getByRole('link', { name: 'Netflix' })).toBeInTheDocument();
    expect(within(table).getByText('Reversed')).toBeInTheDocument();
  });

  it('explains when no transactions match the filters', async () => {
    vi.mocked(api.listTransactions).mockResolvedValue(page([]));

    renderPage();

    expect(await screen.findByText('No transactions match these filters')).toBeInTheDocument();
  });

  it('shows the error when loading fails', async () => {
    vi.mocked(api.listTransactions).mockRejectedValue(new Error('Service unavailable'));

    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('Service unavailable');
  });

  it('asks for the first page again when the status filter changes', async () => {
    vi.mocked(api.listTransactions).mockResolvedValue(page([transaction()]));

    renderPage();
    await screen.findByRole('link', { name: 'Woolworths Sandton' });

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'REVERSED' } });

    await waitFor(() =>
      expect(api.listTransactions).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'REVERSED', page: 0 }),
      ),
    );
  });
});
