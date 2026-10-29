import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { TransactionStatus } from '../api/types';
import { Pagination } from '../components/Pagination';
import { StatusBadge } from '../components/StatusBadge';
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
      <h1>Transactions</h1>

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

      {error && <p role="alert" className="error">{error}</p>}
      {loading && !data && <p>Loading transactions…</p>}

      {data && data.content.length === 0 && <p className="muted">No transactions match these filters.</p>}

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
