# Receipt → YNAB MVP Implementation Plan

## 1) Delivery objective
Ship an Android MVP that scans receipts, extracts structured fields on-device, lets users correct data, and creates YNAB transactions reliably with offline queueing and privacy-first defaults.

Primary release goal: transaction created in YNAB in under 10 seconds after user confirmation on a normal network.

## 1.1) Handoff status (updated 2026-02-20)

### Completed
- [x] Android multi-module scaffold created (`app`, `domain`, `data-*`, `feature-*`)
- [x] Core domain contracts and models implemented (`OcrEngine`, `ReceiptParser`, `TransactionSyncService`, core entities)
- [x] Local Room persistence baseline implemented (receipt + queue entities/DAO/repository)
- [x] YNAB data layer baseline implemented (Retrofit API + repository skeleton)
- [x] Hilt DI wiring implemented across app/data/domain
- [x] Compose navigation shell implemented (Scan / Review / Auth / History)
- [x] Duplicate detection v1 use case + unit test added
- [x] Build blockers fixed (Kotlin dependency compatibility, theme/resource linking, module dependency issues)

### In progress / partially complete
- [~] Week 2 capture + OCR flow
  - [x] Camera permission + CameraX preview/capture integrated
  - [x] Captured image bytes wired into extraction flow
  - [x] ML Kit OCR engine integrated
  - [x] Image preprocessing baseline implemented (contrast + grayscale candidate generation)
  - [x] OCR fallback baseline implemented (multi-candidate scoring with rotation variants)
  - [ ] Advanced preprocessing (deskew/crop) still pending
  - [ ] Blur-specific fallback handling still pending
- [~] Week 3 review UX + offline queue
  - [x] Editable review fields implemented (payee, amount, date)
  - [x] OCR confidence + OCR context displayed in review UI
  - [ ] Queue state UX still pending
  - [ ] WorkManager background sync still pending
  - [ ] Duplicate warning prompt still pending

### Next recommended tasks
1. Finish Week 2 robustness items (preprocessing + OCR fallbacks)
2. Implement Week 3 offline queue state UX + WorkManager sync
3. Add duplicate warning prompt in review submit path
4. Begin Week 4 OAuth + YNAB account/category loading

## 2) Recommended stack (MVP)
- **App:** Native Android (Kotlin), Jetpack Compose UI
- **Architecture:** Clean-ish layering with feature modules + shared domain/parser contracts
- **OCR:** Google ML Kit Text Recognition (on-device)
- **Image pipeline:** CameraX + OpenCV-style preprocessing (deskew/crop/enhance)
- **Local data:** Room + SQLCipher (or encrypted file + Room for metadata)
- **Sync:** WorkManager with constrained retries (network required)
- **Networking:** Retrofit + OkHttp
- **Auth/security:** OAuth 2.0 auth code flow + PKCE, tokens in Android Keystore-backed storage
- **DI:** Hilt
- **Testing:** JUnit, MockWebServer, Espresso/Compose UI tests

## 3) Module boundaries (Android-first, cross-platform-ready)
Use boundaries that allow parser/business logic migration to Kotlin Multiplatform later.

### 3.1 Modules
- `app`  
  Compose app shell, navigation, feature wiring, dependency graph.
- `feature-scan`  
  Camera capture flow, guided framing, capture state machine.
- `feature-review`  
  Editable extracted fields, category/account selection, submit action.
- `feature-auth`  
  YNAB connect/disconnect screens, OAuth handlers, token test mode.
- `feature-history`  
  Local queue/sync status list.
- `data-ocr`  
  OCR adapter contract + ML Kit implementation.
- `data-parser`  
  Text normalization, heuristics, confidence scoring, optional line-item extraction.
- `data-ynab`  
  YNAB API client, DTO mapping, error mapping.
- `data-local`  
  Room entities/DAOs, encrypted image store, queue repository.
- `domain`  
  Use cases: extract fields, detect duplicates, enqueue submit, sync queued items.

### 3.2 Core contracts
- `OcrEngine.recognize(image): OcrDocument`
- `ReceiptParser.parse(ocr: OcrDocument, locale): ParsedReceipt`
- `TransactionSyncService.enqueue(parsedReceipt)`
- `TransactionSyncService.syncPending()`

This keeps OCR/parser/sync orchestration testable and portable if an iOS client consumes shared parsing logic later.

## 4) Data model (MVP)
- `ReceiptScan`
  - `id`, `createdAt`, `imageUriEncrypted`, `ocrText`, `parseVersion`
  - `payee`, `amountMinor`, `currency`, `date`, `taxMinor?`
  - `lineItemsJson?`, `confidenceJson`, `userEditedFieldsJson`
  - `status` (`draft`, `queued`, `synced`, `failed`)
- `QueuedTransaction`
  - `id`, `receiptScanId`, `ynabBudgetId`, `ynabAccountId`, `ynabCategoryId?`
  - `dedupeFingerprint`, `attemptCount`, `lastError`, `nextRetryAt`, `createdAt`
