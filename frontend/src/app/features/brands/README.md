# Brand Page Render Flow

The Brand route keeps the page header and main two-column frame mounted on first render. Loading, empty, and error states render inside the roster and detail cards so route activation never shows detached content or a missing page title.

Render sequence:

- `WorkspaceStore.activeWorkspaceId` and `PermissionStore.canViewBrands` drive the initial brand load.
- `BrandStore.load` clears stale data when the workspace changes, loads the active workspace brands, and selects the first available brand after data arrives.
- `BrandsComponent` reads selected brand state directly from `BrandStore`; it does not keep a second selected brand copy.
- The roster card shows skeleton rows while loading, a friendly empty state when no brands exist, and a retry state on API failure.
- The detail card shows a skeleton while loading, the selected brand after load, or `Select a brand to see details.` when no selection is available.

Do not use click refreshes, deferred route content, `setTimeout`, manual DOM work, or forced change detection to make Brand content appear.
