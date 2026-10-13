# Universal App Reverse Engineering, Complete Recreation & Production-Quality Implementation

## 1. Mission and Objective

Your mission is to perform a **comprehensive, end-to-end reverse-engineering analysis of the reference application and build a complete, production-quality equivalent within our existing application**.

The reference application may be a mobile app, desktop app, web application, or another accessible software product. Use whatever reference environment is available, including an installed app, emulator, physical device, browser, screenshots, recordings, documented workflows, and publicly accessible documentation.

Do not assume the reference app is limited to the screens or features initially mentioned. Discover its functionality systematically.

The objective is to reproduce the reference application's observable functionality, user experience, information architecture, interaction patterns, visual design, state transitions, and relevant edge-case behavior as faithfully as reasonably possible.

**This is not a request for a prototype, a visual mockup, a partial implementation, or a list of recommendations. It is an end-to-end engineering task: investigate, document, implement, test, fix, and verify.**

Do not stop after inspecting the home screen, implementing the primary workflow, or fixing the first set of obvious bugs.

Continue until all discoverable features within the agreed scope have been inspected, every identified gap has been addressed or explicitly documented, and the implemented functionality has been validated.

Use the reference application as a behavioral and visual reference. Reimplement its functionality using our own implementation and appropriate assets, APIs, and architecture. Do not copy proprietary source code, extract protected credentials, or reproduce assets in ways that violate applicable rights or licensing.

---

## 2. Phase One: Understand the Existing Project

Before changing any code, investigate the complete existing project.

### 2.1 Repository and architecture analysis

Identify and document:

- Programming languages, frameworks, SDKs, build tools, and platform targets.
- Application entry points, navigation architecture, routing, and screen composition.
- Modules, packages, feature boundaries, and reusable components.
- Existing design system, typography, colors, spacing, themes, and assets.
- Data models, state-management patterns, persistence, caching, and synchronization.
- Backend APIs, network clients, authentication, authorization, and error handling.
- Third-party SDKs, libraries, permissions, platform integrations, and native components.
- Existing tests, build variants, feature flags, analytics, and release configurations.
- Incomplete implementations, TODOs, known bugs, duplicated logic, and existing limitations.

Trace the important user flows through the code instead of relying exclusively on filenames or class names.

Understand which parts already work, which are incomplete, and which could regress if modified.

### 2.2 Establish a baseline

Before implementation:

1. Build the existing application using the available development environment.
2. Run it on the target device, emulator, browser, or desktop environment.
3. Record existing build errors, runtime failures, crashes, and warnings relevant to the work.
4. Exercise the main existing workflows.
5. Capture representative screenshots and recordings.
6. Run the existing automated test suite.
7. Record baseline behavior and any pre-existing failures.

Do not attribute pre-existing problems to your changes.

Do not overwrite unrelated modifications or discard uncommitted user work.

### 2.3 Identify the implementation boundaries

Determine which capabilities must be implemented locally and which depend on backend services, platform APIs, hardware, permissions, or external integrations.

Do not invent backend capabilities, API responses, credentials, or production infrastructure that does not exist.

When a dependency is unavailable, implement the appropriate abstraction or graceful fallback where possible, and document the limitation precisely.

---

## 3. Phase Two: Systematically Explore the Reference Application

Inspect the reference application directly whenever access is available.

Do not begin by guessing its feature set. Discover it through structured exploration.

### 3.1 Start from every entry point

Inspect all applicable entry points, including:

- First launch and onboarding.
- Login, registration, authentication, and account recovery.
- Home screen and primary navigation.
- Tabs, drawers, sidebars, bottom navigation, and navigation rails.
- Search, global actions, contextual actions, and shortcuts.
- Notifications, deep links, shared content, and external entry points.
- Permission requests, first-run prompts, and system integrations.
- Returning-user flows and restored application state.

Determine how the experience changes for a first-time user, an authenticated user, an unauthenticated user, and a returning user, wherever applicable.

### 3.2 Build a complete screen inventory

Discover and document every reachable:

