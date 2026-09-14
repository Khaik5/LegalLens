# LEGALLENS UI TEAM ASSIGNMENT

## 1. Purpose

This file stores the current screen/feature assignments for the Mobile team.

This file may change frequently when:

- new Figma screens are added
- feature scope changes
- ownership changes
- workload is redistributed
- a feature is completed or handed over

`AGENTS.md` contains the general rules.

`docs/team_assignment.md` contains only the current assignments and actual work status.

The latest assignment approved by the project owner always takes priority.

---

## 2. Assignment Rules

- Each feature/screen has one primary owner at a time.
- Do not modify another member's feature without permission.
- If two features require integration, only modify the agreed integration boundary.
- Shared files must follow `AGENTS.md`.
- If ownership changes, update this file before coding begins.

---

## 3. Current Assignment

### KHAI

```text
KHAI
├── Email Verification / OTP
├── Home / Dashboard
├── Important Dates
└── Contract Management
    ├── Contract List
    ├── Search
    ├── Filter
    ├── Contract Detail
    ├── Version History
    └── Delete Contract Dialog
```

### PHUOC

```text
PHUOC
├── Onboarding Flow
│   ├── Splash
│   ├── Onboarding Fragment 1
│   └── Onboarding Fragment 2
├── Add Contract Flow
│   ├── Add Contract
│   ├── Select PDF / Word / Gallery
│   ├── Upload Progress
│   ├── Upload Error
│   ├── Upload List / Review
│   └── Upload Success
├── Camera / Scan Flow
│   ├── Camera Permission
│   ├── Camera Capture
│   └── Manage Scan Pages
└── AI Analysis Flow
    └── Analysis Complete
```

This is the current temporary assignment based on the Figma screens reviewed so far.

---

## 4. Feature/Flow Groups and Branches

General rule:

```text
1 LARGE FEATURE / FLOW
=
1 BRANCH
```

Do not create a separate branch for every screen, dialog, or UI state within the same flow.

One branch may contain multiple screens and multiple small commits.

### Khai

```text
khai/auth
└── Email Verification / OTP

khai/home
├── Home / Dashboard
└── Important Dates

khai/contracts
├── Contract List
├── Search
├── Filter
├── Contract Detail
├── Version History
└── Delete Contract Dialog
```

### Phuoc

```text
phuoc/onboarding
├── Splash
├── Onboarding Fragment 1
└── Onboarding Fragment 2

phuoc/add-contract
├── Add Contract
├── Select PDF / Word / Gallery
├── Upload Progress
├── Upload Error
├── Upload List / Review
└── Upload Success

phuoc/camera-scan
├── Camera Permission
├── Camera Capture
└── Manage Scan Pages

phuoc/analysis
└── AI Analysis
    └── Analysis Complete
```

For example, Upload screens are part of the Add Contract flow and should use the same branch:

```text
phuoc/add-contract
```

Do not create:

```text
phuoc/upload-progress
phuoc/upload-error
phuoc/upload-success
```

Create a new branch only when the work is an independent feature/flow or the project owner explicitly requests a separate branch.

Actual branch usage must follow the Git workflow in `AGENTS.md`.

---

## 5. Integration Points Between Khai and Phuoc

### Onboarding → Email Verification / OTP

```text
Phuoc Onboarding
      ↓
Khai Email Verification / OTP
```

Rules:

- Phuoc owns Splash and the entire Onboarding flow.
- Khai owns Email Verification / OTP.
- Phuoc only navigates to Auth/OTP through the agreed integration boundary.
- Khai must not modify Onboarding unless ownership is explicitly assigned.
- Phuoc must not reimplement Khai's Auth/OTP.
- If the destination screen does not exist and causes the build to fail, use the smallest agreed skeleton/integration or report the dependency.

### Home → Add Contract

```text
Khai Home
   ↓ + button
Phuoc Add Contract
```

Rules:

- Khai owns Home and the `+` button.
- Phuoc owns Add Contract.
- Khai must not implement Add Contract.
- Phuoc must not modify Home only to connect the flow unless explicitly assigned.
- Only connect navigation through the agreed boundary.

### Contract Detail → AI Analysis

```text
Khai Contract Detail
   ↓
Phuoc AI Analysis
```

Rules:

- Khai owns Contract Detail.
- Phuoc owns AI Analysis.
- Do not reimplement each other's screens.
- If the destination does not exist and causes the build to fail, use the smallest agreed skeleton/integration or report the dependency.

---

## 6. Shared UI / Shared Resources

Areas likely to be touched by both members:

```text
Bottom Navigation
AndroidManifest.xml
strings.xml
colors.xml
dimens.xml
themes.xml
navigation graph
shared components
Gradle files
```

See `AGENTS.md` for the detailed rules.

---

## 7. Work Status

Use these statuses:

```text
TODO
IN_PROGRESS
READY_FOR_OWNER_TEST
OWNER_APPROVED
COMMITTED
PUSHED
BLOCKED
```

Current table:

| Member | Feature | Status | Notes |
|---|---|---|---|
| Khai | Email Verification / OTP | TODO | Branch `khai/auth` |
| Khai | Home / Dashboard | TODO | Branch `khai/home` |
| Khai | Important Dates | TODO | Same Home flow if it continues to stay in `khai/home` |
| Khai | Contract Management | TODO | Includes List/Search/Filter/Detail/History/Delete; branch `khai/contracts` |
| Phuoc | Onboarding Flow | READY_FOR_OWNER_TEST | Splash + 3 Figma onboarding pages implemented; branch `phuoc/onboarding` |
| Phuoc | Add Contract Flow | TODO | Includes Add Contract + source selection + Upload Progress/Error/List/Success; branch `phuoc/add-contract` |
| Phuoc | Camera / Scan Flow | TODO | Permission/Capture/Manage Pages; branch `phuoc/camera-scan` |
| Phuoc | AI Analysis Flow | TODO | Currently only Analysis Complete is confirmed; branch `phuoc/analysis` |

Update the status whenever the real project state changes.

---

## 8. When New Figma Screens Are Added

When a new screen is added:

```text
REVIEW THE FLOW
→ GROUP THE FEATURE
→ IDENTIFY DEPENDENCIES
→ CHOOSE THE OWNER
→ UPDATE THIS FILE
→ THEN START CODING
```

Do not divide work only by counting the number of screens.

Prefer grouping based on:

- same flow
- same feature
- fewer cross-dependencies
- fewer shared-file changes
- reasonable workload balance

---

## 9. Assignment Changes

When the project owner changes ownership:

1. update the feature/screen in this file
2. clearly record the new owner
3. record the current status
4. record dependencies when relevant
5. do not take over another member's code before an actual handoff

Example:

```text
Camera / Scan
Owner: Phuoc
Status: IN_PROGRESS
Branch: phuoc/camera-scan
Dependency: Add Contract
```

---

## 10. Final Rule

```text
AGENTS.md
= stable rules

team_assignment.md
= who does what + current status
```

If the two files conflict about:

```text
architecture
Git
build/test
Figma rules
shared file rules
```

then `AGENTS.md` takes priority.

If they conflict about:

```text
who owns which feature
current task status
```

then the latest assignment approved by the project owner takes priority.
