import { formatMoney, humanise } from './format';

describe('formatMoney', () => {
  it('formats rand amounts with two decimals', () => {
    expect(formatMoney(1299.5, 'ZAR')).toMatch(/299,50/);
  });
});

describe('humanise', () => {
  it('turns enum names into sentence case', () => {
    expect(humanise('UNDER_REVIEW')).toBe('Under review');
    expect(humanise('UNAUTHORISED_TRANSACTION')).toBe('Unauthorised transaction');
  });
});
