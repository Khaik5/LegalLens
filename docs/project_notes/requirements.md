# Product Requirements Map

This is a concise implementation map distilled from the LegalLens proposal. It is not an API contract; backend endpoint/request/response details remain to be agreed with the backend team.

## Scope and safety rules

- Analyze Vietnamese employment and recruitment-related contracts first.
- Accept PDF, Word, image/scanned contracts, and directly drafted content.
- Explain results in plain language for non-lawyers.
- Show that results are AI-supported reference information, not legal advice.
- Do not send a document through the legal-analysis pipeline when backend validation identifies it as non-legal/non-contract content.

## User capabilities

### Account and personal settings

- Register, sign in, manage profile/settings, and receive authorized access by role.

### Contract library

- Upload PDF, Word, or image contracts.
- Capture pages with the mobile camera.
- Rename, edit, delete, search, filter, compare, and revisit contracts and saved analyses.
- Receive reminders for important contract dates and relevant system activity.
- Export analysis results.

### OCR and processing

- Send scanned images for OCR through the backend.
- Let the user review and correct OCR text before analysis.
- Surface processing states and recoverable failures clearly.

### Analysis and risk review

- Show a contract summary.
- Extract parties, signing date, duration, salary/value, probation, payment terms, compensation, and important dates where available.
- Show each party's rights and obligations.
- Flag missing, unclear, unusual, potentially disadvantageous, dispute-prone, or potentially non-compliant clauses.
- For each finding, show risk level/score, affected clause, explanation, legal reference, and suggested next action when available.

### Contract chatbot

- Ask questions about a selected contract, its clauses, rights, obligations, and risks.
- Show answers grounded in contract content and the legal knowledge base, including references/explanations returned by the backend.
- View and manage conversation history.

### Contract generation

- Select a contract type and template.
- Enter required fields; optionally add/customize clauses.
- Preview and edit the generated document, then export PDF or Word.

## Admin capabilities (backend/admin portal scope)

- Manage accounts, roles/permissions, legal documents/articles, RAG knowledge, templates, notifications, configuration, logs, usage, and service status.
- Admin UI is not part of the current Android mobile feature scope unless explicitly requested.

## Delivery order for mobile work

1. Authentication and session handling.
2. Contract library, file/camera intake, and safe local UI states.
3. OCR review and processing status.
4. Analysis summary and clause-level risk detail with legal references.
5. Contract-bound chatbot and history.
6. Search/filter, compare, export, notifications, and generation after core analysis works.

## Definition of ready for a backend-integrated screen

- API endpoint, authentication requirement, and error behavior are documented.
- Request/response DTOs include processing status and structured analysis/legal-reference data where applicable.
- Sensitive data retention/display rules are agreed.
- Loading, empty, success, retryable failure, unauthorized, and cancellation states are designed.
- Domain model, repository interface, use case, mapper, State/Event/Effect, and resource strings are identified.
