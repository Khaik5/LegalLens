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
- The post-auth dashboard is implemented as a separate `MainActivity` navigation host with a Home dashboard Fragment and independent bottom-navigation resources. Contract List/Search/Filter is available on `khai/contracts`, including a Figma-aligned filter bottom sheet for contract type, processing status, and risk level, with local visual data until a backend contract is available. Notification List is available on `khai/notifications`; it displays the Figma-aligned empty state until notification data is available, while category filters and local read-state interactions remain ready for integration. Profile is available with Figma-aligned main, edit, settings, change-password, and logout-dialog UI. Settings persists language labels and System/Light/Dark selection locally; the selected appearance applies app-wide through DayNight resources, while existing app copy remains Vietnamese. The Add Contract entry is connected to the upload-source flow.
- `SplashActivity` is the launcher. It routes first-time users to the three-page onboarding flow and routes completed users to `HomeActivity` for authentication.
- Camera intake routes from the upload-source Camera card to permission, capture, scan-page management, and OCR-processing screens. It uses CameraX, on-device OpenCV checks, and ML Kit text recognition; backend OCR and analysis remain pending an API contract.
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

## 2026-09-12 status

- Add Contract has a local UI-test flow while the backend contract is unavailable: PDF, DOCX, and Gallery selections show metadata on Upload Progress, advance from 68% to 100%, then open OCR Processing with the same metadata. This transition must be replaced with typed backend upload/OCR outcomes before production.
- Camera remains a separate flow: source selection opens Camera Permission, then capture, page management, and OCR Processing.
- Upload Error remains unreachable until a backend contract supplies typed upload/OCR outcomes. OCR Review opens from the local OCR Result mock flow.
- The supplied Figma references cover Add Contract source selection, Upload Error, and OCR Processing. There is no approved Figma frame for Upload Review/Upload List or Upload Success; do not invent or implement those screens until a design is supplied or the owner explicitly approves their scope.
- OCR Result is implemented as a local mock-data screen with a confidence badge, read-only contract preview, and separate Chỉnh sửa and Cần xem lại actions. Chỉnh sửa opens the local editor and returns edited text; Cần xem lại opens OCR Review.
- OCR Review is a local confirmation screen for low-confidence OCR warnings. It displays two safe sample warnings, returns a confirmation result to OCR Result, and does not persist contract content locally.
- OCR Failure is available as a Figma-aligned presentation route for typed OCR failures. It preserves document metadata when retrying OCR; the current local OCR preview remains a success-only mock until the backend returns a typed failure outcome.

## 2026-09-14 status

- OCR Saved now has a standalone Figma-aligned presentation screen under `presentation/feature/ocr/saved`. It accepts document metadata through its intent and keeps Contract Detail and AI Analysis navigation unconnected until their approved destinations are integrated.
- The local OCR flow now routes OCR Review confirmation to OCR Saved. OCR Processing accepts an internal `shouldSimulateFailure` test flag: false keeps the success preview; true opens OCR Failure, whose retry returns to the success preview. Replace this flag with a typed backend outcome before production.

## 2026-09-18 status

- AI Analysis is available under `presentation/feature/analysis`: OCR Saved opens Start with the selected document name, Start opens Processing, and the local Processing presentation advances to the Figma-aligned Result screen.
- Result presents local sample score, key facts, risks, references, and six section entry cards. Replace local sample data and the timed transition with typed backend analysis outcomes before production; section-detail destinations remain pending their approved Figma designs.

## 2026-09-19 status

- The result Summary entry opens a Figma-aligned contract summary detail screen under `presentation/feature/analysis/summary`; its existing-style primary action opens `presentation/feature/analysis/keyfacts` with the same document metadata. The Important Information result entry remains available directly. The salary Key Fact opens a Figma-aligned clause-source bottom sheet, while the remaining source links stay visual-only until their source excerpts are supplied. All display values remain local until the backend provides typed, contract-scoped responses.
- The Analysis Result Parties entry opens the Figma-aligned `presentation/feature/analysis/parties` screen with the same document metadata. Its employer and employee values are anonymized local samples until typed, contract-scoped backend responses are available.

## 2026-09-19 OCR retry status

- OCR Processing and Analysis Processing now share the same state-driven timeline component. A simulated OCR failure still stops after Image Analysis; Retry restarts from Image Analysis, advances through Text Recognition and Ready for Review, and only then opens OCR Result. Timed local behavior remains a presentation placeholder until the backend returns typed OCR progress and outcomes.

## 2026-09-20 status

- The Analysis Result Rights and Obligations entry now opens the Figma-aligned `presentation/feature/analysis/obligations` screen with the same document metadata. It displays local, anonymized obligation examples for employer and employee; tab and clause-source actions remain display-only because the supplied design does not define destinations or typed analysis data.
- The Analysis Result Risk entry now opens `presentation/feature/analysis/risk`, which presents the Figma-aligned local score, severity summary, and legal disclaimer. The high-risk penalty finding opens `presentation/feature/analysis/risk/detail` with its reason, clause excerpt, legal explanation, legal-reference card, and suggested negotiation action; the legal-reference card remains visual-only and the other local findings retain the unavailable detail state until typed backend analysis data and their approved designs are available.

## 2026-09-22 status

- Analysis Processing now has a Figma-aligned failure presentation route under `presentation/feature/analysis/failure`. Its local `shouldSimulateFailure` flag preserves the existing success path by default, opens the failure screen with the current document metadata when enabled, retries through Processing, and returns to `MainActivity` from the home action. Replace the local flag with a typed backend analysis outcome when an API contract is available.
- The Analysis Result legal-basis entry now opens the Figma-aligned legal-reference list under `presentation/feature/analysis/legalreferences`. The list preserves the current document context and supports local filtering across three design-provided sample references. The approved `legal-02-detail` screen is available for the Commercial Law 2005 reference and links its related-risk card to the existing risk-detail screen; the other two reference details remain unavailable. All displayed legal text remains presentation-only sample data until typed backend citations are available.

## 2026-09-23 status

- The legal-reference card on the high-risk penalty detail now opens the Figma-aligned citation-source screen under `presentation/feature/analysis/risk/citation`. It explains why the citation supports the original finding, highlights the statutory 8% limit, and returns to the originating risk detail from either navigation action. The finding and citation remain local presentation samples until the backend supplies typed, contract-scoped evidence.

## 2026-10-02 status

- Contract-bound AI Chatbot is available under `presentation/feature/chatbot`. Contract Detail now opens the Figma-aligned chat screen with the selected contract name and type. Its initial state shows an assistant greeting with four Figma-provided questions; selecting one starts the local sample conversation. The supplied termination-question state renders two contract-source reference cards, the parking-support question renders the approved no-evidence warning, and the local penalty-question response retains its citation card that opens the existing commercial-law detail screen. The Figma navigation drawer opens from the chat menu, starts the Figma-aligned empty new-conversation state with three suggested questions, and opens the local Figma-aligned conversation-history screen. The local “Tóm tắt nghĩa vụ của bên thuê” request renders the approved retryable connection-error card; retry swaps it for the local fallback response. History entries and search remain presentation-only until conversation sync is connected; help, attachment, and backend/RAG-based answers remain pending a typed API contract.
