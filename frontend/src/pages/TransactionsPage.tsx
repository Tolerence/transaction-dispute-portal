import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { TransactionStatus } from '../api/types';
import { PageHeader } from '../components/PageHeader';
import { Pagination } from '../components/Pagination';
import { StatusBadge } from '../components/StatusBadge';
import { TableSkeleton } from '../components/Skeleton';
import { formatDate, formatMoney } from '../format';
import { useAsync } from '../hooks/useAsync';

const PAGE_SIZE = 20;

export function TransactionsPage() {
  const [pageNumber, setPageNumber] = useState(0);
  const [status, setStatus] = useState<TransactionStatus | ''>('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const { data, error, loading } = useAsync(
    () => api.listTransactions({ page: pageNumber, size: PAGE_SIZE, status, from, to }),
    [pageNumber, status, from, to],
  );

  function resetPaging<T>(setter: (value: T) => void) {
    return (value: T) => {
      setPageNumber(0);
      setter(value);
    };
  }

  return (
    <section>
      <PageHeader
        eyebrow="Account activity"
        title="Transactions"
        description="Everything posted to your account. Open a transaction to raise a dispute."
      />

      <div className="filters">
        <label>
          Status
          <select value={status} onChange={(e) => resetPaging(setStatus)(e.target.value as TransactionStatus | '')}>
            <option value="">All</option>
            <option value="POSTED">Posted</option>
            <option value="PENDING">Pending</option>
            <option value="REVERSED">Reversed</option>
          </select>
        </label>
        <label>
          From
          <input type="date" value={from} onChange={(e) => resetPaging(setFrom)(e.target.value)} />
        </label>
        <label>
          To
          <input type="date" value={to} onChange={(e) => resetPaging(setTo)(e.target.value)} />
        </label>
      </div>

      {error && <p role="alert">{error}</p>}
      {loading && !data && <TableSkeleton columns={5} />}

      {data && data.content.length === 0 && (
        <div className="empty-state">
          <strong>No transactions match these filters</strong>
          <span>Try widening the date range or clearing the status.</span>
        </div>
      )}

      {data && data.content.length > 0 && (
        <>
          <table>
            <thead>
              <tr>
                <th scope="col">Date</th>
                <th scope="col">Merchant</th>
                <th scope="col">Reference</th>
                <th scope="col" className="numeric">Amount</th>
                <th scope="col">Status</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((tx) => (
                <tr key={tx.id}>
                  <td>{formatDate(tx.bookedAt)}</td>
                  <td>
                    <Link to={`/transactions/${tx.id}`}>{tx.merchantName}</Link>
                  </td>
                  <td className="mono">{tx.reference}</td>
                  <td className="numeric">{formatMoney(tx.amount, tx.currency)}</td>
                  <td>
                    <StatusBadge status={tx.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination page={data.page} onPageChange={setPageNumber} />
        </>
      )}
    </section>
  );
}
