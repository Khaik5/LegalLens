# LEGALLENS AGENT GUIDE

## 1. Rules & Context

Architecture: **Clean Architecture + MVVM + UiState/UiEvent/UiEffect + DI**.

Read before significant work:

1. `AGENTS.md`
2. `PROJECT_MEMO.md`
3. `docs/team_assignment.md`
4. only relevant `docs/project_notes/*`

Priority:

- architecture/Git/build/UI → `AGENTS.md`
- current owner/status/branch → latest owner-approved `team_assignment.md`
- conflict → **STOP, REPORT, DO NOT GUESS**

Do not change architecture, package name, base branch, framework, or another owner's feature without approval.

---

## 2. Project Structure

```text
com.example.lagallens
│
├── core/
│   ├── error/
│   ├── extension/
│   ├── constants/
│   ├── network/
│   └── utils/
│
├── data/
│   ├── datasource/
│   │   ├── remote/
│   │   │   ├── api/
│   │   │   ├── AuthRemoteDataSource.kt
│   │   │   ├── ContractRemoteDataSource.kt
│   │   │   ├── UploadRemoteDataSource.kt
│   │   │   └── AnalysisRemoteDataSource.kt
│   │   │
│   │   └── local/
│   │       ├── datastore/
│   │       └── database/
│   │           ├── dao/
│   │           └── entity/
│   │
│   ├── model/
│   │   ├── request/
│   │   ├── response/
│   │   └── dto/      # create only when actually needed
│   │
│   ├── mapper/
│   └── repository/
│
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
│
├── presentation/
│   ├── common/
│   │   ├── adapter/
│   │   ├── view/
│   │   ├── dialog/
│   │   └── utils/
│   │
│   └── feature/
│       └── <feature>/
│           ├── ui/
│           ├── viewmodel/
│           ├── contract/
│           ├── adapter/
│           ├── view/
│           └── utils/
│
└── di/
    ├── NetworkModule.kt
    ├── DatabaseModule.kt
    ├── RepositoryModule.kt
    └── UseCaseModule.kt
```

Structure rules:

- organize `presentation` by **feature**, never global `fragment/`, `viewmodel/`, `state/`, etc.
- no `FeatureContract.kt`; `UiState`, `UiEvent`, `UiEffect` are separate files.
- no empty package just to match the tree.
- feature-only UI code stays inside its feature.
- move to `presentation/common/` only when reused by multiple features.
- same UI, only title/image/text/data differs → reuse one Fragment + args/UiState.
- different behavior/flow → separate screen/feature.

---

## 3. Layer Rules

Dependency direction:

```text
presentation → domain, core
data         → domain, core
di           → wires dependencies
domain       → pure Kotlin only
```

### `core`

Shared technical code only: errors, extensions, constants, network helpers/results, date/file/validation utilities. No feature business logic.

### `data`

Real data access: API, DataStore, Room, files/cache, transport models, mapping, repository implementations.

Rules:

- Retrofit API → `data/datasource/remote/api/`
- remote source → `<Feature>RemoteDataSource`
- Room DAO/entity → `data/datasource/local/database/`
- request/response models → `data/model/request|response/`
- `dto/` only when a distinct transport model is truly needed
- mapper → `data/mapper/`
- repository implementation → `data/repository/`
- never expose Request/Response/DTO/Entity to UI
- Mapper transforms shape only; no business logic
- do not create duplicate transport models for the same data

### `domain`

Pure Kotlin only: domain models, repository interfaces, UseCases.

Forbidden dependencies:
`android.*`, Activity, Fragment, View, Retrofit, Room, DataStore.

Rules:

- repository interface → `domain/repository/`
- business/data operation → UseCase
- simple UI-only logic → ViewModel directly
- do not create fake UseCases for trivial UI actions

### `presentation`

Contains UI, ViewModel, UiState, UiEvent, UiEffect, Adapter, Dialog, Component, UI helpers.

Activity/Fragment only:

```text
render UiState
send UiEvent
collect UiEffect
handle Android UI behavior
```

Never put Retrofit/DAO/DataStore/DTO mapping/RepositoryImpl/business logic in Activity, Fragment, Adapter, or ViewModel.

---

## 4. UI State Pattern

Standard feature:

```text
<Feature>Fragment.kt
<Feature>ViewModel.kt
<Feature>UiState.kt
<Feature>UiEvent.kt
<Feature>UiEffect.kt
```

