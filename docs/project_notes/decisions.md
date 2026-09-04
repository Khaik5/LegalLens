# Architectural Decisions

## ADR-001: Preserve a narrow legal scope (2026-09-04)

**Context:** The proposal targets users who need help interpreting legal contracts, but legal accuracy and jurisdiction vary significantly.

**Decision:** Support Vietnamese employment and recruitment-related contracts first. Present outputs as explanations and reference information, not legal advice.

**Alternatives considered:**

- All contract types and jurisdictions -> Deferred because legal coverage and validation would be too broad.
- A lawyer-replacement experience -> Rejected because AI output must remain decision support.

**Consequences:**

- Legal sources, prompts, risk rules, test cases, and copy must identify Vietnamese employment-law context.
- High-risk results must explain the flag and guide users to verify with a qualified professional.

## ADR-002: Keep the Android app thin and use Clean Architecture (2026-09-04)

**Context:** The proposal includes mobile UI plus OCR, AI, RAG, storage, database, and authentication services.

**Decision:** The Android app uses Kotlin, XML, ViewBinding, MVVM, Retrofit, and the existing `app`-level Clean Architecture layout. It accesses backend capabilities only through domain repositories/use cases.

**Alternatives considered:**

- Calling Gemini, Cloud Vision, Qdrant, or MySQL from the app -> Rejected: exposes credentials and bypasses authorization/control.
- Putting business rules in Activities/ViewModels -> Rejected: violates project architecture and harms testability.

**Consequences:**

- DTO/entity mapping belongs in `data`; UI receives domain/UI models only.
- New non-trivial screens use State + Event + Effect + ViewModel with `StateFlow` and `SharedFlow`.

## ADR-003: Explain every AI risk with traceable context (2026-09-04)

**Context:** A risk label without a clause, reason, and legal source may mislead users.

**Decision:** A detected issue should carry the affected clause, risk level/score, plain-language reason, relevant legal reference, and a bounded suggested next action when the backend provides them.

**Alternatives considered:**

- Summary-only analysis -> Rejected: users cannot assess why they should care.
- Uncited chatbot/analysis answers -> Rejected: does not meet the proposal's RAG-based explanation goal.

**Consequences:**

- The backend API contract needs explicit, structured fields for citations and analysis status.
- The Android UI must distinguish AI analysis from verified legal advice.
