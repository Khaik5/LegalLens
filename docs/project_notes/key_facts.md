# Key Facts

Only store non-sensitive, durable facts. Never store passwords, API keys, tokens, private keys, contract text, user data, or personal contact details.

## Product and scope

- Product name: LegalLens.
- Purpose: contract analysis and risk assessment for users without legal expertise.
- Initial jurisdiction: Vietnam.
- Initial contract scope: employment and recruitment-related contracts.
- Product boundary: reference and decision support; not a substitute for lawyers or professional legal advice.

## Android application

- Language/UI: Kotlin, XML, ViewBinding, MVVM.
- HTTP client: Retrofit.
- Package namespace: `com.example.lagallens` (spelling is already established in the project; do not rename casually).
- Screen convention: State + Event + Effect + ViewModel; `StateFlow` for state and `SharedFlow` for one-off effects.
- Resource policy: user-visible strings, colors, and dimensions go in resources; XML + ViewBinding is preferred.

## Backend and integrations proposed

- Backend: Node.js, TypeScript, NestJS; REST API documented with Swagger/OpenAPI.
- Authentication: JWT with role-based access control.
- Relational data: MySQL.
- File storage: Google Cloud Storage.
- OCR: Google Cloud Vision API.
- AI: Gemini API for summary, extraction, risk analysis, explanations, and chatbot answers.
- Legal retrieval: RAG backed by Qdrant vector database and a maintained Vietnamese legal knowledge base.
- The Android app must use the backend API rather than embed service credentials or direct cloud/AI calls.

## Core domain language

- Contract: uploaded or drafted source plus metadata and processing state.
- OCR review: user correction of extracted text before AI analysis.
- Analysis: summary, extracted fields, parties, dates, salary/value, duration, payment terms, rights, obligations, clauses, and risk findings.
- Risk finding: clause, severity/score, rationale, legal reference, and suggested action.
- Legal reference: source law/regulation and applicable provision returned by RAG.

## Security and quality constraints

- Contract data can be personal, confidential, financial, or business-sensitive.
- Authorization must protect accounts, contracts, analyses, conversations, templates, and admin functions.
- Validate document relevance before expensive OCR/AI processing when backend support is available.
- Make OCR edits reviewable before analysis.
- Legal information needs a maintenance process because regulations can change.
- Optimize for understandable Vietnamese content, stable processing, and users with limited legal knowledge.
