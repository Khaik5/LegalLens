# LegalLens Agent Guide

## Project

Android Kotlin project using Clean Architecture.

Preferred structure:

```text
com.example.lagallens
├── core/
├── data/
│   ├── datasource/
│   ├── mapper/
│   ├── model/
│   └── repository/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
└── presentation/
    └── feature/
        └── <feature>/
            ├── <Feature>Activity.kt
            ├── <Feature>Contract.kt
            └── <Feature>ViewModel.kt
```

Keep small features inside `app`. Do not create extra Gradle modules unless necessary.

## Architecture Rules

- `presentation` → `domain`, `core`
- `data` → `domain`, `core`
- `domain` must remain pure Kotlin.
- UI must not access DTOs, entities, Retrofit responses, DataStore keys, database, or API directly.
- Repository interfaces → `domain.repository`
- Repository implementations → `data.repository`
- Mapping DTO/entity → domain model happens in `data`.
- Reusable/non-trivial business logic → UseCase.
- Follow existing architecture instead of introducing a new pattern unnecessarily.

`AGENTS.md` is the current architecture source of truth.

`docs/project_notes/decisions.md` records architecture history/reasoning. If memory conflicts with `AGENTS.md`, report the conflict instead of silently changing architecture.

## Screen Pattern

Non-trivial screens use:

```text
State + Event + Effect + ViewModel
```

- `State`: persistent UI state.
- `Event`: user/UI input.
- `Effect`: one-time actions such as navigation, toast, dialog, permissions.

Use `StateFlow` for state and `SharedFlow` for effects.

Activities/Fragments:

- render `State`
- send `Event`
- collect `Effect`
- contain no business logic

Prefer:

```kotlin
_state.update { it.copy(isLoading = true) }
```

Avoid putting one-time navigation/toast flags inside `State`.

## Clean Code

- Keep responsibilities small and clear.
- Avoid duplicated code and unnecessary abstractions.
- Avoid unnecessary `!!` and mutable state.
- Use descriptive names.
- Remove unused imports, dead code, debug code, and commented-out code.
- Handle expected failures.
- Never use empty `catch`.
- Rethrow `CancellationException` before generic coroutine exception handling.
- Do not refactor unrelated code unless required.

## Android Resources

Use resources instead of hardcoding user-visible values.

```text
strings → strings.xml
colors  → colors.xml
dimens  → dimens.xml
```

Naming:

```text
ic_*        icons
img_*       images
bg_*        backgrounds
shape_*     shapes
selector_*  selectors
anim_*      animations
```

Examples:

```text
ic_camera
img_home_banner
bg_camera_capture_button
shape_rounded_rectangle
```

Prefer XML + ViewBinding/DataBinding unless the project intentionally migrates to Compose.

## Verification

During development, a quick Kotlin check may use:

```powershell
gradlew.bat :app:compileDebugKotlin
```

Before commit/push, always run:

```powershell
gradlew.bat :app:assembleDebug
```

Also run applicable:

```powershell
gradlew.bat :app:testDebugUnitTest
gradlew.bat :app:lintDebug
```

If a task name differs, find and use the correct equivalent.

For UI/instrumentation tests:

```powershell
adb devices
gradlew.bat :app:connectedDebugAndroidTest
```

Run UI tests only when an emulator/device and applicable tests exist.

Never claim a skipped test passed.

If verification fails:

```text
READ ERROR → FIX ROOT CAUSE → RUN AGAIN
```

Do not commit or push while required verification is failing.

## Git Workflow

Base branch:

```text
khai_develop
```

Every new feature uses:

```text
khai/<feature-name>
```

Examples:

```text
khai/login
khai/register
khai/camera
khai/crop-image
```

Before new work:

```powershell
git status
git branch --show-current
git fetch origin
git switch khai_develop
git pull --ff-only origin khai_develop
git switch -c khai/<feature-name>
```

If the correct feature branch already exists, continue using it.

Never discard unrelated local changes automatically.

Never automatically use:

```text
git reset --hard
git clean -fd
git push --force
git push -f
```

Never develop a new feature directly on:

```text
main
master
develop
khai_develop
```

## Commit Convention

```text
[Feat]     new feature
[Fix]      bug fix
[Update]   improve existing feature
[Delete]   remove code/feature
[Refactor] internal refactor
[Test]     tests
[Docs]     documentation
[Merge]    actual branch merge
```

Examples:

```powershell
git commit -m "[Feat] Add login feature"
git commit -m "[Fix] Prevent duplicate login requests"
git commit -m "[Update] Improve login UI"
```

Before commit:

```powershell
git status
git diff
git diff --check
```

Commit only intended files.

Never commit passwords, tokens, API keys, signing keys, private credentials, or secrets.

## Push Rules

After verification and commit:

```powershell
git push -u origin khai/<feature-name>
```

For an already tracked branch:

```powershell
git push
```

Never automatically push directly to:

```text
main
master
develop
khai_develop
```

Do not automatically merge into `khai_develop` unless explicitly requested.

GitHub CLI (`gh`) may be used for repository, PR, and Actions operations when needed.

## Project Memory

Use project memory when available:

```text
PROJECT_MEMO.md
docs/project_notes/
├── bugs.md
├── decisions.md
├── key_facts.md
└── issues.md
```

Before substantial work:

- read `PROJECT_MEMO.md`
- read relevant project notes when needed

After meaningful work, update memory when useful:

- important bug/fix → `bugs.md`
- architecture decision → `decisions.md`
- stable project fact → `key_facts.md`
- completed/in-progress work → `issues.md`

Keep memory concise.

Never store secrets in project memory.

## Definition of Done

A feature is done only after:

```text
UNDERSTAND
→ CHECK GIT
→ SYNC khai_develop
→ CREATE/USE khai/<feature>
→ IMPLEMENT
→ REVIEW UI + LOGIC
→ CLEAN CODE
→ BUILD
→ TEST/LINT
→ REVIEW DIFF
→ COMMIT
→ PUSH FEATURE BRANCH
```

Never:

```text
IMPLEMENT → COMMIT → TEST LATER
```

Always:

```text
IMPLEMENT → VERIFY → COMMIT → PUSH
```
