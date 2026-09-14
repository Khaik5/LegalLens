# LEGALLENS AGENT GUIDE

## 1. Purpose

This file defines the stable rules for architecture, code organization, UI, verification, Git, and team collaboration for the LegalLens Android project.

`AGENTS.md` is the current source of implementation rules.

If there is a conflict between `AGENTS.md`, existing code, project memory, or `docs/project_notes/decisions.md`:

```text
STOP
→ REPORT THE CONFLICT
→ DO NOT SILENTLY CHANGE THE ARCHITECTURE
```

Do not change the architecture without approval from the project owner.

---

## 2. Read First

Before starting any substantial task, the Agent must read in this order:

1. `AGENTS.md`
   - architecture, coding, Git, build, and test rules.

2. `PROJECT_MEMO.md`
   - current project context, scope, and status.

3. `docs/team_assignment.md`
   - determine who owns the current feature/flow and the corresponding branch.

4. Relevant files in `docs/project_notes/`
   - `requirements.md` → functional requirements and acceptance criteria.
   - `decisions.md` → approved architecture decisions.
   - `bugs.md` → known bugs and known fixes.
   - `key_facts.md` → stable project facts.
   - `issues.md` → related work and status.
   - `proposal_source.md` → proposal source material when comparison is actually needed.

The Agent does not need to read the entire `docs/` directory for every task.

Only read files relevant to the current task.

If documentation conflicts:

```text
STOP
→ REPORT THE CONFLICT
→ DO NOT GUESS
```

---

## 3. Overall Architecture

LegalLens uses:

```text
Clean Architecture
+
MVVM
+
UiState / Event / Effect
+
Dependency Injection
```

Preferred package structure:

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
│   │   └── dto/      # create only when truly needed
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

Do not create empty packages only to satisfy the structure.

Examples:

```text
no RecyclerView → do not create adapter/
no Custom View → do not create view/
no feature-specific helper → do not create utils/
```

The package name currently used in this documentation is `com.example.lagallens`.

Do not rename the package automatically. If the actual project package is different, report it first.

---

## 4. Core

`core/` contains technical components shared across the application.

Example:

```text
core/
├── error/
├── extension/
├── constants/
├── network/
└── utils/
```

It may contain:

- shared error handling
- extensions
- constants
- network result handling
- interceptors
- date formatting
- file helpers
- shared validation

Do not place feature-specific business logic in `core`.

---

## 5. Data

`data/` is responsible for:

```text
fetching data
storing data
sending data
transforming data
```

Possible data sources include:

- Backend API
- DataStore
- Room Database
- Local files
- Cache

The data layer must not contain UI code.

---

## 6. Remote / API

Backend/API-related code belongs in:

```text
data/datasource/remote/
```

Retrofit interfaces belong in:

```text
data/datasource/remote/api/
```

Standard flow:

```text
ViewModel
→ UseCase
→ Repository Interface
→ RepositoryImpl
→ RemoteDataSource
→ Retrofit API
→ Backend
```

Do not call Retrofit directly from:

- Activity
- Fragment
- Adapter
- ViewModel
- UseCase

---

## 7. Local

Local data belongs in:

```text
data/datasource/local/
```

### DataStore

Use for:

- auth token
- login state
- settings
- theme
- preferences

### Database / Room

Use for structured local data.

Database entities must not be passed directly to the UI.

---

## 8. Request / Response / DTO

Prefer by default:

```text
data/model/
├── request/
└── response/
```

`request/`:
- data sent to the backend

`response/`:
- data returned by the backend

`dto/`:
- create only when a real intermediate transport model is needed

Do not create multiple classes with the same meaning, such as:

```text
ContractDto
ContractResponse
ContractResponseDto
```

when they represent the same data.

Do not pass Request/Response/DTO objects directly to the UI.

---

## 9. Mapper

Mappers belong in:

```text
data/mapper/
```

Responsibility:

```text
Response / DTO / Entity
→ Mapper
→ Domain Model
```

A mapper only transforms data shape.

A mapper must not contain business logic.

Remember:

```text
Mapper = transform data shape
UseCase = handle business behavior
```

---

## 10. Domain

`domain/` is the clean business layer.

```text
domain/
├── model/
├── repository/
└── usecase/
```

The domain layer must be pure Kotlin.

It must not depend on:

```text
android.*
Retrofit
Room
DataStore
Activity
Fragment
View
```

### Domain Model

Examples:

```text
Contract.kt
User.kt
AnalysisResult.kt
ImportantDate.kt
ScannedDocument.kt
```

### Repository Interface

Located in:

```text
domain/repository/
```

A Repository Interface defines what the application needs to do with data.

Example:

```kotlin
interface ContractRepository {
    suspend fun getContracts(): Result<List<Contract>>
    suspend fun getContract(id: String): Result<Contract>
    suspend fun deleteContract(id: String): Result<Unit>
}
```

### UseCase

Located in:

```text
domain/usecase/
```

A UseCase represents a business operation.

Examples:

```text
GetContractsUseCase
UploadContractUseCase
AnalyzeContractUseCase
DeleteContractUseCase
```

For business operations or data operations, the standard flow is:

```text
ViewModel
→ UseCase
→ Repository
```

Do not let the ViewModel call the Repository directly for business/data operations.

For simple pure UI logic with no business behavior or data operation, the ViewModel may update `UiState` or emit an `Effect` directly without creating a fake UseCase.

Examples:

```text
Change Onboarding page
→ ViewModel updates UiState directly

Load contract list
→ ViewModel → GetContractsUseCase → Repository
```

Do not create a UseCase only to wrap a tiny UI operation that has no business value.

---

## 11. Repository Implementation

Implementations belong in:

```text
data/repository/
```

Examples:

```text
ContractRepositoryImpl.kt
AuthRepositoryImpl.kt
UploadRepositoryImpl.kt
AnalysisRepositoryImpl.kt
```

RepositoryImpl may coordinate:

- RemoteDataSource
- LocalDataSource
- DataStore
- Database
- Mapper
- Cache

Do not place `RepositoryImpl` in `domain`.

---

## 12. Dependency Injection

DI belongs in:

```text
di/
```

Examples:

```text
NetworkModule
DatabaseModule
RepositoryModule
UseCaseModule
```

Do not manually create dependencies throughout the codebase if the project already uses DI.

Do not add a new DI framework without approval.

---

## 13. Presentation

`presentation/` contains:

```text
UI
ViewModel
UiState
Event
Effect
Adapter
Custom View
UI helper
```

### common/

Use only for UI components that are genuinely shared by multiple features.

### feature/

Each feature owns its own presentation code.

Example:

```text
presentation/feature/camera/
├── ui/
│   └── CameraActivity.kt
├── viewmodel/
│   └── CameraViewModel.kt
├── contract/
│   ├── CameraUiState.kt
│   ├── CameraEvent.kt
│   └── CameraEffect.kt
├── adapter/
│   └── ScanPageAdapter.kt
├── view/
│   └── CameraOverlayView.kt
└── utils/
    └── CameraUiUtils.kt
```

---

## 14. UiState / Event / Effect

`UiState`, `Event`, and `Effect` must be separate files.

Do not combine them into one `FeatureContract.kt`.

### UiState

Represents UI state that can be rendered again.

### Event

Represents an action sent from the UI to the ViewModel.

### Effect

Represents one-time actions such as:

- navigation
- toast/snackbar
- dialog
- permission request
- open gallery
- open another app

Do not put one-shot actions in UiState.

---

## 15. ViewModel

ViewModel flow:

```text
receive Event
→ if business/data operation: call UseCase
→ if pure UI logic: handle state/effect directly
→ update UiState or emit Effect
```

Use private mutable flows and expose read-only flows.

Do not create fake UseCases for simple UI operations just to satisfy architecture layers.

Do not let the ViewModel:

- call Retrofit directly
- call DAO directly
- access DataStore directly
- map DTOs
- contain complex business logic

---

## 16. Activity / Fragment

Activity/Fragment should only:

```text
render UiState
send Event
collect Effect
handle Android UI behavior
```

Do not put these inside Activity/Fragment:

- business logic
- Retrofit calls
- database operations
- DataStore access
- DTO mapping
- RepositoryImpl
- complex data processing

---

## 17. Adapter / View / Utils

### adapter/

Contains:

- RecyclerView.Adapter
- ListAdapter
- ViewPager Adapter

Do not place business logic in adapters.

### view/

Contains feature-specific Custom Views.

If a Custom View is shared by multiple features, move it to:

```text
presentation/common/view/
```

### utils/

Feature UI helpers belong in:

```text
presentation/feature/<feature>/utils/
```

Shared UI helpers belong in:

```text
presentation/common/utils/
```

Application-wide technical utilities belong in:

```text
core/utils/
```

---

## 18. Data Flow

### Sending a request

```text
UI
↓ Event
ViewModel
↓
UseCase
↓
Repository Interface
↓
RepositoryImpl
↓
Remote / Local DataSource
↓
API / DataStore / Database
```

### Receiving a response

```text
API / Database
↓
Response / DTO / Entity
↓
Mapper
↓
Domain Model
↓
UseCase
↓
ViewModel
↓ UiState
UI
```

Do not bypass layers only because it is faster.

---

## 19. Clean Code