Meaning:

- `UiState` = persistent/renderable screen state
- `UiEvent` = UI/user input sent to ViewModel
- `UiEffect` = one-shot UI action

Use:

```text
UiEvent → ViewModel → UseCase when needed → UiState / UiEffect
```

Flow convention:

```kotlin
private val _uiState = MutableStateFlow(FeatureUiState())
val uiState = _uiState.asStateFlow()

private val _uiEffect = MutableSharedFlow<FeatureUiEffect>()
val uiEffect = _uiEffect.asSharedFlow()

fun onEvent(event: FeatureUiEvent) { ... }
```

Use `StateFlow` for state; `SharedFlow` for effects.

Effects include navigation, toast/snackbar, dialog, permission, file/gallery picker, opening another app. Never store one-shot actions as UiState flags.

---

## 5. Data Flow

```text
UI
→ UiEvent
→ ViewModel
→ UseCase
→ Repository interface
→ RepositoryImpl
→ Remote/LocalDataSource
→ API/DataStore/Room/File
```

Return:

```text
API/Local
→ Response/DTO/Entity
→ Mapper
→ Domain Model
→ Repository
→ UseCase
→ ViewModel
→ UiState/UiEffect
→ UI
```

Do not bypass a required layer merely for speed.

---

## 6. Naming

### Packages

Lowercase, feature-oriented:

```text
auth home contracts camera analysis importantdates
```

Avoid: `misc`, `temp`, `others`.

### Kotlin

```text
<Feature>Activity
<Feature>Fragment
<Feature>ViewModel
<Feature>UiState
<Feature>UiEvent
<Feature>UiEffect

<Feature>Adapter
<Feature>ViewPagerAdapter
<Feature>Dialog
<Feature>OverlayView

<Feature>Repository
<Feature>RepositoryImpl
<Feature>RemoteDataSource
<Feature>LocalDataSource

<Action><Feature>UseCase
<Feature>Request
<Feature>Response
<Feature>Dto
<Feature>Entity
<Feature>Mapper
```

Style:

```text
Class/Interface/Object/Enum → PascalCase
function/variable/property  → lowerCamelCase
constant                    → UPPER_SNAKE_CASE
```

Boolean prefix: `is`, `has`, `can`, `should`.

Event names describe what happened:
`LoginClicked`, `QueryChanged`, `RetryClicked`, `ContractClicked`.

Effect names describe one action:
`NavigateToHome`, `ShowMessage`, `ShowDeleteDialog`, `RequestCameraPermission`, `OpenGallery`.

Use descriptive names; avoid `data1`, `temp`, `obj`, `item2`, `test123`.

### Android resources

```text
text  → strings.xml
color → colors.xml
size  → dimens.xml
```

Prefixes:

```text
ic_*        icon
img_*       image
bg_*        background
shape_*     shape
selector_*  selector
anim_*      animation
```

Prefer feature prefix:

```text
auth_* home_* contract_* camera_* upload_* analysis_*
```

Layouts:

```text
activity_<feature>.xml
fragment_<feature>.xml
item_<feature>.xml
dialog_<feature>.xml
bottom_sheet_<feature>.xml
```

View IDs: lowerCamelCase, e.g. `tvTitle`, `ivIcon`, `btnSubmit`, `etEmail`, `rvContracts`, `vpOnboarding`, `pbLoading`.

Prefer XML + ViewBinding/DataBinding unless Compose migration is approved.

---

## 7. Adapter / Component / Dialog

```text
adapter/   → RecyclerView/ListAdapter/ViewPager2 Adapter
component/ → feature Custom View/UI component
dialog/    → feature Dialog/BottomSheet
utils/     → feature UI helper only
```

No business logic in these classes.

If reused by multiple features, move to matching `presentation/common/*`. Do not move to common for hypothetical future reuse.

---

## 8. DI

Use the project's existing DI framework.

Typical modules:

```text
NetworkModule
DatabaseModule
RepositoryModule
```

Prefer constructor injection when supported. Do not manually build dependency chains in UI, add another DI framework, or create empty modules.

---

## 9. Figma / Team / Shared Files

Approved Figma is UI source of truth for layout, spacing, typography, colors, assets, component size, state, dialog/bottom sheet, and navigation flow.

Do not redesign or "improve" Figma without approval. Material ambiguity → **STOP → REPORT**.

