# LegalLens Project Memo

## Read first

- Product: Android application that helps people understand Vietnamese employment and recruitment contracts. It is an analysis and reference tool, not legal advice or a lawyer replacement.
- Current research scope: employment and recruitment-related contracts under Vietnamese law. Do not silently broaden the jurisdiction or contract types.
- Android stack: Kotlin, XML, ViewBinding, MVVM, Retrofit. Keep features in `app` unless a real modularization need appears.
- Architecture: presentation depends only on domain/core; data implements domain repositories; domain is pure Kotlin. Non-trivial screens use State + Event + Effect + ViewModel.
- Backend boundary: NestJS REST API owns JWT/RBAC, MySQL, Cloud Storage, Google Cloud Vision OCR, Gemini, Qdrant, RAG, and legal-data maintenance. The app must not call those services directly except through its backend API.
- Privacy: contracts are sensitive. Never place contract content, tokens, keys, credentials, or personal data in source, logs, screenshots, fixtures, or project memory.

## Product capabilities

- Account and profile management.
- Contract import (PDF, Word, image), camera scanning, OCR-text review, and contract library/search/history.
- AI summary, extracted fields, rights/obligations, clause-level risks, severity/score, explanations, legal references, and recommended next actions.
- Contract-bound chatbot with cited legal context and conversation history.
- Template-based contract drafting, preview/edit, and export.
- Contract comparison, reminders/notifications, and result export.

See [requirements.md](docs/project_notes/requirements.md) for acceptance-oriented detail and [proposal_source.md](docs/project_notes/proposal_source.md) for source notes and inconsistencies.

## Working protocol

1. Before substantial work, read this memo, `AGENTS.md`, and relevant files in `docs/project_notes/`.
2. Before an architecture change, check `decisions.md`; report a conflict rather than silently replacing a decision.
3. Record meaningful decisions, stable facts, resolved bugs, and work status in the appropriate note. Keep entries dated and concise.
4. Verify Android work proportionately. Before commit/push, follow the commands and branch workflow in `AGENTS.md`.

## Current status

- Mobile home, login, register, forgot-password, OTP verification, and success UI screens are implemented in the working tree. Registration and password-reset paths currently simulate OTP delivery and verification until the backend contract is available. Auth/OTP package migration is ready for owner testing on `khai/auth` and follows the feature-first Clean Architecture layout in `AGENTS.md`.
- The post-auth dashboard is implemented as a separate `MainActivity` navigation host with a Home dashboard Fragment and independent bottom-navigation resources. Contract List/Search/Filter is available on `khai/contracts` with local visual data until a backend contract is available. Notifications, profile, and add-contract entries remain shared integration placeholders.
- `SplashActivity` is the launcher. It routes first-time users to the three-page onboarding flow and routes completed users to `HomeActivity` for authentication.
- Camera intake routes from the upload-source Camera card to permission, capture, scan-page management, and OCR-processing screens. It uses CameraX, on-device OpenCV checks, and ML Kit text recognition; backend OCR and analysis remain pending an API contract.
- No backend/API contract is present in this repository yet. Treat endpoint shapes, authentication flows, AI response schemas, and legal-reference schema as open integration work.
