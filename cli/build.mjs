import { build } from 'esbuild';
import { chmod } from 'node:fs/promises';

await build({
  entryPoints: ['src/main.ts'],
  bundle: true,
  platform: 'node',
  target: 'node20',
  format: 'esm',
  outfile: 'dist/relay.mjs',
  banner: { js: '#!/usr/bin/env node' },
  minify: true,
  legalComments: 'none',
});
await chmod('dist/relay.mjs', 0o755);