- Screen and sub-screen.
- Nested route and navigation destination.
- Dialog and confirmation prompt.
- Modal, bottom sheet, popup, tooltip, and contextual menu.
- Settings page and settings subsection.
- Form, validation state, and submission result.
- Detail view, list view, search result, and filtered view.
- Empty state, loading state, error state, success state, and offline state.
- Permission-denied state and recovery flow.
- Onboarding step and optional walkthrough.
- Fullscreen, landscape, responsive, and alternate-layout state.
- Premium, restricted, disabled, or feature-flag-dependent state, when applicable.

Explore every visible control and every relevant navigation path.

A screen is not fully inspected merely because its initial layout has been observed.

### 3.3 Use a page-by-page exploration protocol

For every screen:

1. Open the screen through the actual application.
2. Capture its initial appearance.
3. Identify every visible interactive element.
4. Tap each button, icon, tab, link, card, menu, and contextual action.
5. Test long presses, double taps, swipes, scrolling, dragging, and keyboard input where applicable.
6. Open every menu, dialog, submenu, and secondary destination.
7. Change each relevant setting and observe its immediate effect.
8. Navigate away and return to determine whether state persists.
9. Test back navigation, dismissal, cancellation, and reopening.
10. Test relevant loading, empty, error, permission, and unavailable states.
11. Record the resulting screen, state changes, and navigation destination.
12. Add any newly discovered destinations to the inventory.
13. Repeat the process for each newly discovered screen.

Continue this process until the exploration queue is exhausted or an explicitly documented access limitation prevents further inspection.

Do not mark inaccessible functionality as inspected.

### 3.4 Inspect behavior, not just appearance

For each interaction, determine:

- What triggers the interaction.
- What conditions enable or disable it.
- What immediate visual feedback appears.
- Whether the action changes application state.
- Whether it causes navigation, a network request, or a platform action.
- Whether the action can be cancelled or reversed.
- Whether the action persists after navigation or restart.
- How loading and failures are handled.
- Whether repeated actions cause duplicate requests or inconsistent states.
- How the behavior changes with permissions, connectivity, account state, and configuration.

If behavior cannot be observed directly, distinguish verified facts from reasonable hypotheses. Do not present assumptions as confirmed behavior.

---

## 4. Phase Three: Reverse-Engineer the Complete User Experience

Analyze the reference across all relevant dimensions.

### 4.1 Visual design and layout

Record:

- Screen dimensions, safe areas, system bars, and content insets.
- Layout hierarchy, alignment, dimensions, margins, and padding.
- Typography, font weights, line heights, and text truncation.
- Colors, gradients, borders, dividers, shadows, and elevation.
- Corner radii, opacity, transparency, and blur effects.
- Icons, illustrations, images, and image-cropping behavior.
- Buttons, text fields, chips, cards, lists, and reusable components.
- Selection indicators, focus states, hover states, and disabled states.
- Animations, transitions, motion curves, and timing.
- Light mode, dark mode, accent colors, and customization options.
- Responsive layouts, orientation changes, and window resizing.
- Accessibility scaling and long-content behavior.

Inspect representative screens at multiple sizes and configurations rather than assuming a single screenshot captures the complete design.

Use screenshots and side-by-side comparisons to identify visual differences. When possible, use image overlays or image-difference analysis to locate misaligned elements.

Do not rely exclusively on pixel comparison: dynamic content, font rendering, operating-system differences, and animations may create harmless differences.

### 4.2 Interaction design

Inspect:

- Tap, double-tap, long-press, drag, swipe, pinch, and keyboard interactions.
- Gesture boundaries, thresholds, sensitivity, and conflict resolution.
- Touch interception and event propagation.
- Focus management and keyboard visibility.
- Scrolling, pagination, pull-to-refresh, and infinite loading.
- Auto-hide controls, transient overlays, and dismissal behavior.
- Drag-and-drop and resizable elements, where applicable.
- Undo, redo, confirmation, and destructive-action safeguards.
- Accessibility actions and alternate input methods.
- Interaction behavior during animations, transitions, and network requests.

Document how simultaneous or competing gestures are resolved.

### 4.3 Navigation and state management

Map all meaningful transitions between screens.

Determine:

- Initial routes and default destinations.
- Back-stack behavior and deep-link handling.
- Nested navigation and modal navigation.
- State restoration after rotation, backgrounding, and process recreation.
- Persistence across application restarts.
- Tab-specific navigation history.
- Unsaved changes and confirmation prompts.
- Authentication-dependent navigation.
- Navigation failures and invalid destinations.
- Behavior after account changes or data updates.

