# Bug Log

Record only resolved, instructive defects. Do not log credentials, contract content, personally identifiable information, or raw AI prompts/responses.

## Entries

### 2026-09-06 - Successful login reopened LoginActivity

- **Issue**: Valid local credentials reopened the login screen instead of continuing to the post-auth flow.
- **Root cause**: `LoginActivity` handled its success effect by starting `LoginActivity` again.
- **Solution**: Login now clears the task and opens the dashboard navigation host directly; the Success flow remains for OTP completion.
- **Prevention**: Keep one-shot navigation effect names aligned with their concrete destination and validate the complete post-auth activity stack.

## Entry format

### YYYY-MM-DD - Brief title

- **Issue**: What failed.
- **Root cause**: Why it failed.
- **Solution**: What fixed it.
- **Prevention**: How to avoid recurrence.
