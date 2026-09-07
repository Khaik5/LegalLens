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

- Mobile register and home screen files exist in the working tree but were not reviewed or changed while creating this memory system.
- No backend/API contract is present in this repository yet. Treat endpoint shapes, authentication flows, AI response schemas, and legal-reference schema as open integration work.
- Onboarding flow is in `presentation.feature.onboarding`: `ui` hosts `OnboardingActivity`, `adapter` hosts `OnboardingPagerAdapter`, `viewmodel` hosts `OnboardingViewModel`, and `contract` separates `OnboardingUiState`, `OnboardingEvent`, and `OnboardingEffect`. `OnboardingPage` supplies page content; `SplashActivity` is the launcher entry and routes into the onboarding flow.

## 2026-09-05 status

- Splash and three onboarding pages are implemented on `phuoc/onboarding` from Figma frames `auth-01-splash` through `auth-04-onboarding-03`. Onboarding now uses one activity with `ViewPager2`; its header, buttons, and dots remain fixed while page content changes.
- Onboarding completion is stored locally with DataStore after Start or Skip. Splash checks the completion state before opening onboarding; the completed-user destination remains the pending Auth/OTP integration point.
- App build, instrumentation test compilation, unit tests, and lint pass. Runtime instrumentation remains pending until an Android emulator or device is connected.

## 2026-09-06 status

- The canonical Phuoc working tree is `D:\legallens\LegalLens` on `phuoc/add-contract`. Splash and onboarding source/assets remain available alongside the Add Contract work.
- Add Contract source selection and the OCR text-review screen are implemented in the same flow. OCR review supports editing, basic text formatting, restoring the generated text, and local save feedback; the backend OCR/upload integration remains pending.
- The Add Contract flow now routes a locally selected document to the Figma-aligned upload-progress UI. It presents local file metadata and a cancellable placeholder processing state; it does not upload or perform OCR until the backend contract is available.
- The Figma-aligned upload failure state is available with a retry path back to file selection and a return-to-caller path. The backend must provide a typed upload/OCR failure before this state can be reached from live processing.
