# Relay

Relay keeps files, notes and checklists in folders, end-to-end encrypted, and moves them between your phone and your computer. This repository holds three clients of Manage API, a private Laravel backend that is not published:

| Client | Folder | What it is for |
| --- | --- | --- |
| Android app | [`app/`](app) | The main app: folders, secure folders behind a PIN, chunked uploads up to 1 GB, gallery, video player, share target, offline sync |
| Web app | [`web/`](web) | Use your files from any computer's browser: drag and drop, paste with Ctrl+V, previews, notes, transfers panel |
| CLI | [`cli/`](cli) | `relay send`, `relay pull --watch` and friends, for scripts and automation |

The web app and the CLI share the encryption and API code in [`core/`](core).

Other clients of the same backend: [`calendar`](https://github.com/Viniciusadm/calendar), [`exercises`](https://github.com/Viniciusadm/exercises), [`oklo`](https://github.com/Viniciusadm/oklo) and [`finances`](https://github.com/Viniciusadm/finances).

## How the encryption works

Folder names, notes, checklist items, file names and file contents are encrypted on the device with AES-256-GCM before they are sent. The server only stores ciphertext.

- The 256-bit key is created on the phone and never goes to the server in the clear.
- Files are stored as `IV (12 bytes) + ciphertext + tag (16 bytes)`. The web app encrypts and decrypts this format in streaming, with WebCrypto AES-CTR plus an incremental GHASH, so large files never sit whole in memory before being sent.
- Secure folders use a second key, wrapped with PBKDF2-SHA256 (200,000 iterations) over `globalKey:PIN`. The web app unlocks them with the same PIN; the CLI reads it from `RELAY_PIN`.

### Bringing the key to a computer

The computer shows a code like `D4JAA-TWZHC`. On the phone, open **Configurações → Conectar computador** and type it.

Under the hood, the computer creates an ephemeral P-256 key pair and posts the public key. The code is the first 50 bits of its SHA-256, so the phone can check it is talking to the computer you are looking at and not a key swapped in by the server. The phone derives a shared secret with ECDH, encrypts the key with it and posts it back. The pairing lives for 10 minutes in the server cache and is read once.

Pasting the key by hand still works: **Configurações → Chave de criptografia → Copiar chave** on the phone.

## Android app

Requirements: JDK 11+, Android SDK with `compileSdk` 36, Android 7.1+ (`minSdk` 25). AGP 9.2.1 and Kotlin 2.2.10, from [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

The backend address comes from `API_BASE_URL` in `local.properties`, which is not versioned. Without it the app points to `https://example.invalid/`:

```properties
API_BASE_URL=http://10.0.2.2:8000/api/
```

```bash
./gradlew installDebug
./gradlew testDebugUnitTest
```

The release build is signed only when `local.properties` has `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`.

The app syncs when it opens, every 30 seconds while it is on screen and every 15 minutes in the background, so whatever you send from the computer shows up when you open it.

## Web app and CLI

Requires Node.js 20+. From the repository root:

```bash
npm install
npm test          # core (crypto interop with Node's AES-GCM) and the rest
npm run build     # web/dist and cli/dist
```

### Web app

```bash
cp web/.env.example web/.env   # VITE_API_URL=https://your-server/api
npm run dev --workspace web
```

`npm run build --workspace web` produces a static site in `web/dist` that works from any path. Serve it from any static host, or copy it into the Laravel `public/` folder (for example `public/relay`) to keep it on the same domain as the API. Without `VITE_API_URL`, the login screen asks for the server.

Sign in with the same e-mail and password as the app. The key stays in this browser only (`localStorage`); **Ajustes → Sair** removes it.

### CLI

```bash
npm run build --workspace cli
npm install -g ./cli          # installs the `relay` command

relay login --server https://your-server   # asks for e-mail and password once
relay pair                                 # type the code on the phone
```

`relay login` creates an API token that can only reach the files module and does not keep your password. You can also create one in the web app (**Ajustes → Linha de comando**) and run `relay login --server … --token manage_…`.

| Command | What it does |
| --- | --- |
| `relay send <files…> [-t folder] [-m text] [-r]` | Uploads files. Without `-t`, they go to the **Transferências** folder, created on first use |
| `relay note "text"` / `echo text \| relay note -` | Sends a note (a link to open on the phone, for example) |
| `relay pull [folder] [-o dir] [--delete] [--watch [s]]` | Downloads what arrived since the last pull; `--delete` moves instead of copying; `--watch` keeps going |
| `relay get <name or #id…>` / `relay get --latest 3` | Downloads specific files |
| `relay ls [folder] [--all]`, `relay folders` | Lists files and folders |
| `relay rm`, `relay mkdir`, `relay status`, `relay key show` | Housekeeping |
| `relay config --default-folder <name>` | Changes the folder used when none is given |

Every listing command takes `--json`. For scripts and CI, the environment variables `RELAY_SERVER`, `RELAY_TOKEN`, `RELAY_KEY`, `RELAY_PIN`, `RELAY_FOLDER` and `RELAY_CONFIG_DIR` override the saved config (`~/.config/relay/config.json`, mode 600).

Folders are matched by name (accents and case ignored), by path (`Work/Invoices`) or by `#id`.

## API contract

Every request sends `Accept: application/json`, and every response comes in an envelope with the payload in `body`. The app and the web app authenticate with `POST auth/login` and `Authorization: Bearer <token>`. The CLI uses `X-API-Token: <token>`, which the backend accepts on every files route below except setting or resetting the secure-folder PIN. On 429 or 503 the clients wait for `Retry-After` (or back off) and retry.

Endpoints used, relative to `API_BASE_URL`:

| Area | Endpoints |
| --- | --- |
| Session | `POST auth/login`, `POST logout`, `GET user` |
| API tokens (Bearer only) | `GET user/api-tokens`, `POST user/api-tokens`, `DELETE user/api-tokens/{id}` |
| Secure folders | `GET user/secure-folders/meta`, `POST user/secure-folders/verify`; Bearer only: `POST user/secure-folders/pin`, `POST user/secure-folders/reset` |
| Pairing | `POST files/pairings`, `GET files/pairings/pending`, `GET files/pairings/{id}`, `POST files/pairings/{id}/approve`, `DELETE files/pairings/{id}` |
| Folders | `GET channels`, `POST folders`, `PATCH folders/{id}`, `DELETE folders/{id}` |
| Messages | `GET folders/{folderId}/messages`, `GET messages/sync?since=&cursor=&limit=`, `POST messages` (JSON, multipart with files, or `uploadIds[]`), `POST messages/checklists`, `PATCH messages/{id}/move`, `PATCH messages/{id}/favorite`, `DELETE messages/{id}` |
| Checklist items | `POST messages/{messageId}/list-items`, `PATCH messages/{messageId}/list-items/{itemId}`, `PATCH messages/{messageId}/list-items/{itemId}/toggle`, `PATCH messages/{messageId}/list-items/reorder`, `DELETE messages/{messageId}/list-items/{itemId}` |
| Attachments | `GET attachments/{id}/download`, `PATCH attachments/{id}/rename`, `PATCH attachments/{id}/move`, `PATCH attachments/{id}/favorite`, `DELETE attachments/{id}`, `POST attachments/bulk-move`, `POST attachments/bulk-delete` |
| Uploads | `POST uploads`, `GET uploads/{uploadId}`, `POST uploads/{uploadId}/chunks`, `POST uploads/{uploadId}/complete`, `DELETE uploads/{uploadId}` |

Uploads are sent in 2 MB chunks, in order; the size declared in `POST uploads` is the encrypted size (plain size + 28). `messages/sync` takes the `syncedAt` of the previous call as `since` and also returns deleted messages (with `deletedAt`) so clients can drop them.

The request and response shapes are in [`app/src/main/java/com/archieapps/relay/data/remote`](app/src/main/java/com/archieapps/relay/data/remote) and [`core/src`](core/src).
