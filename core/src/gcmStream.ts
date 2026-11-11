import { ghash } from '@noble/ciphers/_polyval.js';
import { concatBytes } from './base64.ts';
import { KeyError, type FlesKey } from './key.ts';

export const IV_LENGTH = 12;
export const TAG_LENGTH = 16;
export const GCM_OVERHEAD = IV_LENGTH + TAG_LENGTH;
const BLOCK = 16;
const EMPTY = new Uint8Array(0);

type GHashState = ReturnType<typeof ghash.create>;

function counterBlock(iv: Uint8Array, counter: number): Uint8Array<ArrayBuffer> {
  const block = new Uint8Array(BLOCK);
  block.set(iv, 0);
  new DataView(block.buffer).setUint32(IV_LENGTH, counter >>> 0, false);
  return block;
}

async function ctrTransform(key: CryptoKey, counter: Uint8Array<ArrayBuffer>, data: Uint8Array<ArrayBuffer>): Promise<Uint8Array<ArrayBuffer>> {
  const output = await crypto.subtle.encrypt({ name: 'AES-CTR', counter, length: 32 }, key, data);
  return new Uint8Array(output);
}

class GcmCore {
  private counter = 2;
  private processed = 0;

  private constructor(
    private readonly key: CryptoKey,
    private readonly iv: Uint8Array,
    private readonly hash: GHashState,
    private readonly tagMask: Uint8Array,
  ) {}

  static async create(key: FlesKey, iv: Uint8Array): Promise<GcmCore> {
    const ctrKey = await key.ctr();
    const zero = new Uint8Array(BLOCK);
    const hashKey = await ctrTransform(ctrKey, new Uint8Array(BLOCK), zero);
    const tagMask = await ctrTransform(ctrKey, counterBlock(iv, 1), zero);
    return new GcmCore(ctrKey, iv, ghash.create(hashKey), tagMask);
  }

  async encrypt(plain: Uint8Array<ArrayBuffer>): Promise<Uint8Array<ArrayBuffer>> {
    const cipher = await this.transform(plain);
    this.hash.update(cipher);
    return cipher;
  }

  async decrypt(cipher: Uint8Array<ArrayBuffer>): Promise<Uint8Array<ArrayBuffer>> {
    this.hash.update(cipher);
    return this.transform(cipher);
  }

  tag(): Uint8Array {
    const lengths = new Uint8Array(BLOCK);
    const view = new DataView(lengths.buffer);
    const bits = this.processed * 8;
    view.setUint32(8, Math.floor(bits / 0x100000000), false);
    view.setUint32(12, bits >>> 0, false);
    this.hash.update(lengths);
    const digest = this.hash.digest();
    return digest.map((byte, index) => byte ^ (this.tagMask[index] ?? 0));
  }

  private async transform(data: Uint8Array<ArrayBuffer>): Promise<Uint8Array<ArrayBuffer>> {
    if (data.length === 0) return new Uint8Array(0);
    const output = await ctrTransform(this.key, counterBlock(this.iv, this.counter), data);
    this.counter += Math.ceil(data.length / BLOCK);
    this.processed += data.length;
    return output;
  }
}

function splitAligned(data: Uint8Array<ArrayBuffer>, keepTail = 0): [Uint8Array<ArrayBuffer>, Uint8Array<ArrayBuffer>] {
  const available = Math.max(data.length - keepTail, 0);
  const aligned = available - (available % BLOCK);
  return [data.slice(0, aligned), data.slice(aligned)];
}

export class GcmStreamEncryptor {
  private pending: Uint8Array<ArrayBuffer> = new Uint8Array(0);
  private headerSent = false;

  private constructor(private readonly core: GcmCore, private readonly iv: Uint8Array<ArrayBuffer>) {}

  static async create(key: FlesKey, iv: Uint8Array<ArrayBuffer> = crypto.getRandomValues(new Uint8Array(IV_LENGTH))): Promise<GcmStreamEncryptor> {
    return new GcmStreamEncryptor(await GcmCore.create(key, iv), iv);
  }

  async update(plain: Uint8Array): Promise<Uint8Array<ArrayBuffer>> {
    const [aligned, rest] = splitAligned(concatBytes([this.pending, plain]));
    this.pending = rest;
    const cipher = aligned.length > 0 ? await this.core.encrypt(aligned) : EMPTY;
    return this.withHeader(cipher);
  }

  async final(): Promise<Uint8Array<ArrayBuffer>> {
    const cipher = this.pending.length > 0 ? await this.core.encrypt(this.pending) : EMPTY;
    this.pending = new Uint8Array(0);
    return this.withHeader(concatBytes([cipher, this.core.tag()]));
  }

  private withHeader(data: Uint8Array): Uint8Array<ArrayBuffer> {
    if (this.headerSent) return concatBytes([data]);
    this.headerSent = true;
    return concatBytes([this.iv, data]);
  }
}

export class GcmStreamDecryptor {
  private header: Uint8Array<ArrayBuffer> = new Uint8Array(0);
  private buffer: Uint8Array<ArrayBuffer> = new Uint8Array(0);
  private core: GcmCore | null = null;

  constructor(private readonly key: FlesKey) {}

  async update(chunk: Uint8Array): Promise<Uint8Array<ArrayBuffer>> {
    let data: Uint8Array = chunk;
    if (!this.core) {
      this.header = concatBytes([this.header, chunk]);
      if (this.header.length < IV_LENGTH) return new Uint8Array(0);
      this.core = await GcmCore.create(this.key, this.header.slice(0, IV_LENGTH));
      data = this.header.subarray(IV_LENGTH);
    }
    const [aligned, rest] = splitAligned(concatBytes([this.buffer, data]), TAG_LENGTH);
    this.buffer = rest;
    return aligned.length > 0 ? this.core.decrypt(aligned) : new Uint8Array(0);
  }

  async final(): Promise<Uint8Array<ArrayBuffer>> {
    if (!this.core || this.buffer.length < TAG_LENGTH) {
      throw new KeyError('Arquivo criptografado inválido ou incompleto.');
    }
    const tag = this.buffer.slice(this.buffer.length - TAG_LENGTH);
    const rest = this.buffer.slice(0, this.buffer.length - TAG_LENGTH);
    const plain = rest.length > 0 ? await this.core.decrypt(rest) : new Uint8Array(0);
    const expected = this.core.tag();
    let difference = 0;
    for (let index = 0; index < TAG_LENGTH; index++) {
      difference |= (expected[index] ?? 0) ^ (tag[index] ?? 0);
    }
    if (difference !== 0) {
      throw new KeyError('Falha na verificação do arquivo: chave errada ou arquivo corrompido.');
    }
    return plain;
  }
}

export async function encryptFileBytes(key: FlesKey, plain: Uint8Array): Promise<Uint8Array<ArrayBuffer>> {
  const encryptor = await GcmStreamEncryptor.create(key);
  return concatBytes([await encryptor.update(plain), await encryptor.final()]);
}

export async function decryptFileBytes(key: FlesKey, cipher: Uint8Array): Promise<Uint8Array<ArrayBuffer>> {
  const decryptor = new GcmStreamDecryptor(key);
  return concatBytes([await decryptor.update(cipher), await decryptor.final()]);
}
