# Proposal Source Notes

## Source

- Input document: `C1SE36-Capstone-Proposal ver1.1 (1).docx`.
- Proposal version/date: 1.1, 2026-08-28.
- Project period listed: 2026-08-10 to 2026-12-05.
- This file stores only proposal-derived facts needed for implementation. It intentionally omits team contact details and other unnecessary personal data.

## Source of truth hierarchy

1. `AGENTS.md` is the architecture and workflow source of truth for this repository.
2. Current agreed API contracts and current law/knowledge-base content supersede proposal prose for implementation details.
3. The proposal defines the initial product vision and scope.

## Proposal inconsistencies to resolve before implementation

- The cover table still refers to a different project title, `EventAnyWhere`; treat this as an editing artifact, not a LegalLens requirement.
- One technical-constraints paragraph mentions Flutter, C#, HTML/CSS, JavaScript, Python, MongoDB, ZEGOCLOUD, and Map. It conflicts with the LegalLens stack stated elsewhere (Kotlin/XML, NestJS, MySQL, GCP, Gemini, Qdrant) and should not drive this repository.
- The proposal spells Qdrant once as `Quadrant`; use Qdrant.
- An old 2021 master-plan table conflicts with the 2026 proposal timeline; use the 2026 project period only after confirming current team planning.
- The stated team composition says 5 people including 2 developers, while the role table lists three developers. Do not derive ownership from this document without confirmation.

## External dependencies that need decisions later

- Backend API base URL, endpoint schemas, versioning, retry policy, and error model.
- Authentication refresh/session-expiry behavior.
- Android file size/type limits and local file-handling policy.
- Camera/image compression, OCR polling/asynchronous job handling, and cancellation.
- Legal knowledge-base provenance, update cadence, citation schema, and AI evaluation criteria.
- Data retention/deletion, encryption, export, notification, and consent requirements.
