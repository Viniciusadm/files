import { createCipheriv, createDecipheriv, randomBytes } from 'node:crypto';
import { describe, expect, it } from 'vitest';
import { concatBytes } from './base64.ts';
import { GcmStreamDecryptor, GcmStreamEncryptor, decryptFileBytes, encryptFileBytes } from './gcmStream.ts';
import { FlesKey } from './key.ts';

function nodeEncrypt(raw: Uint8Array, plain: Uint8Array): Uint8Array {
  const iv = randomBytes(12);
  const cipher = createCipheriv('aes-256-gcm', raw, iv);
  return concatBytes([iv, cipher.update(plain), cipher.final(), cipher.getAuthTag()]);
}

function nodeDecrypt(raw: Uint8Array, data: Uint8Array): Uint8Array {
  const decipher = createDecipheriv('aes-256-gcm', raw, data.subarray(0, 12));
  decipher.setAuthTag(data.subarray(data.length - 16));
  return concatBytes([decipher.update(data.subarray(12, data.length - 16)), decipher.final()]);
}

async function streamEncrypt(key: FlesKey, plain: Uint8Array, pieceSize: number): Promise<Uint8Array> {
  const encryptor = await GcmStreamEncryptor.create(key);
  const parts: Uint8Array[] = [];
  for (let offset = 0; offset < plain.length; offset += pieceSize) {
    parts.push(await encryptor.update(plain.subarray(offset, offset + pieceSize)));
  }
  parts.push(await encryptor.final());
  return concatBytes(parts);
}

async function streamDecrypt(key: FlesKey, data: Uint8Array, pieceSize: number): Promise<Uint8Array> {
  const decryptor = new GcmStreamDecryptor(key);
  const parts: Uint8Array[] = [];
  for (let offset = 0; offset < data.length; offset += pieceSize) {
    parts.push(await decryptor.update(data.subarray(offset, offset + pieceSize)));
  }
  parts.push(await decryptor.final());
  return concatBytes(parts);
}

describe('streaming AES-GCM', () => {
  const sizes = [0, 1, 15, 16, 17, 1000, 65536, 100_003];
  const pieces = [1, 7, 16, 4096, 1 << 20];

  for (const size of sizes) {
    it(`matches node for ${size} bytes`, async () => {
      const key = FlesKey.generate();
      const plain = randomBytes(size);
      for (const piece of pieces) {
        const encrypted = await streamEncrypt(key, plain, piece);
        expect(encrypted.length).toBe(size + 28);
        expect(Buffer.from(nodeDecrypt(key.raw, encrypted))).toEqual(plain);
        const fromNode = nodeEncrypt(key.raw, plain);
        expect(Buffer.from(await streamDecrypt(key, fromNode, piece))).toEqual(plain);
      }
    });
  }

  it('round trips with helpers', async () => {
    const key = FlesKey.generate();
    const plain = randomBytes(5000);
    expect(Buffer.from(await decryptFileBytes(key, await encryptFileBytes(key, plain)))).toEqual(plain);
  });

  it('rejects tampered data and wrong keys', async () => {
    const key = FlesKey.generate();
    const encrypted = await encryptFileBytes(key, randomBytes(100));
    const tampered = encrypted.slice();
    tampered[40] = (tampered[40] ?? 0) ^ 1;
    await expect(decryptFileBytes(key, tampered)).rejects.toThrow();
    await expect(decryptFileBytes(FlesKey.generate(), encrypted)).rejects.toThrow();
    await expect(decryptFileBytes(key, encrypted.subarray(0, 20))).rejects.toThrow();
  });
});
