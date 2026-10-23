# Prompt: source-backed QA workbook

Copy everything below the line into the other project. Fill the three blanks at the top, then send it as the whole task. Do not attach Max Player cases. The other agent must read that project's code.

---

You are writing a local Excel QA workbook for this project. Do not change app code. Do not invent screens, buttons, logins, or gestures that are not in the source.

Project name: `<PROJECT NAME>`
Repo root: `<ABSOLUTE PATH>`
Output file: `<REPO>/docs/<ProjectName>-QA.xlsx`

## What "done" means

A tester who has never seen the code can open the workbook, start at P0, and execute every row. Each row says what to do, what must happen, and what must not happen. The labels in the row are the labels on screen, copied from source. If the source has no login, there is no Login sheet. If a button only toasts "not implemented", the expected result is that toast, not the feature the button name suggests.

Read the code before you write a case. Prefer the UI file, the navigation graph, and the view-model or event contract over a README. A README feature list is not evidence that a control exists.

## How to inventory

1. Find every navigation destination: routes, tabs, drawer or sidebar items, bottom bars, deep links, and modal screens that are not routes (player, dialogs, sheets).
2. For each destination, list every visible string, content description, icon button, chip, menu row, switch, slider, picker, text field, and gesture.
3. Record the gate for each control: which platform, window size, setting, empty state, permission, or capability shows or hides it.
4. Record the exact result from code: navigation target, toast text, dialog title and button labels, what Cancel does, what a failed platform call does.
5. Search the repo for each setting key. If the key is only declared and drawn in settings, and no player, list, or service reads it, the case must say "checkbox saves only; playback does not read this key." Do not write the summary sentence as if it were implemented.
6. Separate shared widgets (file menus, confirm dialogs, view options) onto their own sheet if more than one screen uses them. Say which screens open them.
7. Put features that only show an explanation page or a "not available" toast on a Not Built sheet. Do not hide them, and do not write steps that assume they work.

Also cover, when the code has them:

- Cold start and the real start destination
- System permission dialogs versus in-app screens
- Empty, loading, and error copy, quoted exactly
- Back, system back, and selection-mode back
- Cancel versus confirm on every destructive dialog
- What a denied system consent dialog leaves behind
- Rotation and process death only if you can say what the code actually restores
- Wide versus narrow layout, if width changes navigation
- Every platform branch (Android, iOS, desktop, web, or whatever this repo targets)

## Workbook shape

Create a new `.xlsx`. Do not edit an existing QA file in place.

Sheet order:

1. `Overview`
2. `P0 First`
3. One sheet per real screen or shared surface, in the order a tester walks the app
4. `Not Built` last, only if such gaps exist

Sheet names must be 31 characters or fewer and must match the on-screen name a tester would recognise. Do not create a sheet for a screen that does not exist. Do not merge two unrelated screens onto one tab just to save sheets. A player, its gesture layer, and its panels may be separate sheets if one sheet would be unusable. Settings index can be one sheet and each settings page another, or one Settings sheet plus one tuner sheet. Pick the split a tester can finish.

### Overview

- A1: `<PROJECT NAME> QA`
- A2: one sentence that the cases come from this repo's source, and name any major screen the user might expect that does not exist
- Row 4 headers: `Screen tab`, `Open`, `Cases`, `P0`, `P1`, `P2`, then one Pass column and one Fail column for the primary platform, then `Not run`
- One row per case sheet. `Open` is a hyperlink to that sheet's A1, label `Go to <sheet>`
- Cases formula: `=COUNTA('<Sheet>'!C:C)-1`
- P0/P1/P2: `=COUNTIF('<Sheet>'!J:J,"P0")` and the same for P1 and P2
- Pass/Fail: `=COUNTIF` on the primary result column
- Not run: cases minus pass, fail, blocked, and N/A on that result column
- A TOTAL row with `=SUM` of those columns
- Below the table, four short how-to-test lines: sort is already P0 then P1 then P2; result values are Pass, Fail, Blocked, or N/A; which platforms have a real engine or backend; do not mark an unshipped platform Fail only because the feature cannot run there

Quote sheet names that contain a space: `=COUNTA('File Actions'!C:C)-1`

### P0 First

Columns: `#`, `Screen`, `Section`, `Title`, `Open`

One row per P0 case, in the same screen order as Overview. `Open` hyperlinks to that case's row on its sheet. This is an index, not a second copy of the steps.

### Every case sheet

Row 1 headers, exactly:

| Col | Header |
|---|---|
| A | `#` |
| B | `Section` |
| C | `Title` |
| D | `Platform` |
| E | `Condition (when it shows)` |
| F | `Flow (Steps)` |
| G | `Expected Result` |
| H | `Must Not Happen` |
| I | `Platform note` |
| J | `Priority` |
| K onward | one Result column per platform this app ships, named `<Platform> Result` |
| last | `QA Input / Clarification` |

If the app is only Android, use `Android Result` only and do not invent Desktop, iOS, and Web columns. If it ships four targets, use four result columns, as in the reference shape: Android, Desktop, iOS, Web. Leave every result cell empty.

