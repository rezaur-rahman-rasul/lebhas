# Auth Dialog Layout Rules

The auth dialog uses one fixed-height split shell for both login and register modes. Login has fewer fields and register has more fields, but the outer modal height, close button position, compact tab switcher position, and right marketing panel height must not change when switching tabs.

Layout rules:

- The modal panel owns the responsive fixed height through `panelClass`.
- The modal content disables outer scrolling and fills the panel height.
- `auth-dialog-split` fills the modal height.
- `auth-dialog-panel` and `auth-dialog-visual` both use `height: 100%` and `min-height: 0`.
- `auth-dialog-form-scroll` is the emergency overflow area for very short viewports; normal desktop/laptop heights should not show a visible scrollbar.
- The tab switcher stays compact at roughly 44px high, with 36px tab buttons.
- The right marketing panel is visible on desktop and stays independent from form content height.

Do not add DOM measurements, fake resize events, `setTimeout`, or click-based refreshes to stabilize the dialog. Fix height issues through the shared shell structure and CSS only.
