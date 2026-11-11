import { describe, expect, it } from 'vitest';
import { ApiClient } from './api.ts';
import { FlesKey } from './key.ts';
import { PairingRequest, approvePairing, normalizePairingCode, pairingCode, type PairingInfo } from './pairing.ts';

function fakeServer() {
  let pairing: PairingInfo | null = null;
  const respond = (status: number, body: unknown) => new Response(JSON.stringify({ success: status < 400, body, message: 'x' }), { status });
  const fetchImpl: typeof fetch = async (input, init) => {
    const url = new URL(String(input));
    const path = url.pathname.replace(/^\/api\//, '');
    const method = init?.method ?? 'GET';
    const json = init?.body ? JSON.parse(String(init.body)) : {};
    if (method === 'POST' && path === 'files/pairings') {
      pairing = { id: 'p1', publicKey: json.publicKey, deviceName: json.deviceName, status: 'pending', createdAt: '', expiresAt: new Date(Date.now() + 60_000).toISOString(), response: null };
      return respond(201, pairing);
    }
    if (method === 'GET' && path === 'files/pairings/pending') {
      return pairing && pairing.status === 'pending' ? respond(200, pairing) : respond(404, null);
    }
    if (method === 'POST' && path === 'files/pairings/p1/approve' && pairing) {
      pairing = { ...pairing, status: 'approved', response: json };
      return respond(200, pairing);
    }
    if (method === 'GET' && path === 'files/pairings/p1' && pairing) {
      return respond(200, pairing);
    }
    return respond(404, null);
  };
  return new ApiClient({ baseUrl: 'http://fake.test', fetch: fetchImpl });
}

describe('pairing', () => {
  it('transfers the exact key string through ECDH', async () => {
    const api = fakeServer();
    const phoneKey = FlesKey.generate();
    const request = await PairingRequest.start(api, 'notebook');
    expect(request.code).toMatch(/^[0-9A-Z]{5}-[0-9A-Z]{5}$/);
    await expect(approvePairing(api, phoneKey, 'AAAAA-AAAAA')).rejects.toThrow();
    await approvePairing(api, phoneKey, request.code.toLowerCase().replace('-', ' '));
    const received = await request.waitForKey();
    expect(received.encoded).toBe(phoneKey.encoded);
  });

  it('produces stable codes and normalizes typing mistakes', async () => {
    const bytes = new Uint8Array(65).fill(7);
    expect(await pairingCode(bytes)).toBe(await pairingCode(bytes.slice()));
    expect(normalizePairingCode('ab1o2 il3x9')).toBe('AB102-113X9');
  });
});
