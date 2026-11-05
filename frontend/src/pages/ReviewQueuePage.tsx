import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { DisputeStatus } from '../api/types';
import { PageHeader } from '../components/PageHeader';
import { Pagination } from '../components/Pagination';
import { StatusBadge } from '../components/StatusBadge';
import { TableSkeleton } from '../components/Skeleton';
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
      <PageHeader
        eyebrow="Reviewer workspace"
        title="Review queue"
        description="Disputes raised by customers. Oldest first by default, so the longest-waiting cases come first."
      />

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

      {error && <p role="alert">{error}</p>}
      {loading && !data && <TableSkeleton columns={6} />}

      {data && data.content.length === 0 && (
        <div className="empty-state">
          <strong>Nothing in the queue</strong>
          <span>There are no disputes with this status right now.</span>
        </div>
      )}

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