Current ownership/branches:

```text
docs/team_assignment.md
```

Rules:

- modify only assigned features
- do not implement/refactor another owner's feature
- cross-feature work changes only the agreed boundary
- missing destination causing build failure → smallest agreed skeleton or report dependency

High-conflict files:

```text
AndroidManifest.xml
strings.xml
colors.xml
dimens.xml
themes.xml
navigation graph
Gradle files
presentation/common/*
```

Edit minimally; do not reformat whole files, rename/delete unrelated resources, or overwrite another member's entries.

---

## 10. Clean Code

- clear single responsibility
- descriptive names
- avoid duplication and unnecessary abstraction
- avoid unnecessary mutable state and `!!`
- expose read-only Flow
- remove unused imports, dead/debug/commented-out legacy code
- no empty `catch`; handle expected failures
- rethrow `CancellationException` before generic coroutine exceptions
- do not refactor outside task scope
- never store/commit secrets, tokens, keys, credentials

---

## 11. Verification

Quick compile:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Required before owner test/commit:

```powershell
.\gradlew.bat :app:assembleDebug
```

When applicable:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

UI/instrumentation when applicable:

```powershell
adb devices
.\gradlew.bat :app:connectedDebugAndroidTest
```

UI work: build → open screen if possible → compare Figma → test main interaction/navigation + loading/error/empty + crash/scroll → report tested/not-tested items.

Never claim skipped tests passed.

Failure:

```text
READ ERROR → FIX ROOT CAUSE → RUN AGAIN
```

No commit/push while required verification fails.

---

## 12. Git — Khải

Repository:

```text
https://github.com/Khaik5/LegalLens
```

Base:

```text
khai_develop
```

Branch:

```text
1 LARGE FEATURE/FLOW = 1 BRANCH
khai/<feature-or-flow-name>
```

Do not branch per Activity/Fragment/Dialog/UI state/sub-screen. Exact current branches come from `docs/team_assignment.md`.

Before new work:

```powershell
git status
git branch --show-current
git fetch origin
git switch khai_develop
git pull --ff-only origin khai_develop
git switch -c khai/<feature-or-flow-name>
```

If feature branch exists, reuse it.

Never develop a new feature directly on:

```text
main master develop khai_develop phuoc_develop
```

Never run automatically:

```text
git reset --hard
git clean -fd
git push --force
git push -f
```

Do not change base branch automatically.

### Commits

```text
[Feat] new feature
[Fix] bug fix
[Update] improvement
[Delete] removal
[Refactor] internal refactor
[Test] tests
[Docs] documentation
[Merge] actual merge
[Chore] tooling/config
```

Before commit:

```powershell
git status
git diff
git diff --check
git branch --show-current
```

Commit only intended files. Feature branch must start with `khai/`.

First push:

```powershell
git push -u origin khai/<feature-or-flow-name>
```

Later:

```powershell
git push
```

Never direct-push/auto-merge protected/shared branches.

---

## 13. Approval / Memory / Done

Required workflow:

```text
READ RULES
→ CHECK FIGMA + ASSIGNMENT
→ CHECK/SYNC GIT
→ IMPLEMENT
→ REVIEW/CLEAN
→ BUILD/TEST
→ TEST UI
→ REPORT OWNER
→ OWNER TEST
→ WAIT "OK"
→ REVIEW DIFF
→ COMMIT
→ PUSH FEATURE BRANCH
```

Before owner says `OK`: **no completed-feature commit, push, or merge**. Automated tests do not replace owner approval.

Project memory when available:

````text
PROJECT_MEMO.md
docs/
├── team_assignment.md
└── project_notes/
    ├── requirements.md
    ├── decisions.md
    ├── bugs.md
    ├── key_facts.md
    └── issues.md

Read/update only relevant notes; never store secrets.

Definition of Done:

```text
ASSIGNED
→ FIGMA UNDERSTOOD
→ CORRECT BRANCH
→ IMPLEMENTED
→ ARCHITECTURE/NAMING CLEAN
→ BUILD PASS
→ RELEVANT TEST/LINT
→ UI TESTED
→ OWNER OK
→ DIFF REVIEWED
→ COMMITTED
→ PUSHED FEATURE BRANCH
````

Always:

```text
IMPLEMENT → VERIFY → OWNER OK → COMMIT → PUSH
```
