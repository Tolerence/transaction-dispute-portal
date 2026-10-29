import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { DisputeStatus } from '../api/types';
import { Pagination } from '../components/Pagination';
import { StatusBadge } from '../components/StatusBadge';
import { formatDate, formatMoney, humanise } from '../format';
import { useAsync } from '../hooks/useAsync';

const PAGE_SIZE = 20;
const QUEUE_STATUSES: DisputeStatus[] = ['SUBMITTED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'WITHDRAWN'];

/** Oldest first by default, so the longest-waiting disputes are handled first. */
export function ReviewQueuePage() {
  const [pageNumber, setPageNumber] = useState(0);
  const [status, setStatus] = useState<DisputeStatus | ''>('SUBMITTED');

  const { data, error, loading } = useAsync(
    () => api.listReviewQueue({ page: pageNumber, size: PAGE_SIZE, status }),
    [pageNumber, status],
  );

  return (
    <section>
      <h1>Review queue</h1>
      <p className="muted">Disputes raised by customers. Oldest first by default.</p>

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
            {QUEUE_STATUSES.map((s) => (
              <option key={s} value={s}>
                {humanise(s)}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error && <p role="alert" className="error">{error}</p>}
      {loading && !data && <p>Loading queue…</p>}

      {data && data.content.length === 0 && <p className="muted">Nothing in the queue for this status.</p>}

      {data && data.content.length > 0 && (
        <>
          <table>
            <thead>
              <tr>
                <th scope="col">Raised</th>
                <th scope="col">Customer</th>
                <th scope="col">Merchant</th>
                <th scope="col">Reason</th>
                <th scope="col" className="numeric">Amount</th>
                <th scope="col">Status</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((d) => (
                <tr key={d.id}>
                  <td>{formatDate(d.createdAt)}</td>
                  <td className="mono">
                    <Link to={`/review/${d.id}`}>{d.customerId}</Link>
                  </td>
                  <td>{d.transaction.merchantName}</td>
                  <td>{humanise(d.reason)}</td>
                  <td className="numeric">{formatMoney(d.transaction.amount, d.transaction.currency)}</td>
                  <td>
                    <StatusBadge status={d.status} />
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
