import { logger } from './logger';

export interface IntegrityVerificationResult {
  isValid: boolean;
  verdict?: string;
  deviceRecognitionVerdict?: string[];
  appLicensingVerdict?: string;
}

export interface IntegrityVerifier {
  verify(token: string | undefined, expectedNonce: string): Promise<IntegrityVerificationResult>;
}

/**
 * No-op implementation for Phase F3.
 * TODO (Phase F4): Integrate with Google Play Integrity API via googleapis client library,
 * decode JWT response, verify package name, certificate digest, nonce match,
 * and check MEETS_DEVICE_INTEGRITY / MEETS_STRONG_INTEGRITY verdicts.
 */
export class NoopIntegrityVerifier implements IntegrityVerifier {
  async verify(token: string | undefined, expectedNonce: string): Promise<IntegrityVerificationResult> {
    logger.debug({ hasToken: !!token, expectedNonce }, 'Integrity verification bypassed (No-op in Phase F3)');
    return {
      isValid: true,
      verdict: 'NOOP_BYPASS_PHASE_F3',
    };
  }
}

let integrityVerifierInstance: IntegrityVerifier | null = null;

export function getIntegrityVerifier(): IntegrityVerifier {
  if (!integrityVerifierInstance) {
    integrityVerifierInstance = new NoopIntegrityVerifier();
  }
  return integrityVerifierInstance;
}
