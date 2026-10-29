import type { Page } from '../api/types';

interface PaginationProps {
  page: Page<unknown>['page'];
  onPageChange: (pageNumber: number) => void;
}

export function Pagination({ page, onPageChange }: PaginationProps) {
  if (page.totalPages <= 1) {
    return null;
  }
  return (
    <nav className="pagination" aria-label="Pagination">
      <button type="button" onClick={() => onPageChange(page.number - 1)} disabled={page.number === 0}>
        Previous
      </button>
      <span>
        Page {page.number + 1} of {page.totalPages}
      </span>
      <button
        type="button"
        onClick={() => onPageChange(page.number + 1)}
        disabled={page.number + 1 >= page.totalPages}
      >
        Next
      </button>
    </nav>
  );
}
