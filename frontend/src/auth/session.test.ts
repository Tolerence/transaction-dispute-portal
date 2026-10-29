import { currentRoles } from './session';

function tokenWith(claims: Record<string, unknown>): string {
  const encode = (value: unknown) =>
    btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${encode({ alg: 'none' })}.${encode(claims)}.signature`;
}

describe('currentRoles', () => {
  it('reads Keycloak realm roles', () => {
    const token = tokenWith({ realm_access: { roles: ['DISPUTE_REVIEWER', 'offline_access'] } });
    expect(currentRoles(token)).toEqual(['DISPUTE_REVIEWER', 'offline_access']);
  });

  it('reads the flat roles claim used by the dev token', () => {
    expect(currentRoles(tokenWith({ roles: ['DISPUTE_REVIEWER'] }))).toEqual(['DISPUTE_REVIEWER']);
  });

  it('returns no roles for a customer token or a malformed token', () => {
    expect(currentRoles(tokenWith({ sub: 'alice' }))).toEqual([]);
    expect(currentRoles('not-a-token')).toEqual([]);
    expect(currentRoles(null)).toEqual([]);
  });
});
