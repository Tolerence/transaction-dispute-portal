/** Placeholder rows shown while a table loads, so the layout does not jump when data arrives. */
export function TableSkeleton({ columns, rows = 6 }: { columns: number; rows?: number }) {
  const widths = [60, 80, 45, 35, 55, 40];
  return (
    <div className="table-skeleton" aria-busy="true" aria-live="polite">
      <span className="sr-only">Loading</span>
      <table aria-hidden="true">
        <tbody>
          {Array.from({ length: rows }, (_, row) => (
            <tr key={row} className="skeleton-row">
              {Array.from({ length: columns }, (_, col) => (
                <td key={col}>
                  <span
                    className="skeleton skeleton-bar"
                    style={{ ['--w' as string]: `${widths[(row + col) % widths.length]}%` }}
                  />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** Placeholder for a details page while its record loads. */
export function DetailSkeleton() {
  return (
    <div aria-busy="true" aria-live="polite">
      <span className="sr-only">Loading</span>
      <span className="skeleton skeleton-bar" style={{ ['--w' as string]: '30%', height: '1.4rem' }} />
      <span className="skeleton skeleton-card" style={{ display: 'block', marginTop: '1.5rem' }} />
      <span className="skeleton skeleton-card" style={{ display: 'block' }} />
    </div>
  );
}