Identify duplicate destinations, navigation loops, inaccessible screens, and inconsistent state restoration.

### 4.4 Business logic and data behavior

For every major feature, determine:

- Inputs and validation rules.
- Preconditions and business rules.
- Expected outputs and side effects.
- Data sources and transformation rules.
- Sorting, filtering, grouping, and pagination.
- Caching and refresh behavior.
- Persistence and synchronization.
- Network requests and response-dependent states.
- Error handling, retries, and cancellation.
- Access restrictions and feature availability.
- Calculations, formatting, dates, and localization.
- Idempotency and duplicate-action handling.

For features requiring backend behavior, inspect existing API contracts and integration points. Use real services when available and authorized. If they are unavailable, do not silently substitute fake production behavior.

### 4.5 Platform and system integrations

Where applicable, inspect:

- Permissions and permission-denied recovery.
- Camera, microphone, photos, files, location, and sensors.
- Notifications, background execution, and scheduling.
- Clipboard, sharing, external intents, and deep links.
- Hardware buttons, keyboard shortcuts, and accessibility services.
- Audio focus, media sessions, external devices, and interruptions.
- Battery restrictions, network changes, and lifecycle transitions.
- Browser integration, file downloads, and operating-system dialogs.

Implement platform-specific behavior through supported APIs and preserve the conventions of each target platform.

### 4.6 Specialized feature domains

Do not assume that the application belongs to any one category. Discover its domain first.

Then create detailed checklists for every relevant specialized feature.

Examples include:

- Media applications: playback, seeking, subtitles, audio tracks, gestures, playlists, casting, and background playback.
- Productivity applications: documents, editing, collaboration, autosave, version history, and exports.
- E-commerce applications: catalogs, filters, carts, checkout, payments, and order tracking.
- Communication applications: conversations, attachments, delivery status, notifications, and synchronization.
- Finance applications: balances, transactions, calculations, permissions, and secure workflows.
- AI applications: input modes, streaming, context handling, cancellation, history, feedback, and failure recovery.
- Utility applications: configuration, device access, scheduling, and background tasks.

These are examples, not mandatory features. Only include features relevant to the reference application, and ensure the actual discovered feature set—not this example list—drives implementation.

---

## 5. Phase Four: Maintain a Traceable Feature Checklist

Create a persistent audit document in the repository, for example:

- `docs/reference-app-audit.md`
- `docs/feature-parity-checklist.md`

For each discovered feature, record:

| Field | Required information |
|---|---|
| Feature ID | Unique identifier |
| Feature / screen | Name of the screen or capability |
| Entry point | How to reach it |
| Reference behavior | Observed behavior and relevant conditions |
| Visual requirements | Layout, components, styles, and states |
| Functional requirements | Inputs, actions, outputs, and side effects |
| Dependencies | APIs, permissions, platform capabilities |
| Existing implementation | Reusable components and current gaps |
| Required changes | Concrete implementation work |
| Test cases | Positive, negative, and edge-case scenarios |
| Status | Not inspected, inspected, implemented, or verified |
| Evidence | Screenshots, recordings, logs, or test results |
| Blockers | Unavailable access, dependencies, or unresolved questions |

Assign every discovered feature a unique ID so that it can be traced from observation through implementation and verification.

Use separate statuses for:

- Not inspected.
- Inspected and documented.
- Gap identified.
- Implementation in progress.
- Implemented, not verified.
- Verified against reference.
- Blocked, with reason.
- Intentionally out of scope, with justification.

**Never treat implementation as proof of correctness.** A feature is verified only after its relevant tests have passed.

Do not mark the entire application complete while required features remain uninspected, unverified, or silently excluded.

---

## 6. Phase Five: Convert Observations into an Implementation Plan

Before making broad architectural changes, translate the audit findings into concrete implementation tasks.

For each task, define:

1. The reference behavior.
2. The current implementation and identified gap.
3. The expected outcome.
4. The affected modules and dependencies.
5. The proposed implementation approach.
6. Regression risks.
7. Test cases and verification criteria.

Prioritize foundational issues first, including navigation architecture, design tokens, state management, shared components, and critical infrastructure.

