# Receipt → YNAB mobile app (Android‑first, cross‑platform‑ready)

## Goal
Build an Android-first mobile app that captures a receipt photo, performs **on‑device OCR** to extract payee, amount, date, etc., lets the user confirm/edit the fields, then uploads the transaction to the user’s YNAB account via the YNAB API.

---

## Scope (MVP)
- Camera capture + guided framing for receipts
- On‑device OCR (no cloud OCR) and preprocessing (deskew, crop, enhance)
- Extracted fields: **payee, amount, date, currency, tax (if present), line‑items (optional)**
- Review screen with editable fields and category selection
- YNAB integration: OAuth sign‑in (prod) / token test mode; create transaction(s) via API
- Duplicate detection & simple reconciliation suggestions
- Local storage for offline scanning; sync when online
- Privacy-first: image retention minimal and encrypted; user control for uploads

---

## Success criteria / Acceptance tests
- Payee extraction accuracy ≥ 92% on sampled receipts
- Amount extraction accuracy ≥ 99% (including decimal parsing & currency)
- Date parsed correctly ≥ 98% (common formats)
- End‑to‑end: scanned receipt → user confirms → transaction appears in YNAB within 10s
- Secure auth: user can sign in to YNAB and revoke app access successfully

---

## Technical requirements & recommendations 🔧
- **Platform:** Android-first (native Kotlin recommended for MVP) with architecture prepared for later cross‑platform reuse (e.g., Kotlin Multiplatform module for parsing/business logic).
- **OCR engine:** Google ML Kit Text Recognition (on‑device). Provide an adapter so iOS (Vision) or other engines can be swapped.
- **Parsing:** image preprocessing → OCR text → rule-based + small ML (line classification / NER) for robust field extraction. Manual correction in UI required.
- **YNAB integration:** OAuth 2.0 Authorization Code flow for production; support developer/personal token for testing. Store tokens securely (Android Keystore).
- **Privacy & security:** images encrypted at rest, remove images after upload (or keep only if user opts in), TLS for network, minimal permissions.
- **Offline:** queue transactions locally and sync when online; show sync status.

---

## UX / User flow
1. Onboard & connect YNAB (OAuth)
2. Tap “Scan receipt” → guided camera → auto‑capture or manual photo
3. Auto‑extract fields → present editable review page with highlighted OCR text → user confirms/edits
4. Select account/category → submit → show success + link to transaction in YNAB

---

## Deliverables
- Android APK (MVP) with:
  - Camera + OCR + parser + review UI
  - YNAB OAuth + transaction upload
  - Local storage & sync
  - Unit & integration tests, basic e2e tests
- Architecture doc describing how to add iOS later (module boundaries, parser contract)
- Test dataset & evaluation report (accuracy numbers)
- README + deployment steps

---

## Edge cases & optional features
- Split receipts / multi‑transaction receipts (v1 optional)
- Multi‑currency support, receipts in other languages (priority?)
- Attach receipt image to YNAB (if API supports) — otherwise store externally and link in memo
- Auto‑categorization using ML (post‑MVP)

---

## Timeline (example)
- Week 1: Design + OCR parser prototype
- Week 2–3: Camera UI + OCR integration + field extraction
- Week 4: YNAB auth + transaction upload + review UI
- Week 5: Testing, polish, docs

---

## Open decisions
- Framework for MVP: **Kotlin (native)** (recommended) vs Flutter / React Native / KMP
- Include uploading receipt images to YNAB transactions (if supported) or only transaction data
- Languages/locales to support on launch
- Target accuracy thresholds or acceptance metrics adjustments

---
