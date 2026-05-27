# Products / Services Render Flow

The Products / Services page keeps the route frame mounted from the first render:

- the page header always renders before catalog data resolves
- the roster, detail, and relationship rule cards share one stable grid
- loading, empty, unavailable, and error states render inside those cards
- `ProductServiceStore` owns the selected catalog item and auto-selects the first loaded item
- workspace changes clear stale catalog state before loading the next workspace
- brand context is loaded alongside product services so relationship copy can render without a click

Do not replace the page grid with top-level loading branches or click-based refresh logic. The shell should stay stable while card content changes reactively.