Then implement the features in dependency order.

Avoid making broad, unrelated changes merely to simplify the code.

Preserve existing working functionality unless the reference behavior or a documented requirement calls for a change.

Do not replace functioning features with placeholders, static mockups, hardcoded demonstration data, or simulated success states.

Where a backend or external integration is unavailable, make the limitation explicit and build the best valid implementation supported by the available contracts.

---

## 7. Phase Six: Implement Complete Feature and Design Parity

### 7.1 Visual implementation

Build reusable components and design tokens where they improve consistency.

Reproduce the observed layout, typography, spacing, colors, states, animations, and responsive behavior.

Check every component in its meaningful states, including:

- Default.
- Selected.
- Pressed.
- Focused.
- Disabled.
- Loading.
- Success.
- Error.
- Empty.
- Expanded and collapsed.
- Light and dark themes, where supported.

Do not hardcode screenshot-specific coordinates when a responsive layout or existing design system provides a more robust solution.

Use appropriate, licensed assets or original equivalents.

### 7.2 Functional implementation

Implement every applicable interaction and business rule discovered during the audit.

Ensure:

- UI state reflects the actual underlying data and operation status.
- Actions trigger the correct behavior.
- Async operations expose loading, success, failure, and cancellation states.
- Repeated actions are handled safely.
- Navigation and back behavior are consistent.
- Persistence works according to the reference behavior.
- Permissions and system integrations fail gracefully.
- Network and lifecycle changes do not leave the UI in an invalid state.
- Accessibility and platform conventions are respected.
- All relevant existing functionality continues to work.

### 7.3 Engineering quality

Follow the project's established architecture and conventions unless a documented issue justifies a change.

Apply appropriate separation of concerns, reusable abstractions, lifecycle management, error handling, concurrency control, and resource cleanup.

Avoid:

- Duplicated business logic.
- Unnecessary architectural rewrites.
- Fragile touch interception.
- Unmanaged background work.
- Race conditions and stale state.
- Memory leaks and resource leaks.
- Hardcoded credentials or sensitive information.
- Silent exception handling.
- Unnecessary dependencies.
- Dead code and incomplete TODOs introduced by this work.

Do not add abstractions simply for their own sake. The implementation should remain understandable, maintainable, and consistent with the existing project.

### 7.4 Data, security, and privacy

Respect the application's existing authentication, authorization, privacy, and data-retention requirements.

Validate user input and external data.

Handle sensitive information securely.

Do not bypass security restrictions or access protected data merely to reproduce a feature.

Where the reference's behavior would conflict with security requirements, preserve security and document the difference.

---

## 8. Phase Seven: Test Every Feature and Interaction

Testing must be systematic, repeatable, and evidence-based.

### 8.1 Screen-level testing

For every screen, test:

- Initial rendering.
- Every visible interactive element.
- Every menu, popup, dialog, and secondary route.
- Scrolling and long content.
- Loading, empty, success, and error states.
- Input validation and submission.
- Back navigation and dismissal.
- State persistence and restoration.
- Relevant theme, orientation, and screen-size variations.

### 8.2 Interaction-level testing

For every interaction, verify:

- Correct trigger recognition.
- Correct action execution.
- Correct state transition.
- Correct visual feedback.
- Correct navigation result.
- Correct cancellation and recovery behavior.
- No unexpected interception by another component.
- No duplicate actions or stale callbacks.
- No regressions after repeated interactions.

### 8.3 Edge-case testing

Test relevant combinations of:

- Slow, unavailable, and changing network connections.
- Empty, malformed, large, or unexpected data.
- Permission granted, denied, and permanently denied.
- Repeated taps and concurrent requests.
- Rotation, resizing, backgrounding, and foregrounding.
- Process recreation and application restart.
- Session expiration and authentication changes.
- Rapid navigation and interrupted transitions.
- Accessibility settings and larger text.
- Unsupported platform capabilities and unavailable integrations.

Adapt these scenarios to the actual feature set instead of blindly running irrelevant tests.

### 8.4 Automated testing

Add or update tests at the appropriate levels:

- Unit tests for business logic and transformations.
- Component tests for UI state and interactions.
- Integration tests for APIs and persistence.
- Navigation tests for important flows.
- End-to-end tests for complete user journeys.
- Regression tests for every significant defect discovered.

