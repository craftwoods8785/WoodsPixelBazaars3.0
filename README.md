# WoodsPixelBazaars (PaperMC 1.21)

A Hypixel-style Bazaar plugin for PaperMC 1.21 with:

- `/bz` command (permission-gated)
- Category-based Bazaar GUI
- Instant buy/sell with click controls
- Dynamic supply/demand pricing
- EssentialsX economy support through Vault
- Persistent market multipliers and basic price history

## Build

```bash
mvn clean package
```

Jar output:

- `target/woodspixel-bazaars-1.0.0.jar`

## Install

1. Install **Vault** and **EssentialsX** on your Paper 1.21 server.
2. Put the plugin jar into `plugins/`.
3. Restart server.

## Commands

- `/bz` - Open WoodsPixel Bazaar
- `/bz reload` - Reload config (admin permission)

## Permissions

- `woodspixelbazaars.use` (default: true)
- `woodspixelbazaars.admin` (default: op)

## GUI Controls

Inside item category menu:

- Left click: Buy 1
- Shift + Left click: Buy 64
- Right click: Sell 1
- Shift + Right click: Sell 64

## Notes

- Buy transactions check economy coins before processing.
- If player has partial inventory space when buying, only delivered amount is charged.
- Prices move up when players buy and down when players sell.
- Prices slowly recover toward baseline over time.
