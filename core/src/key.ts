import { base64ToBytes, bytesToBase64, fromUtf8, utf8 } from './base64.ts';

export const ENVELOPE_ALGORITHM = 'AES-256-GCM';

export interface Envelope {
  version: number;
  algorithm: string;
  iv: string;
  ciphertext: string;
}

export class KeyError extends Error {}

export class RelayKey {
  readonly raw: Uint8Array<ArrayBuffer>;
  readonly encoded: string;
  private gcmKey?: Promise<CryptoKey>;
  private ctrKey?: Promise<CryptoKey>;

  private constructor(raw: Uint8Array<ArrayBuffer>, encoded: string) {
    this.raw = raw;
    this.encoded = encoded;
  }

  static fromBase64(value: string): RelayKey {
    const encoded = value.trim();
    let raw: Uint8Array<ArrayBuffer>;
    try {
      raw = base64ToBytes(encoded);
    } catch {
      throw new KeyError('A chave não é um base64 válido.');
    }
    if (raw.length !== 32) {
      throw new KeyError('A chave precisa ter 32 bytes (AES-256) em base64.');
    }
    return new RelayKey(raw, encoded);
  }

  static fromRaw(raw: Uint8Array): RelayKey {
    const copy = new Uint8Array(raw);
    return RelayKey.fromBase64(bytesToBase64(copy));
  }

  static generate(): RelayKey {
    return RelayKey.fromRaw(crypto.getRandomValues(new Uint8Array(32)));
  }

  gcm(): Promise<CryptoKey> {
    this.gcmKey ??= crypto.subtle.importKey('raw', this.raw, 'AES-GCM', false, ['encrypt', 'decrypt']);
    return this.gcmKey;
  }

  ctr(): Promise<CryptoKey> {
    this.ctrKey ??= crypto.subtle.importKey('raw', this.raw, 'AES-CTR', false, ['encrypt', 'decrypt']);
    return this.ctrKey;
  }

  async fingerprint(): Promise<string> {
    const hash = new Uint8Array(await crypto.subtle.digest('SHA-256', this.raw));
    return Array.from(hash.subarray(0, 8), (byte) => byte.toString(16).padStart(2, '0').toUpperCase()).join(':');
  }

  async encryptBytes(plain: Uint8Array<ArrayBuffer>, version = 1): Promise<string> {
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const ciphertext = new Uint8Array(await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, await this.gcm(), plain));
    const envelope: Envelope = {
      version,
      algorithm: ENVELOPE_ALGORITHM,
      iv: bytesToBase64(iv),
      ciphertext: bytesToBase64(ciphertext),
    };
    return JSON.stringify(envelope);
  }

  encryptText(value: string, version = 1): Promise<string> {
    return this.encryptBytes(utf8(value), version);
  }

  async decryptBytes(envelopeJson: string): Promise<Uint8Array<ArrayBuffer>> {
    const envelope = parseEnvelope(envelopeJson);
    if (!envelope) throw new KeyError('Envelope criptografado inválido.');
    try {
      const plain = await crypto.subtle.decrypt(
        { name: 'AES-GCM', iv: base64ToBytes(envelope.iv) },
        await this.gcm(),
        base64ToBytes(envelope.ciphertext),
      );
      return new Uint8Array(plain);
    } catch {
      throw new KeyError('Não foi possível descriptografar: a chave não confere.');
    }
  }

  async decryptText(envelopeJson: string): Promise<string> {
    return fromUtf8(await this.decryptBytes(envelopeJson));
  }
}

export function parseEnvelope(value: string | null | undefined): Envelope | null {
  if (!looksLikeEnvelope(value)) return null;
  try {
    const parsed = JSON.parse(value as string) as Partial<Envelope>;
    if (typeof parsed.iv !== 'string' || typeof parsed.ciphertext !== 'string') return null;
    return {
      version: typeof parsed.version === 'number' ? parsed.version : 1,
      algorithm: parsed.algorithm ?? ENVELOPE_ALGORITHM,
      iv: parsed.iv,
      ciphertext: parsed.ciphertext,
    };
  } catch {
    return null;
  }
}

export function looksLikeEnvelope(value: string | null | undefined): boolean {
  const text = value?.trim();
  if (!text || !text.startsWith('{') || !text.endsWith('}')) return false;
  return text.includes('"algorithm"') && text.includes('"iv"') && text.includes('"ciphertext"');
}

export async function decryptOrNull(key: RelayKey | null, value: string | null | undefined): Promise<string | null> {
  if (value == null || value.trim() === '') return value ?? null;
  if (!looksLikeEnvelope(value)) return value;
  if (!key) return null;
  try {
    return await key.decryptText(value);
  } catch {
    return null;
  }
}