Use deterministic test data and reliable assertions.

Do not weaken, remove, or bypass existing tests simply to obtain a passing build.

### 8.5 Real-device or equivalent-environment validation

When a device, emulator, browser, or suitable test environment is available:

1. Install and launch the application.
2. Navigate through the implemented feature.
3. Perform actual interactions.
4. Observe the resulting UI and state.
5. Capture screenshots or recordings where useful.
6. Compare against the reference under equivalent conditions.
7. Inspect logs and runtime failures.
8. Repeat the scenario after fixing a defect.

Code inspection and static screenshots alone are insufficient to validate interactive behavior.

If physical-device access is unavailable, use the best available equivalent environment and clearly state what remains unverified.

---

## 9. Phase Eight: Perform a Dedicated Gap Analysis

After implementation, repeat the reference exploration and compare it with the updated application.

Do not rely on memory or on the assumption that previously inspected features are complete.

Compare the two applications feature by feature.

For every difference, classify it as one of the following:

- Visual mismatch.
- Missing feature.
- Incorrect functionality.
- Incorrect business logic.
- Navigation mismatch.
- State or persistence mismatch.
- Gesture or interaction defect.
- Accessibility issue.
- Performance issue.
- Platform-specific limitation.
- Backend or integration gap.
- Security or privacy difference.
- Unverified behavior.

For every identified issue:

1. Record the evidence.
2. Reproduce it.
3. Identify the root cause.
4. Implement the fix.
5. Add or update a regression test.
6. Retest the affected feature.
7. Retest related workflows that might have been affected.
8. Update the checklist.

Continue auditing after fixing individual issues. A fix may expose another issue or cause a regression elsewhere.

Repeat the gap-analysis cycle until no further in-scope gaps are discovered during the defined audit process and all required verification criteria are satisfied.

Do not claim that every possible issue has been eliminated solely because a checklist is complete. Report the actual coverage, test evidence, and remaining uncertainty.

---

## 10. Phase Nine: Performance, Reliability, and Accessibility

Validate the complete implementation beyond functional correctness.

### Performance

Check applicable metrics such as:

- Startup and screen-transition latency.
- Rendering smoothness and dropped frames.
- Memory and CPU consumption.
- Network usage and redundant requests.
- Battery consumption and background activity.
- Scrolling performance and large-data handling.
- Image and media loading.
- Cancellation and cleanup of background operations.

Optimize measured bottlenecks without introducing unnecessary complexity.

### Reliability

Check:

- Crash-free execution during audited workflows.
- Correct resource and lifecycle management.
- Recovery after interruptions.
- Consistent state after repeated operations.
- Safe handling of partial failures.
- Compatibility with supported OS versions and device configurations.

### Accessibility

Verify relevant:

- Screen-reader labels and navigation order.
- Touch-target sizes.
- Text scaling and contrast.
- Keyboard and alternative-input support.
- Focus visibility and focus restoration.
- Reduced-motion preferences.
- Accessible descriptions for meaningful visual states.

Preserve the reference experience where appropriate without sacrificing essential accessibility or platform requirements.

---

## 11. Phase Ten: Regression Testing and Delivery

Before declaring the task complete:

1. Run the full available build and test pipeline.
2. Run unit, integration, UI, and end-to-end tests as supported.
3. Recheck critical existing user journeys.
4. Verify all significant fixes against their original defects.
5. Check relevant supported platform and configuration variants.
6. Review changes for accidental scope expansion, insecure handling, and unfinished code.
7. Inspect the final diff for unrelated modifications.
8. Document any known limitations, failures, and unverified behavior.
9. Ensure the audit checklist reflects the actual final state.
10. Provide a concise implementation and verification report.

Do not claim that tests passed unless they were actually executed and their results support that statement.

Do not claim device validation if the app was not run in a device or equivalent environment.

---

## 12. Definition of Done

The project is complete only when all applicable conditions below have been satisfied.

### Reference analysis
- [ ] All accessible entry points have been inspected.
- [ ] All discovered screens and reachable sub-screens have been documented.
- [ ] All relevant menus, dialogs, popups, settings, and secondary states have been inspected.
- [ ] All relevant interactions and navigation flows have been documented.
- [ ] Domain-specific functionality has been inventoried.
- [ ] Inaccessible or unobservable functionality has been explicitly identified.

