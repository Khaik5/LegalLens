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

`docs/team_assignment.md` contains only the actual assignments and current status.

The latest assignment confirmed by the project owner always takes priority.

---

## 2. Assignment Rules

- Each feature/screen has only one primary owner at a time.
- Do not modify another person's feature on your own.
- If integration between two features is required, modify only the agreed integration boundary.
- Shared files must follow `AGENTS.md`.
- If assignments change, update this file before starting implementation.

---

## 3. Current Assignments

### KHẢI

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

### PHƯỚC

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

These are temporary assignments based on the Figma screens reviewed so far.

---

## 4. Feature/Flow Grouping and Branches

General rule:

```text
1 LARGE FEATURE / FLOW
=
1 BRANCH
```

Do not create a separate branch for each screen, dialog, or UI state inside the same flow.

One branch may contain multiple screens and multiple small commits.

### Khải

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

### Phước

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

For example, Upload screens belonging to the Add Contract flow should share:

```text
phuoc/add-contract
```

Do not create:

```text
phuoc/upload-progress
phuoc/upload-error
phuoc/upload-success
```

Create a new branch only when it is an independent feature/flow or when the project owner explicitly requests a split.

Actual branches must follow the Git workflow in `AGENTS.md`.

---

## 5. Integration Points Between Khải and Phước

### Onboarding → Email Verification / OTP

```text
Phước Onboarding
      ↓
Khải Email Verification / OTP
```

Rules:

- Phước owns Splash and the entire Onboarding flow.
- Khải owns Email Verification / OTP.
- Phước only navigates to Auth/OTP through the agreed integration boundary.
- Khải must not modify Onboarding unless assigned.
- Phước must not reimplement Khải's Auth/OTP.
- If the destination screen does not exist yet and causes the build to fail, use the smallest agreed skeleton/integration or report the dependency.

### Home → Add Contract

```text
Khải Home
   ↓ + button
Phước Add Contract
```

Rules:

- Khải owns Home and the `+` button.
- Phước owns Add Contract.
- Khải must not implement Add Contract.
- Phước must not modify Home merely to connect the flow unless assigned.
- Connect navigation only through the agreed boundary.

### Contract Detail → AI Analysis

```text
Khải Contract Detail
   ↓
Phước AI Analysis
```

Rules:

- Khải owns Contract Detail.
- Phước owns AI Analysis.
- Do not reimplement each other's screens.
- If the destination does not exist yet and causes the build to fail, use the smallest agreed skeleton/integration or report the dependency.

---

## 6. Shared UI / Shared Resources

Areas likely to be modified by multiple people:

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

See `AGENTS.md` for detailed rules.

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

| Owner | Feature                  | Status      | Notes                                                                                                      |
| ----- | ------------------------ | ----------- | ---------------------------------------------------------------------------------------------------------- |
| Khải  | Email Verification / OTP | READY_FOR_OWNER_TEST | Branch `khai/auth`; Login/Register/Forgot Password → OTP → Success flow implemented; build/test/lint pass |
| Khải  | Home / Dashboard         | TODO        | Branch `khai/home`                                                                                         |
| Khải  | Important Dates          | TODO        | Same Home flow if it remains grouped in `khai/home`                                                        |
| Khải  | Contract Management      | TODO        | Includes List/Search/Filter/Detail/History/Delete; branch `khai/contracts`                                 |
| Phước | Onboarding Flow          | IN_PROGRESS | Splash + 2 Onboarding Fragments already exist; branch `phuoc/onboarding`                                   |
| Phước | Add Contract Flow        | TODO        | Includes Add Contract + source selection + Upload Progress/Error/List/Success; branch `phuoc/add-contract` |
| Phước | Camera / Scan Flow       | TODO        | Permission/Capture/Manage Pages; branch `phuoc/camera-scan`                                                |
| Phước | AI Analysis Flow         | TODO        | Currently only Analysis Complete has been finalized; branch `phuoc/analysis`                               |

Update the status whenever the actual work status changes.

---

## 8. When New Figma Screens Are Added

When a new screen is added:

```text
REVIEW THE FLOW
→ GROUP THE FEATURE
→ IDENTIFY DEPENDENCIES
→ SELECT THE OWNER
→ UPDATE THIS FILE
→ THEN IMPLEMENT
```

Do not divide work simply by the number of screens.

Prefer grouping by:

- same flow
- same feature
- fewer cross-feature dependencies
- fewer shared-file changes
- reasonable workload balance

---

## 9. Assignment Changes

When the project owner changes the person responsible:

1. update the feature/screen in this file
2. clearly record the new owner
3. clearly record the current status
4. record dependencies if any
5. do not take over another person's code unless it has been handed over

Example:

```text
Camera / Scan
Owner: Phước
Status: IN_PROGRESS
Branch: phuoc/camera-scan
Dependency: Add Contract
```

---

## 10. Final Rules

```text
AGENTS.md
= stable rules

team_assignment.md
= who owns what + current status
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

then the latest assignment confirmed by the project owner takes priority.
