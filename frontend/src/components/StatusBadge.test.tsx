import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { StatusBadge } from './StatusBadge';

describe('StatusBadge', () => {
  it('shows a readable label and a status-specific class', () => {
    render(<StatusBadge status="UNDER_REVIEW" />);
    const badge = screen.getByText('Under review');
    expect(badge).toHaveClass('badge', 'badge-under_review');
  });
});
