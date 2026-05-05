# angelTrade

A Minecraft Paper plugin (1.21.1) providing two interconnected economy systems:
**Trade Routes** and **Trade Shops (Caravans)**.

---

## Requirements

| Dependency | Notes |
|---|---|
| Paper 1.21.1 | Server target |
| Vault | Economy abstraction — required |
| angelEconomy | **WIP — API integration pending** (see below) |
| angelCompany | **WIP — API integration pending** (see below) |

---

## ⚠️ WIP Integration Notice

### angelEconomy (Server /shop price display)
The Trade Shop GUI is designed to show the **server /shop price alongside company prices**
for direct comparison. This integration is currently **stubbed out** in `TradeShopGUI.java`:

```java
// Stub: replace this line with actual server shop price lookup
lore.add("\u00A77Server Price:  \u00A7f$?.?? \u00A78(angelEconomy — WIP)");
```

Once angelEconomy exposes a price-lookup API, replace the stub with:
```java
double serverPrice = AngelEconomyAPI.getItemPrice(si.getItemKey());
lore.add("\u00A77Server Price:  \u00A7f$" + String.format("%.2f", serverPrice));
```

### angelCompany (Company ownership & ledger)
Routes and Shops are intended to be **owned by companies** with member permission
delegation and ledger logging. This is currently stubbed — `companyId` is stored as
a plain String. Once angelCompany is available, replace with:
```java
Company company = AngelCompanyAPI.getCompany(player);
String companyId = company.getId(); // or UUID
boolean isOfficer = company.isOfficer(player);
company.getLedger().log(...);
```

Search for `// STUB: angelCompany` comments in the source to find all integration points.

---

## Systems

### 🪨 Trade Routes
Physical waystone-to-waystone delivery routes with tier progression,
decay mechanics, insurance, and saboteur detection.

#### Tiers
| Tier | Uses | Value Bonus |
|---|---|---|
| Dirt Road | 0–20 | +5% |
| Stone Road | 21��60 | +12% |
| Gold Road | 61–150 | +20% + passive income |
| Royal Road | 151+ | +30% + passive income |

#### Decay
| Days Unused | Result |
|---|---|
| 7 | Warning notification |
| 14 | INACTIVE — no bonuses |
| 21 | BROKEN — route deleted |

Broken routes have a **3-day grace period** to revive by replacing the waystone.

#### Commands
```
/route create          — Begin route creation (use Route Deed + Waystones)
/route list            — List your routes
/route info <id>       — View route details
/route insure <id>     — Insure a route against decay
/route remove <id>     — Remove a route
```

### 🏪 Trade Shops (Caravans)
Company-owned shop blocks placeable in a public marketplace.
Right-click to open a GUI showing company prices vs server prices.

#### Commands
```
/tradeshop place                        — Get a Trade Shop block
/tradeshop relocate <id>               — Relocate your shop
/tradeshop additem <id> <material> <price>   — Add item listing
/tradeshop removeitem <id> <material>  — Remove item
/tradeshop setprice <id> <material> <price>  — Update price
/tradeshop info <id>                   — View shop info
```

#### Admin helpers (op only)
```
/tradeshop give_waystone    — Give yourself a Waystone
/tradeshop give_deed        — Give yourself a Route Deed
```

---

## Crafting Recipes

### Waystone
```
G G G
G L G    G = Gold Ingot, L = Lodestone
G G G
```

### Route Deed
```
_ F _
_ P _    F = Feather, P = Paper, I = Ink Sac
_ I _
```

### Trade Shop Block
```
_ G _
E C E    G = Gold Block, E = Emerald, C = Chest
_ E _
```

---

## Configuration (`config.yml`)

| Key | Default | Description |
|---|---|---|
| `max-route-distance` | 1000 | Max blocks between waystones |
| `decay.warn-days` | 7 | Days before warning |
| `decay.inactive-days` | 14 | Days before inactive |
| `decay.broken-days` | 21 | Days before deletion |
| `revival-grace-days` | 3 | Days to revive broken route |
| `tradeshop-relocate-cooldown` | 300 | Seconds between relocations |
| `upkeep.*` | varies | Daily upkeep cost per tier |
| `route-bonus.*` | varies | Value bonus multiplier per tier |
| `route-passive-income.*` | varies | Passive income % per tier |
| `currency-symbol` | $ | Symbol shown in GUIs |

---

## Database
SQLite — stored at `plugins/angelTrade/angeltrade.db`  
Tables: `trade_routes`, `trade_shops`, `shop_items`, `saboteurs`

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `angeltrade.route.create` | true | Create routes |
| `angeltrade.route.list` | true | List routes |
| `angeltrade.route.info` | true | View route info |
| `angeltrade.route.insure` | true | Insure routes |
| `angeltrade.route.remove` | true | Remove routes |
| `angeltrade.tradeshop.use` | true | Use trade shops |
| `angeltrade.admin` | op | Admin override |

---

## Package Structure
```
me.angelique.angelTrade
├── AngelTrade.java
├── commands/
│   ├── RouteCommand.java
│   └── TradeShopCommand.java
├── gui/
│   └── TradeShopGUI.java
├── listeners/
│   ├── WaystoneListener.java
│   └── TradeShopListener.java
├── managers/
│   ├── BonusManager.java
│   ├── RecipeManager.java
│   ├── RouteManager.java
│   └── TradeShopManager.java
├── models/
│   ├── ShopItem.java
│   ├── TradeRoute.java
│   └── TradeShop.java
└── data/
    └── DataManager.java
```
