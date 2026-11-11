import { createCipheriv, createHash, randomBytes } from 'node:crypto';
import { describe, expect, it } from 'vitest';
import { FlesKey, decryptOrNull, looksLikeEnvelope } from './key.ts';

describe('FlesKey', () => {
  it('validates key length', () => {
    expect(() => FlesKey.fromBase64('abc')).toThrow();
    expect(() => FlesKey.fromBase64(Buffer.alloc(16).toString('base64'))).toThrow();
    expect(FlesKey.fromBase64(` ${Buffer.alloc(32, 1).toString('base64')}\n`).raw.length).toBe(32);
  });

  it('decrypts envelopes produced like the Android app', async () => {
    const raw = randomBytes(32);
    const iv = randomBytes(12);
    const cipher = createCipheriv('aes-256-gcm', raw, iv);
    const ciphertext = Buffer.concat([cipher.update('Relatório.pdf', 'utf8'), cipher.final(), cipher.getAuthTag()]);
    const envelope = `{"version":1,"algorithm":"AES-256-GCM","iv":"${iv.toString('base64')}","ciphertext":"${ciphertext.toString('base64')}"}`;
    const key = FlesKey.fromBase64(raw.toString('base64'));
    expect(await key.decryptText(envelope)).toBe('Relatório.pdf');
  });

  it('round trips text and reports fingerprint like the app', async () => {
    const key = FlesKey.generate();
    const envelope = await key.encryptText('olá', 2);
    expect(looksLikeEnvelope(envelope)).toBe(true);
    expect(JSON.parse(envelope).version).toBe(2);
    expect(await key.decryptText(envelope)).toBe('olá');
    const expected = createHash('sha256').update(key.raw).digest().subarray(0, 8).toString('hex').toUpperCase().match(/../g)!.join(':');
    expect(await key.fingerprint()).toBe(expected);
  });

  it('decryptOrNull handles plain text, missing key and wrong key', async () => {
    const key = FlesKey.generate();
    const envelope = await key.encryptText('x');
    expect(await decryptOrNull(key, 'plain')).toBe('plain');
    expect(await decryptOrNull(null, envelope)).toBeNull();
    expect(await decryptOrNull(FlesKey.generate(), envelope)).toBeNull();
    expect(await decryptOrNull(key, envelope)).toBe('x');
  });
});
