# Accessible Tabs

Write `Tabs` in `Tabs.tsx` following the
[WAI-ARIA tabs pattern](https://www.w3.org/WAI/ARIA/apg/patterns/tabs/) with **automatic
activation** (a tab is selected as soon as it gets focus with the keyboard).

```tsx
<Tabs
  label="Settings"
  tabs={[
    { id: "profile", title: "Profile", content: <ProfileForm /> },
    { id: "billing", title: "Billing", content: <Billing />, disabled: true },
    { id: "security", title: "Security", content: <Security /> },
  ]}
  defaultTabId="security"
  onChange={(id) => console.log(id)}
/>
```

## Structure

- A container with `role="tablist"` and `aria-label={label}`.
- One `<button type="button" role="tab">` per tab, with its `title` as text. The selected tab
  has `aria-selected="true"`, the others `"false"`. Each tab has `aria-controls` set to the id
  of the panel.
- One element with `role="tabpanel"` for the **selected** tab only, holding its `content`,
  with `aria-labelledby` set to the id of its tab and `tabIndex={0}`.
- Ids must be unique even with two `Tabs` on the page (use `useId`).

## Behaviour

- The initial tab is `defaultTabId`, or the first enabled tab.
- Clicking an enabled tab selects it. A disabled tab has `aria-disabled="true"` (and stays
  focusable with arrow keys skipping it) and cannot be selected.
- **Roving tabindex**: the selected tab has `tabIndex={0}`, every other tab `-1`, so Tab moves
  from outside into the selected tab, and Tab again into the panel.
- Keys on a focused tab: **ArrowRight** / **ArrowLeft** select and focus the next / previous
  enabled tab, wrapping around; **Home** / **End** the first / last enabled tab.
- `onChange(id)` is called each time the selection changes (not when the selected tab is
  chosen again).

There are 12 tests: 1 in `tests/visible.test.tsx` and 11 hidden ones. Run them with
`npm install` once, then `npm test`.
