# WCI Shopping Integration

WynnExtras includes a lightweight WCI shopping helper for WynnBuilder crafted builds.

## Supported workflow

1. Copy a supported WynnBuilder crafter link or builder link that embeds crafted items.
2. Open the handled-screen shopping panel with `/we wci` or enable it with `/we wci enable`.
3. Click **Import** in the panel to add the clipboard build requirements to the WCI cart.
4. Use the row list to review needed ingredients and crafting materials. Row details show requirement type, need/have counts, remaining count, and Trade Market query text.
5. Click a row to copy/search the matching Trade Market query when the Trade Market workflow is available.
6. Use row copy actions or `/we wci copy` to copy shopping-list text.
7. Use **Clear** or `/we wci clear` to empty the persisted cart.

## Commands

- `/we wci` toggles the WCI shopping panel.
- `/we wci enable` enables the WCI shopping panel.
- `/we wci disable` disables the WCI shopping panel.
- `/we wci clear` clears the WCI shopping cart and saves the change.
- `/we wci copy` copies the WCI shopping list to the clipboard.
- `/we wci resetposition` resets the draggable panel and launcher positions to their default handled-screen placements.

The `/wynnextras` alias exposes the same `wci` subcommands.

## Backend notes

- WynnBuilder imports are all-or-nothing: stale or unknown ingredient and recipe IDs fail the import rather than partially mutating the cart.
- Cart entries aggregate by actual requirement identity. Source/provenance text is preserved for display but is not used to split identical requirements.
- Crafted ingredient requirements remain distinct from recipe material requirements.
- The cart is persisted per player and restored when the player context is available.
- The registry resources under `assets/wynnextras/wci` provide WynnBuilder ingredient and recipe lookup data.

## Known limitations

- The feature does not automate purchases.
- The feature does not auto-scan or open bank pages.
- Trade Market interactions stay limited to the existing row workflow.
- Bank and inventory counts depend on currently available WynnExtras/Wynntils client-side data.