- `AuthState`
  - `accessTokenRef`, `refreshTokenRef`, `expiresAt`, `selectedBudgetId`

## 5) End-to-end flow
1. Capture receipt image via CameraX with guide overlay.
2. Preprocess image (crop, deskew, contrast) locally.
3. OCR on-device; persist raw text + image reference locally.
4. Parse fields with confidence scores.
5. Show review screen with editable fields and OCR context.
6. User selects YNAB account/category and confirms.
7. Create queue item and trigger sync worker.
8. Sync worker posts transaction to YNAB, stores remote transaction id, marks synced.

## 6) Duplicate detection & reconciliation (simple MVP)
- Generate a fingerprint from normalized payee + amount + date (+ optional last4 card hint if available).
- On submit, compare against:
  - local synced receipts in last 14 days
  - latest fetched YNAB transactions in same window
- If match confidence high, show “Possible duplicate” prompt with keep/cancel choices.

## 7) Privacy and security defaults
- Default behavior: do not upload receipt images to third parties.
- Encrypt local image files at rest; keep only while needed for review/history.
- Add user setting: auto-delete image after successful YNAB sync.
- Request minimum permissions (camera only for MVP).
- Use TLS, keystore token storage, and structured redaction in logs.

## 8) Milestone plan (5-week baseline)

### Week 1 — Foundations + parser spike ✅ Mostly complete
- App skeleton, modules, navigation, DI, local DB.
- Camera prototype + sample image ingestion.
- OCR adapter and parser prototype on sample dataset.
- Definition of parser confidence rules and error taxonomy.

Exit criteria:
- Parse pipeline works on at least 50 representative receipts offline. *(dataset harness still pending)*

### Week 2 — Capture + OCR production flow 🟡 In progress
- Guided camera UX and capture controls.
- Image preprocessing pipeline.
- OCR integration with robustness handling (blur, rotation, glare fallbacks).
- Draft review UI populated from parser output.

Exit criteria:
- User can capture a receipt and reach editable review in one flow. *(capture→review path implemented; robustness and preprocessing pending)*

### Week 3 — Review UX + offline queue ⏳ Not started
- Complete editable fields and category/account selection UI.
- Local queue model + sync status states.
- WorkManager background sync and retry strategy.
- Duplicate detection v1 prompt.

Exit criteria:
- Confirmed receipt can be queued offline and auto-sync when network returns.

### Week 4 — YNAB integration ⏳ Not started
- OAuth 2.0 + PKCE flow.
- Budget/account/category loading.
- Create transaction endpoint integration and idempotency handling.
- Token test mode and error UX.

Exit criteria:
- End-to-end flow creates transaction in YNAB for authenticated user.

### Week 5 — Quality, metrics, docs ⏳ Not started
- Accuracy evaluation run + fixes for parsing edge cases.
- Integration/UI tests and smoke e2e.
- Security/privacy checks and retention settings.
- README, architecture notes, release checklist.

Exit criteria:
- MVP acceptance checks pass and APK is release-candidate ready.

## 9) Test strategy mapped to acceptance criteria

### 9.1 Accuracy harness
- Curate labeled receipt dataset covering major date/currency formats.
- Evaluate:
  - payee precision/recall and top-1 accuracy
  - amount exact-match rate (minor units)
  - date parse exact-match rate
- Gate release on:
  - Payee ≥ 92%
  - Amount ≥ 99%
  - Date ≥ 98%

### 9.2 Integration tests
- MockWebServer for YNAB API success/failure/token refresh.
- Offline-to-online sync with WorkManager test driver.
- Duplicate detection test matrix (same-day duplicates, near matches, non-duplicates).

### 9.3 UI/e2e smoke
- Capture/import sample image → edit → submit → success state.
- OAuth connect and disconnect/revoke path.
- Failed sync retry and user-visible error state.

## 10) Release readiness checklist
- OAuth production app configured with redirect URIs.
- Keystore-backed token storage verified on target Android versions.
- Privacy settings visible and defaults validated.
- Crash/ANR baseline tested on low-end Android devices.
- Telemetry events (privacy-safe) available for funnel diagnostics.

## 11) Open decisions with defaults for execution now
To avoid schedule drift, use these defaults unless explicitly changed:
- MVP framework: **Native Kotlin Android**
- Receipt image upload to YNAB: **No** (store locally only)
- Launch locales: **en-US first**, parser architecture locale-aware for expansion
- Acceptance metrics: **keep current thresholds**

## 12) Risks and mitigations
- OCR failures on poor photos  
  Mitigation: capture guidance + preprocessing + review correction.
- YNAB rate limit or API errors  
  Mitigation: queue + exponential backoff + clear sync state.
- Token expiry edge cases  
  Mitigation: centralized auth interceptor with refresh retry once.
- Parser quality variability by locale  
  Mitigation: explicit locale strategy and phased locale rollout.