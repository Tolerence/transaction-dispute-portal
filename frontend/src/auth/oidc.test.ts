import { createCodeChallenge, createCodeVerifier } from './oidc';

describe('PKCE helpers', () => {
  it('derives the S256 challenge from the RFC 7636 appendix B example', async () => {
    const verifier = 'dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk';
    expect(await createCodeChallenge(verifier)).toBe('E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM');
  });

  it('creates verifiers of valid length from the unreserved character set', () => {
    const verifier = createCodeVerifier();
    expect(verifier.length).toBeGreaterThanOrEqual(43);
    expect(verifier.length).toBeLessThanOrEqual(128);
    expect(verifier).toMatch(/^[A-Za-z0-9\-_]+$/);
  });

  it('creates a different verifier each time', () => {
    expect(createCodeVerifier()).not.toBe(createCodeVerifier());
  });
});
