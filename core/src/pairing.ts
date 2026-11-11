import type { ApiClient } from './api.ts';
import { ApiError } from './api.ts';
import { base64ToBytes, bytesToBase64, fromUtf8, utf8 } from './base64.ts';
import { RelayKey, KeyError } from './key.ts';

const CURVE = { name: 'ECDH', namedCurve: 'P-256' } as const;
const CROCKFORD = '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
const POLL_INTERVAL_MS = 2000;

export interface PairingInfo {
  id: string;
  publicKey: string;
  deviceName: string | null;
  status: 'pending' | 'approved';
  createdAt: string;
  expiresAt: string;
  response?: { publicKey: string; iv: string; ciphertext: string } | null;
}

export async function pairingCode(publicKey: Uint8Array<ArrayBuffer>): Promise<string> {
  const hash = new Uint8Array(await crypto.subtle.digest('SHA-256', publicKey));
  let bits = 0;
  let value = 0;
  let code = '';
  for (const byte of hash) {
    value = (value << 8) | byte;
    bits += 8;
    while (bits >= 5 && code.length < 10) {
      code += CROCKFORD[(value >>> (bits - 5)) & 31];
      bits -= 5;
    }
    value &= (1 << bits) - 1;
    if (code.length >= 10) break;
  }
  return `${code.slice(0, 5)}-${code.slice(5)}`;
}

export function normalizePairingCode(value: string): string {
  const clean = value.toUpperCase().replace(/[^0-9A-Z]/g, '').replace(/O/g, '0').replace(/[IL]/g, '1');
  return clean.length === 10 ? `${clean.slice(0, 5)}-${clean.slice(5)}` : clean;
}

async function sharedAesKey(privateKey: CryptoKey, peerPublicKey: Uint8Array<ArrayBuffer>): Promise<RelayKey> {
  const peer = await crypto.subtle.importKey('raw', peerPublicKey, CURVE, false, []);
  const secret = await crypto.subtle.deriveBits({ name: 'ECDH', public: peer }, privateKey, 256);
  return RelayKey.fromRaw(new Uint8Array(await crypto.subtle.digest('SHA-256', secret)));
}

function sleep(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) return reject(signal.reason);
    const timer = setTimeout(resolve, ms);
    signal?.addEventListener('abort', () => {
      clearTimeout(timer);
      reject(signal.reason);
    }, { once: true });
  });
}

export class PairingRequest {
  private constructor(
    private readonly api: ApiClient,
    private readonly privateKey: CryptoKey,
    readonly id: string,
    readonly code: string,
    readonly expiresAt: Date,
  ) {}

  static async start(api: ApiClient, deviceName: string): Promise<PairingRequest> {
    const pair = await crypto.subtle.generateKey(CURVE, false, ['deriveBits']);
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('raw', pair.publicKey));
    const info = await api.post<PairingInfo>('files/pairings', {
      publicKey: bytesToBase64(publicKey),
      deviceName: deviceName.slice(0, 80),
    });
    return new PairingRequest(api, pair.privateKey, info.id, await pairingCode(publicKey), new Date(info.expiresAt));
  }

  async waitForKey(signal?: AbortSignal): Promise<RelayKey> {
    for (;;) {
      if (Date.now() > this.expiresAt.getTime()) throw new KeyError('O código expirou. Gere um novo.');
      let info: PairingInfo;
      try {
        info = await this.api.get<PairingInfo>(`files/pairings/${this.id}`, undefined, signal);
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) throw new KeyError('O código expirou ou foi cancelado. Gere um novo.');
        throw error;
      }
      if (info.status === 'approved' && info.response) {
        const shared = await sharedAesKey(this.privateKey, base64ToBytes(info.response.publicKey));
        const envelope = JSON.stringify({ version: 1, algorithm: 'AES-256-GCM', iv: info.response.iv, ciphertext: info.response.ciphertext });
        return RelayKey.fromBase64(fromUtf8(await shared.decryptBytes(envelope)));
      }
      await sleep(POLL_INTERVAL_MS, signal);
    }
  }

  async cancel(): Promise<void> {
    await this.api.delete(`files/pairings/${this.id}`).catch(() => undefined);
  }
}

export async function approvePairing(api: ApiClient, key: RelayKey, typedCode: string): Promise<void> {
  const pending = await api.get<PairingInfo>('files/pairings/pending');
  const desktopKey = base64ToBytes(pending.publicKey);
  if ((await pairingCode(desktopKey)) !== normalizePairingCode(typedCode)) {
    throw new KeyError('O código não confere com o pedido pendente.');
  }
  const pair = await crypto.subtle.generateKey(CURVE, true, ['deriveBits']);
  const shared = await sharedAesKey(pair.privateKey, desktopKey);
  const envelope = JSON.parse(await shared.encryptBytes(utf8(key.encoded))) as { iv: string; ciphertext: string };
  await api.post(`files/pairings/${pending.id}/approve`, {
    publicKey: bytesToBase64(new Uint8Array(await crypto.subtle.exportKey('raw', pair.publicKey))),
    iv: envelope.iv,
    ciphertext: envelope.ciphertext,
  });
}
