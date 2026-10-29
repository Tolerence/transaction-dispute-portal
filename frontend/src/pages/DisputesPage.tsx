import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { DisputeStatus } from '../api/types';
import { Pagination } from '../components/Pagination';
import { StatusBadge } from '../components/StatusBadge';
import { formatDate, formatMoney, humanise } from '../format';
import { useAsync } from '../hooks/useAsync';

const PAGE_SIZE = 20;
const STATUSES: DisputeStatus[] = ['SUBMITTED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'WITHDRAWN'];

export function DisputesPage() {
  const [pageNumber, setPageNumber] = useState(0);
  const [status, setStatus] = useState<DisputeStatus | ''>('');

  const { data, error, loading } = useAsync(
    () => api.listDisputes({ page: pageNumber, size: PAGE_SIZE, status }),
    [pageNumber, status],
  );

  return (
    <section>
      <h1>Dispute history</h1>
      <p className="muted">Every dispute you have raised, including closed and withdrawn cases.</p>

      <div className="filters">
        <label>
          Status
          <select
            value={status}
            onChange={(e) => {
              setPageNumber(0);
              setStatus(e.target.value as DisputeStatus | '');
            }}
          >
            <option value="">All</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {humanise(s)}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error && <p role="alert" className="error">{error}</p>}
      {loading && !data && <p>Loading disputes…</p>}

      {data && data.content.length === 0 && (
        <p className="muted">
          No disputes yet. You can raise one from any <Link to="/transactions">transaction</Link>.
        </p>
      )}

      {data && data.content.length > 0 && (
        <>
          <table>
            <thead>
              <tr>
                <th scope="col">Raised</th>
                <th scope="col">Merchant</th>
                <th scope="col">Reason</th>
                <th scope="col" className="numeric">Amount</th>
                <th scope="col">Status</th>
                <th scope="col">Closed</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((d) => (
                <tr key={d.id}>
                  <td>{formatDate(d.createdAt)}</td>
                  <td>
                    <Link to={`/disputes/${d.id}`}>{d.transaction.merchantName}</Link>
                  </td>
                  <td>{humanise(d.reason)}</td>
                  <td className="numeric">{formatMoney(d.transaction.amount, d.transaction.currency)}</td>
                  <td>
                    <StatusBadge status={d.status} />
                  </td>
                  <td>{d.closedAt ? formatDate(d.closedAt) : '—'}</td>
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