- keep class/function responsibilities clear
- use understandable names
- avoid duplicated code
- avoid unnecessary abstractions
- avoid unnecessary mutable state
- avoid `!!`
- remove unused imports
- remove dead code
- remove debug code after completion
- do not keep old implementation code commented out for long periods
- do not use empty `catch`
- handle expected failures
- coroutines must rethrow `CancellationException` before generic exceptions
- do not refactor code outside the task scope
- do not modify another member's feature just to make the code "cleaner"

---

## 20. Android Resources

Use:

```text
text  → strings.xml
color → colors.xml
size  → dimens.xml
```

Naming:

```text
ic_*        icon
img_*       image
bg_*        background
shape_*     shape
selector_*  selector
anim_*      animation
```

Prefer feature prefixes:

```text
camera_*
upload_*
analysis_*
contract_*
auth_*
home_*
```

Do not use vague names such as:

```text
image1
icon
drawable1
background
button_bg
```

Prefer XML + ViewBinding/DataBinding unless the project intentionally migrates to Compose.

---

## 21. Figma Is the UI Source of Truth

When implementing UI, follow the approved Figma for:

- layout
- spacing
- typography
- color
- icons/images
- component size
- button position
- state
- dialog
- bottom sheet
- screen flow

Do not independently:

- redesign
- "improve" the design
- change colors
- change spacing
- change the flow
- change components

without approval.

If Figma is unclear:

```text
STOP
→ REPORT
→ WAIT FOR A DECISION
```

---

## 22. Team Ownership Rules

- Only modify features assigned to you.
- Do not modify another member's feature without permission.
- If you find a problem in another member's feature:

```text
REPORT
→ DO NOT MODIFY
```

- Current assignments are stored in:

```text
docs/team_assignment.md
```

- The latest assignment approved by the project owner takes priority.

---

## 23. Cross-feature Navigation

A feature may navigate to a feature owned by another member.

Rules:

- the source-screen owner only modifies the source screen
- the destination-screen owner is responsible for the destination
- do not copy or reimplement another member's screen
- only connect navigation through the agreed integration boundary

If the destination class/route does not exist and causes the build to fail:

```text
USE THE SMALLEST AGREED SKELETON/INTEGRATION
OR
REPORT THE DEPENDENCY
```

---

## 24. Shared Files

Files commonly touched by multiple members:

```text
AndroidManifest.xml
strings.xml
colors.xml
dimens.xml
themes.xml
navigation graph
Gradle files
shared components
```

When modifying them:

- change only what is required
- do not reformat the entire file
- do not rename unrelated resources
- do not remove another member's entries
- do not rewrite shared components unless needed
- review the diff carefully
- prefer feature-prefixed resources

---

## 25. Verification

Quick Kotlin check:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

After each completed screen/feature, always run:

```powershell
.\gradlew.bat :app:assembleDebug
```

When applicable:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

UI/instrumentation:

```powershell
adb devices
.\gradlew.bat :app:connectedDebugAndroidTest
```

Do not claim PASS if the test did not actually run.

If verification fails:

```text
READ THE ERROR
→ FIND THE ROOT CAUSE
→ FIX IT
→ RUN AGAIN
```

Do not commit or push while required verification is failing.

---

## 26. Mandatory UI Verification

After completing a screen:

1. Build successfully.
2. Open the screen on an emulator/device when available.
3. Compare it against Figma.
4. Check layout, spacing, typography, colors, icons, buttons, scrolling, and navigation.
5. Check loading/error/empty states when applicable.
6. Check for crashes.
7. Test the main interactions.
8. Report exactly what was tested.
9. Report what was not tested.
10. Tell the project owner the screen is ready for manual testing.
11. Stop and wait for confirmation.

---

## 27. Git Workflow — Phuoc

Repository:

```text
https://github.com/Khaik5/LegalLens
```

Base branch:

```text
phuoc_develop
```

### Branch Creation Principle

Use this rule:

```text
1 LARGE FEATURE / FLOW
=
1 FEATURE BRANCH
```

Do not create a separate branch for every:

- Activity
- Fragment
- Dialog
- BottomSheet
- UI state
- sub-screen
- small operation

when they belong to the same feature/flow.

For example, `Add Contract` may contain multiple screens:

```text
Add Contract Flow
├── Select contract source
│   ├── PDF
│   ├── Word
│   ├── Gallery
│   └── Camera
├── Upload Progress
├── Upload Error
├── Upload List / Review
└── Upload Success
```

All screens/states above use the same branch:

```text
phuoc/add-contract
```

Do not create:

```text
phuoc/select-pdf
phuoc/select-word
phuoc/upload-progress
phuoc/upload-error
phuoc/upload-success
```

Camera/Scan is a separate flow and may use:

```text
phuoc/camera-scan
```

containing:

```text
Camera / Scan
├── Permission
├── Capture
└── Manage Scan Pages
```

AI Analysis is a separate flow:

```text
phuoc/analysis
```

One branch may contain multiple small commits as long as all commits belong to the same feature/flow.

Example inside `phuoc/add-contract`:

```text
[Feat] Add contract source selection
[Feat] Add upload progress state
[Feat] Add upload result states
[Fix] Fix duplicate upload request
```

Do not create a new branch only to separate those commits.

### When Should a New Branch Be Created?

Create a new branch only when the work:

- is an independent feature/flow
- has a clearly separate business scope
- can be developed/tested relatively independently
- or the project owner explicitly requests separation

Branch naming:

```text
phuoc/<feature-or-flow-name>
```

The current branch/flow list must not be hardcoded in `AGENTS.md`.

See current assignments and branches in:

```text
docs/team_assignment.md
```

`AGENTS.md` only defines the general branch naming rules.

### Before Starting

Always check:

```powershell
git status
git branch --show-current
git fetch origin
```

When the working tree is safe:

```powershell
git switch phuoc_develop
git pull --ff-only origin phuoc_develop
git switch -c phuoc/<feature-or-flow-name>
```

If the correct feature branch already exists, continue using it instead of creating a new branch for each screen.

Do not develop a new feature directly on:

```text
main
master
develop
phuoc_develop
khai_develop
```

If `phuoc_develop` does not exist:

```text
STOP
→ REPORT
```

Do not silently use another base branch.

Do not automatically run:

```text
git reset --hard
git clean -fd
git push --force
git push -f
```

---

## 28. Owner Approval Gate

Mandatory workflow:

```text
UNDERSTAND THE TASK
→ CHECK FIGMA
→ CHECK THE ASSIGNMENT
→ CHECK GIT
→ SYNC phuoc_develop
→ CREATE/USE phuoc/<feature-or-flow-name>
→ IMPLEMENT
→ REVIEW UI + LOGIC
→ CLEAN CODE
→ BUILD
→ TEST/LINT
→ TEST UI
→ REPORT TO PROJECT OWNER
→ PROJECT OWNER TESTS
→ WAIT FOR "OK"
→ REVIEW DIFF
→ COMMIT
→ PUSH FEATURE BRANCH
```

Before the project owner says `OK`:

- do not commit the completed feature
- do not push
- do not merge
- do not push `main`
- do not push `develop`
- do not push `phuoc_develop`
- do not push `khai_develop`

If the project owner reports a problem:

```text
FIX
→ BUILD
→ TEST
→ REPORT AGAIN
→ WAIT FOR OK
```

Automated test PASS does not replace owner approval.

---

## 29. Commit Convention

```text
[Feat]     new feature
[Fix]      bug fix
[Update]   update/improvement
[Delete]   remove code/feature
[Refactor] refactor
[Test]     tests
[Docs]     documentation
[Merge]    actual branch merge
[Chore]    tooling/config/maintenance
```

Before commit:

```powershell
git status
git diff
git diff --check
git branch --show-current
```

Commit only files related to the task.

The branch must start with:

```text
phuoc/
```

Do not commit passwords, tokens, API keys, signing keys, credentials, or secrets.

---

## 30. Push Rules

Push only after:

```text
BUILD/TEST PASS
→ OWNER TESTS
→ OWNER SAYS "OK"
→ REVIEW DIFF
→ COMMIT
```

First push:

```powershell
git push -u origin phuoc/<feature-or-flow-name>
```

Already tracked:

```powershell
git push
```

Do not push directly to:

```text
main
master
develop
phuoc_develop
khai_develop
```

Do not automatically merge into a shared/protected branch.

---

## 31. Project Memory

If available:

```text
PROJECT_MEMO.md
docs/project_notes/
├── bugs.md
├── decisions.md
├── key_facts.md
└── issues.md
```

Reading documentation before a task must follow the `Read First` section.

After meaningful work, update the appropriate related file when useful.

Do not store secrets or sensitive data in project memory.

---

## 32. Definition of Done

A screen is ready for owner testing only when:

```text
ASSIGNED
→ FIGMA UNDERSTOOD
→ CORRECT FEATURE BRANCH
→ IMPLEMENTATION COMPLETE
→ UI REVIEWED
→ CODE CLEANED
→ BUILD PASSED
→ APPLICABLE TEST/LINT RUN
→ UI TESTED
→ RESULT REPORTED
```

A feature is Git-complete only when:

```text
OWNER TESTS
→ OWNER SAYS "OK"
→ DIFF REVIEWED
→ COMMITTED
→ FEATURE BRANCH PUSHED
```

Always:

```text
FIGMA
→ ASSIGNMENT
→ FEATURE BRANCH
→ IMPLEMENT
→ VERIFY
→ OWNER TEST
→ OWNER OK
→ COMMIT
→ PUSH
```
