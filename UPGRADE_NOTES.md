# SkyAuctions Premium Upgrade

Implemented from the supplied reference screenshots:

- Premium small-caps/gradient sale action-bar announcement.
- Main GUI bottom navigation:
  - Ender Chest = Sold Items / earnings
  - Glowstone = Expired / returned items
  - Red Shulker = previous page
  - Bell = refresh
  - Lime Shulker = next page
  - Book = information
- Premium auction item hover lore with seller, price, amount and time left.
- Click sold listing to collect its money.
- Click expired/cancelled listing to return the item.
- `/ah` and `/auction` aliases.
- Added command set: `help`, `sell`, `bid`, `collect`, `expired`, `force_end_all`, `listed`, `view`, `remove`, `reload`, `test`.
- Added permissions for admin force-expire and action-bar testing.
- Included GitHub Actions Maven build workflow.
