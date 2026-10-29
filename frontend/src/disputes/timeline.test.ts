import { describeTransition } from './timeline';

describe('describeTransition', () => {
  it('labels the opening event when fromStatus is null or missing', () => {
    expect(describeTransition(null, 'SUBMITTED')).toBe('Dispute raised');
    expect(describeTransition(undefined, 'SUBMITTED')).toBe('Dispute raised');
  });

  it('labels a status change', () => {
    expect(describeTransition('UNDER_REVIEW', 'ACCEPTED')).toBe('Under review → Accepted');
  });
});
