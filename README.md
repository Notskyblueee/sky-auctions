# SkyAuctions — Premium Edition

Paper 1.21.x Auction House plugin with a clean, premium GUI inspired by the supplied reference screenshots.

## Commands

Both command aliases work:

- `/ah`
- `/auction`

Subcommands:

- `/ah sell <price>` — list the item in your hand
- `/ah listed` — your active listings
- `/ah collect` — collect all sold money and returned items
- `/ah expired` — expired/cancelled item GUI
- `/ah bid <listing-id>` — purchase a listing by UUID
- `/ah view <listing-id>` — show listing details
- `/ah remove <listing-id>` — cancel your listing
- `/ah reload` — reload config/messages
- `/ah help` — command help
- `/ah force_end_all` — admin force-expire active listings
- `/ah test` — admin action-bar test

## Main GUI layout

The bottom navigation follows the supplied screenshot:

1. **Ender Chest** — Sold Items / pending earnings
2. **Glowstone** — Expired / returned items
3. **Red Shulker Box** — Previous page
4. **Bell** — Refresh
5. **Lime Shulker Box** — Next page
6. **Book** — Auction information

The main listing area shows:

- Seller
- Price
- Amount
- Time remaining
- Purchase/cancel hint

Hovering an auction item gives a detailed premium lore layout.

## Sale announcement

When a listing is successfully created, every player with `skyauctions.notify.sale` receives a premium action-bar announcement above the hotbar.

Default:

`✦ Player has put 1x Diamond Sword in auction for $5,000.00 ✦`

Edit it in `plugins/SkyAuctions/config.yml` under `broadcast.format`.

## Collection

**Sold Items / Ender Chest**
- Shows successful sales.
- Shows buyer and amount earned.
- Clicking a sold item collects its pending money.

**Expired Items / Glowstone**
- Shows expired or manually cancelled listings.
- Clicking an item returns it to the player's inventory.

`/ah collect` can collect both categories in one command.

## Build

### Local Maven build

Requirements:
- Java 21
- Maven 3.9+

From the `SkyAuctions` project directory:

```bash
mvn clean package
```

The plugin will be generated at:

```text
target/SkyAuctions-1.0.0.jar
```

### GitHub Actions

The included `.github/workflows/build.yml` automatically builds the plugin on pushes, pull requests, and manual workflow runs.

The compiled JAR is uploaded as a GitHub Actions artifact.

## Dependencies

Required:
- Paper 1.21.x
- Vault
- A Vault-compatible economy plugin

Optional:
- PlaceholderAPI
- ShopGUIPlus
- EconomyShopGUI

## Permissions

- `skyauctions.use`
- `skyauctions.sell`
- `skyauctions.collect`
- `skyauctions.notify.sale`
- `skyauctions.slots.5`
- `skyauctions.slots.10`
- `skyauctions.slots.20`
- `skyauctions.slots.50`
- `skyauctions.bypass.price`
- `skyauctions.bypass.limit`
- `skyauctions.bypass.tax`
- `skyauctions.admin.reload`
- `skyauctions.admin.remove`
- `skyauctions.admin.remove.others`
- `skyauctions.admin.force-end`
- `skyauctions.admin.test`
