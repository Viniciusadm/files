# Fles

Fles is an Android app for files, notes and checklists kept in folders, with end-to-end encryption, PIN-protected secure folders, chunked uploads and a share target. This repository holds the Android app. It is a client of Manage API, a private Laravel backend that is not published; this README describes the part of its contract the app consumes.

Other clients of the same backend: [`calendar`](https://github.com/Viniciusadm/calendar), [`exercises`](https://github.com/Viniciusadm/exercises), [`oklo`](https://github.com/Viniciusadm/oklo) and [`finances`](https://github.com/Viniciusadm/finances).

## Requirements

- JDK 11+
- Android SDK with `compileSdk` 36; the app runs on Android 7.1+ (`minSdk` 25)
- Android Gradle Plugin 9.2.1 and Kotlin 2.2.10 (from [`gradle/libs.versions.toml`](gradle/libs.versions.toml))

## Building

The backend address is read at build time from `API_BASE_URL` in `local.properties`, which is not versioned. Without it the app points to `https://example.invalid/`:

```properties
API_BASE_URL=http://10.0.2.2:8000/api/
```

Build and install the debug app:

```bash
./gradlew installDebug
```

The release build is signed only when `local.properties` has `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`.

## API contract

Every request sends `Accept: application/json`, and every response comes in an envelope with the payload in `body`. `POST auth/login` returns a token, sent afterwards as `Authorization: Bearer <token>`; `POST logout` ends the session. On 429 or 503 the app waits for `Retry-After` (or backs off) and retries.

Folder names, message texts and file names are encrypted on the device with AES-GCM before they are sent, using a 256-bit key in base64 that never leaves the device; the user copies it by hand to read the same content on another device. The backend stores and returns ciphertext only.

Endpoints used, relative to `API_BASE_URL`:

| Area | Endpoints |
| --- | --- |
| Session | `POST auth/login`, `POST logout`, `GET user` |
| Secure folders | `GET user/secure-folders/meta`, `POST user/secure-folders/pin`, `POST user/secure-folders/verify`, `POST user/secure-folders/reset` |
| Folders | `GET channels`, `POST folders`, `PATCH folders/{id}`, `DELETE folders/{id}` |
| Messages | `GET folders/{folderId}/messages`, `GET messages/sync?since=&cursor=`, `POST messages` (JSON or multipart with files), `POST messages/checklists`, `PATCH messages/{id}/move`, `PATCH messages/{id}/favorite`, `DELETE messages/{id}` |
| Checklist items | `POST messages/{messageId}/list-items`, `PATCH messages/{messageId}/list-items/{itemId}`, `PATCH messages/{messageId}/list-items/{itemId}/toggle`, `PATCH messages/{messageId}/list-items/reorder`, `DELETE messages/{messageId}/list-items/{itemId}` |
| Attachments | `PATCH attachments/{id}/rename`, `PATCH attachments/{id}/move`, `PATCH attachments/{id}/favorite`, `DELETE attachments/{id}`, `POST attachments/bulk-move`, `POST attachments/bulk-delete` |
| Uploads | `POST uploads`, `GET uploads/{uploadId}`, `POST uploads/{uploadId}/chunks`, `POST uploads/{uploadId}/complete`, `DELETE uploads/{uploadId}` |

Downloads use the URLs the backend returns in each attachment. The request and response shapes are in [`app/src/main/java/com/archieapps/fles/data/remote`](app/src/main/java/com/archieapps/fles/data/remote).