Sort each sheet by Priority (P0, then P1, then P2), then Section, then Title. After sorting, number `#` from 1. The visual order is the test order.

Data validation on every result column: list `Pass,Fail,Blocked,N/A`, blank allowed. Pass fills green, Fail red, Blocked orange.

## How to write one case

A case is one executable check, not a paragraph that covers five buttons.

- Title is the control or the rule, short enough to scan: `Delete cancel keeps the file`, not `Test delete`.
- Condition says when the control exists. If it is hidden on web, say so here, not only in Must Not.
- Steps are imperative and ordered. Name the exact label to tap. Include the setup: which setting, which data, which platform.
- Expected Result is what the code does, including toast text, dialog title, button labels, and the next screen. Quote UI strings.
- Must Not Happen is the wrong screen, the data loss, the silent success, or the lookalike control. Every row needs this. Do not leave it blank.
- Platform note says where the engine, permission, or capability differs. If a platform cannot perform the action, say N/A, not Fail.
- Priority is only `P0`, `P1`, or `P2`.

Priority rules:

- P0: cannot use the app, wrong destination, data loss, delete or reset or payment or logout, permission denial crash, security gate (PIN, login, paywall), primary play or submit or save, confirm versus cancel on a destructive action
- P1: a normal CTA, navigation, a setting that changes behaviour the code actually reads, empty and error states on a main screen
- P2: cosmetic, rare format, an unwired setting, a tooltip, a second way to do something already covered as P0

Do not mark a dead button P0 just because its label sounds important. Mark the case P1 and expect the toast or explanation page.

## Coverage required on each screen

For every control found in source, write the cases that apply. Skip a bullet only if that state cannot happen in code.

- Control is visible in the condition you named, and hidden when the gate is false
- Primary action, and the exact next screen or toast
- Cancel, dismiss, or system back does not apply the action
- A second tap does not double-submit, double-navigate, or stack the same screen
- Empty, loading, and error copy
- Disabled versus hidden, if the code disables instead of hiding
- Persistence: leave the screen and come back; force-stop only if the value is stored
- The action does not change an unrelated setting, list, or account
- Platform branch: one case, or an explicit platform note, when Android and web do different things
- If several choices exist (sort keys, theme modes, decoder options), one case per choice when the choices do different things. One case is enough when they share a picker and you list every choice in Expected Result

For a player or media surface, if the code has them, do not skip: play, pause, next, previous, seek, lock, error dialog, resume, and each gesture flag. For a form, do not skip: valid submit, empty submit, invalid format, and back with unsaved text.

For settings:

- A checkbox row applies immediately unless the code waits for OK
- A choice dialog applies on pick and Cancel keeps the old value, if that is what the code does
- A slider commits on release if that is what the code does
- A text dialog OK saves trimmed text; CANCEL discards
- Reset and clear-history require a confirm case and a cancel case
- Export or import cancel is silent if the code shows no toast

## Formatting

- Header row: bold white text, dark fill, wrap, freeze at A2, auto-filter on the full table
- Priority cell fill: P0 light red, P1 light amber, P2 light grey
- Wrap text on all body cells. Row height about 48. Do not shrink text to fit.
- Column widths: `#` narrow; Title, Steps, Expected, Must Not wide enough to read without opening the cell
- Landscape, fit to width 1, repeat header row on print, footer page number
- Overview and P0 First tab colours can differ from case sheets. Case sheets use one calm colour. Not Built uses a muted colour.
- Gridlines off is fine. Do not add charts.

Formulas must stay formulas. Do not paste the counts as hardcoded numbers on Overview. After writing, recalculate if a local engine is available, then reopen and check Overview totals match the number of body rows.

## Do not

- Do not copy cases from another app, including Max Player, MX Player, or a sample QA spreadsheet. Those files are format references only.
- Do not add a Login, OTP, Cart, or Profile sheet because a template had one.
- Do not describe a setting summary as behaviour unless a non-settings file reads that key.
- Do not write "should work" or "user-friendly". Write the observable result.
- Do not merge Pass and Fail into one cell. Testers type Pass, Fail, Blocked, or N/A.
- Do not hide gaps. A missing engine, a no-op share, or a button that only toasts is a case.
- Do not generate hundreds of duplicate rows that only change the title. Volume comes from real controls, choices, cancel paths, and platform branches.
- Do not stop at the happy path.

## Verify before you deliver

1. Every navigation destination appears on Overview or inside a parent sheet you name in Platform note.
2. Every on-screen label you found has at least one row, or is named inside a row that lists a closed set (for example every sort key).
3. Every sheet is sorted P0, P1, P2, and `#` is 1..n in that order.
4. No Expected Result and no Must Not Happen cell is empty.
5. Result cells are empty and have the Pass/Fail/Blocked/N/A dropdown.
6. Overview formulas return the same totals as a manual count of body rows.
7. Spot-check five P0 rows against source. If a toast or route is wrong, fix the set, do not only fix the sample.

Deliver the `.xlsx` only. Keep a generator script in the repo if you used one, but the workbook is the deliverable. In the reply, give the path, the sheet list, total cases, and P0 count. Name any screen you refused to invent, and any control that exists in UI but is not read by behaviour code.