### Implementation
- [ ] All required, in-scope features have been implemented.
- [ ] Required business logic and state transitions behave correctly.
- [ ] Relevant visual differences have been addressed.
- [ ] Navigation and persistence match the documented requirements.
- [ ] Loading, empty, error, and recovery states are handled.
- [ ] Platform integrations and permissions behave correctly.
- [ ] Existing functionality has been preserved or intentionally changed with justification.

### Verification
- [ ] Every required feature has appropriate test coverage.
- [ ] Critical interactions have been executed in the application.
- [ ] Visual comparisons have been performed where relevant.
- [ ] Regression tests have been added for discovered defects.
- [ ] Available build and test checks have been executed.
- [ ] Remaining failures and unverified areas are documented.
- [ ] No known unresolved in-scope defects are being silently ignored.

### Delivery
- [ ] Audit and implementation checklists are updated.
- [ ] Evidence supports the reported verification status.
- [ ] The final changes are reviewable and maintainable.
- [ ] Known limitations, blockers, and follow-up requirements are explicit.

A checklist entry must never be marked verified merely because its implementation exists.

A feature may be marked blocked only when the blocker and its impact are documented.

Do not declare completion while known, required, in-scope defects remain unresolved.

---

## 13. Working Instructions

Follow these rules throughout the task:

**Rule 1 — Investigate before implementing.** Understand the reference behavior and existing architecture before making changes.

**Rule 2 — Be exhaustive and systematic.** Discover functionality through actual exploration, not assumptions about what a typical application contains.

**Rule 3 — Maintain traceability.** Link each observation to an implementation task and a verification result.

**Rule 4 — Verify behavior, not just appearance.** Screenshots cannot prove that interactions, state transitions, or business logic work correctly.

**Rule 5 — Implement real functionality.** Do not substitute static UI, hardcoded fake results, or nonfunctional controls for required features.

**Rule 6 — Preserve working code.** Avoid unrelated rewrites and test existing workflows after modifying shared components.

**Rule 7 — Fix root causes.** Do not add superficial workarounds when a structural defect needs correction.

**Rule 8 — Retest every fix.** A change is not complete until the affected behavior and relevant regression scenarios have been checked.

**Rule 9 — Be transparent about limitations.** Distinguish observed facts, assumptions, implemented features, verified features, and blocked work.

**Rule 10 — Continue until the audit is exhausted.** Do not stop after the first screen, the first successful build, the initial implementation, or the issues originally reported.

**Rule 11 — Do not overclaim.** Report only the screens explored, features implemented, tests executed, and results actually verified.

**Rule 12 — Deliver working software.** The final outcome must be a functioning implementation integrated into the existing project, not merely a plan, report, or demonstration.

---

## 14. Start Here

Begin executing the task now.

**Step 1:** Inspect the existing repository and establish a baseline.

**Step 2:** Identify the reference application and determine which reference-access and device-control capabilities are actually available.

**Step 3:** Open the reference application and start with its first screen. Explore it systematically, documenting every discovered screen, interaction, state, and navigation destination.

**Step 4:** Continue until the accessible feature inventory is sufficiently complete, maintaining the audit documents as you go.

**Step 5:** Compare the reference findings against the existing application and generate a prioritized, traceable gap list.

**Step 6:** Implement the missing functionality and design changes in dependency order.

**Step 7:** Test each feature, fix defects, and run regression checks continuously.

**Step 8:** Perform a final reference-to-implementation gap analysis and close every identified, in-scope gap that can be resolved with the available access and dependencies.

**Step 9:** Deliver the updated application and an evidence-based report showing completed features, verification results, known limitations, and any remaining blockers.

Do not stop at the planning stage. Execute the work using the available tools, inspect actual files and runtime behavior, make the necessary code changes, and validate the results.

If you encounter a blocker, document it, continue with independent tasks, and return to it when possible. Ask for clarification only when a genuinely necessary decision or unavailable resource prevents a responsible implementation.

**Final objective: Build the most complete, reliable, maintainable, and faithful implementation supported by the available reference and requirements, with no known in-scope gaps silently overlooked.**
