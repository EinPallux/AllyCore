# ✦ AllyCore
### *Your loyal AI Companion — forged in battle, grown through glory.*

> **Version:** 1.0.0 · **API:** Paper 1.21.1 · **Author:** Pallux

---

## 📖 Overview

AllyCore introduces a fully custom **AI Companion** — your personal Ally — that fights alongside you, levels up with you, and grows stronger the more you invest in them. Every Ally is unique to its owner, fully configurable, and deeply integrated with your server's economy.

No Citizens. No external AI frameworks. Every behaviour is written from scratch.

---

## ✨ Features at a Glance

| Feature | Description |
|---|---|
| 🤖 Custom AI | Fully hand-written pathfinding & combat AI — no Citizens dependency |
| ⚔ Combat Modes | Defensive, Aggressive, Neutral, Allround |
| 📊 Stat Tree | Upgrade Health, Attack, Defense, Speed, Regeneration |
| 🛡 Armory | Upgrade Armor (Leather → Netherite) and Weapon (Wood → Netherite) |
| 🎖 Levelling | Ally gains XP from kills and walking distance |
| 💀 Death & Revive | Ally can die and be revived for a cost |
| 🪙 Dual Currency | Vault money + custom **Mementos** currency |
| 🏷 Holograms | Floating name, level, health and stats above every Ally |
| ✏ Custom Names | Name your Ally anything you like |
| 🎯 Attack Command | Point your crosshair and order your Ally to attack |
| 🛒 Built-in Shop | Integrated shop to purchase an Ally |
| 📐 PlaceholderAPI | 15 placeholders for scoreboards, menus and more |
| 🎨 Hex Color Support | Full `&#RRGGBB` and `&x` legacy color support everywhere |
| ⚙ Fully Configurable | Every number, message, cost and GUI slot is in a config file |

---

## 🔧 Requirements

| Dependency | Type | Notes |
|---|---|---|
| [Paper 1.21.1](https://papermc.io) | **Required** | Spigot may work but is untested |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | **Required** | Plus any economy plugin (EssentialsX, CMI, etc.) |
| [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) | Recommended | Required for custom Ally skins |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Optional | Enables all `%allycore_*%` placeholders |

---

## 🚀 Installation

1. Download `AllyCore-1.0.0.jar` and drop it into your `/plugins` folder.
2. Make sure **Vault** and an economy plugin are installed.
3. Optionally install **ProtocolLib** (skins) and **PlaceholderAPI** (placeholders).
4. Start your server — AllyCore generates all config files automatically.
5. Edit the configs in `/plugins/AllyCore/config/` to your liking.
6. Reload with `/allyadmin reload` — no restart needed for config changes.

---

## 🎮 Gameplay

### Getting an Ally
Players can purchase an Ally from the built-in shop:
```
/allyshop
```
Or an admin can give one directly:
```
/allyadmin give <player>
```

---

### The Ally Menu
**Shift + Right-Click** your Ally to open the full management menu.

```
┌─────────────────────────────────┐
│          ✦ Ally Menu ✦          │
│                                 │
│   [Stat Tree]  [Info]  [Armory] │
│                                 │
│  [Follow/Stay] [Name] [Summon]  │
│                                 │
│              [Revive]           │
└─────────────────────────────────┘
```

---

### Combat Modes
Set how your Ally behaves in combat via the menu or `/ally mode <mode>`:

| Mode | Behaviour |
|---|---|
| 🔵 **Defensive** | Only attacks when you or the Ally are attacked |
| 🔴 **Aggressive** | Auto-attacks any hostile mob within range |
| ⚪ **Neutral** | Never engages in combat |
| 🟡 **Allround** | Attacks hostiles on sight AND defends you |

---

### Levelling Up
Your Ally gains XP from two sources:

- **Killing mobs** — XP scales by mob difficulty (Easy → Boss)
- **Walking distance** — A small trickle of XP for every block travelled together

On level up your Ally receives a small stat bonus and is fully healed. The main way to make your Ally powerful is through the **Stat Tree** and **Armory** — levels are a bonus, not the core progression.

---

### The Stat Tree
Upgrade five core stats, each with up to **10 tiers**. Each tier costs Vault money + Mementos.

| Stat | Effect per Tier |
|---|---|
| ❤ **Health** | +5 Max HP |
| ⚔ **Attack** | +1 Attack Damage |
| 🛡 **Defense** | +1 Armor (damage reduction) |
| ⚡ **Speed** | +0.02 Movement Speed |
| ✨ **Regeneration** | +0.5 HP regenerated per second |

---

### The Armory
Upgrade your Ally's equipment in two tracks:

**Armor Track** (6 tiers):
`No Armor → Leather → Iron → Gold → Diamond → Netherite`

**Weapon Track** (7 tiers):
`Bare Hands → Wooden → Stone → Iron → Gold → Diamond → Netherite`

Higher tiers significantly boost defense and attack. Each tier has its own Vault + Memento cost defined in `config/upgrades.yml`.

---

### Mementos 💜
**Mementos** are AllyCore's custom currency, earned exclusively through battle:

- Your Ally assists in killing a mob → **+2 Mementos**
- Your Ally helps kill a player → **+10 Mementos**
- Boss kill bonus → **+25 extra Mementos**
- Higher Ally level = slight earn multiplier

Mementos are spent alongside Vault money on every upgrade and revival. Check your balance with `/ally mementos`.

---

### Death & Revival
Your Ally can die in battle. When they do:

- A death message is sent to you.
- The Ally despawns and is marked as **Fallen**.
- To bring them back, use `/ally revive` or the **Revive button** in the menu.
- Revival costs **Vault money + Mementos** (configurable, scales with level).

Admins can revive any Ally for free with `/allyadmin revive <player>`.

---

### Commanding Your Ally
Beyond the menu, you can directly command your Ally in the field:

| Action | How |
|---|---|
| Order attack | Look at a mob/player, use `/ally attack` |
| Cancel attack | `/ally attack cancel` |
| Toggle follow | `/ally follow` or via menu |
| Make them stay | `/ally stay` or via menu |
| Change mode | `/ally mode <DEFENSIVE\|AGGRESSIVE\|NEUTRAL\|ALLROUND>` |

---

## 💬 Commands

### Player Commands
> Default permission: `allycore.player` (all players)

| Command | Description |
|---|---|
| `/ally` | Open the Ally Menu |
| `/ally help` | Show all commands |
| `/ally summon` | Summon your Ally |
| `/ally dismiss` | Dismiss your Ally |
| `/ally info` | View your Ally's full stats |
| `/ally rename <name>` | Give your Ally a custom name |
| `/ally attack [cancel]` | Order Ally to attack your crosshair target |
| `/ally follow` | Set Ally to follow you |
| `/ally stay` | Set Ally to hold position |
| `/ally mode <mode>` | Change combat mode |
| `/ally revive` | Revive your fallen Ally |
| `/ally mementos` | Check your Memento balance |
| `/allyshop` | Open the Ally Shop |

### Admin Commands
> Permission: `allycore.admin` (OP by default)

| Command | Description |
|---|---|
| `/allyadmin give <player>` | Give a player an Ally for free |
| `/allyadmin remove <player>` | Remove a player's Ally |
| `/allyadmin reload` | Reload all config files |
| `/allyadmin setstat <player> <stat> <tier>` | Set a stat tier directly |
| `/allyadmin setlevel <player> <level>` | Set Ally level directly |
| `/allyadmin revive <player>` | Revive a player's dead Ally for free |
| `/allyadmin list` | List all Allies on the server |
| `/allyadmin mementos <player> <set\|add\|remove> <amount>` | Manage Mementos |
| `/allyadmin info <player>` | View detailed Ally info for a player |

---

## 🔑 Permissions

| Permission | Description | Default |
|---|---|---|
| `allycore.player` | Access to all player features | ✅ Everyone |
| `allycore.admin` | Access to all admin commands | OP only |
| `allycore.bypass.cost` | Bypass all upgrade/shop costs | OP only |
| `allycore.bypass.revive` | Bypass revive cost | OP only |

---

## 📐 PlaceholderAPI Placeholders

Use these in scoreboards, chat formatters, DeluxeMenus, and more:

| Placeholder | Returns |
|---|---|
| `%allycore_has_ally%` | `true` / `false` |
| `%allycore_ally_name%` | Ally's display name |
| `%allycore_ally_level%` | Current level |
| `%allycore_ally_xp%` | Current XP |
| `%allycore_ally_xp_needed%` | XP needed for next level |
| `%allycore_ally_health%` | Current health |
| `%allycore_ally_max_health%` | Maximum health |
| `%allycore_ally_attack%` | Attack damage |
| `%allycore_ally_defense%` | Defense value |
| `%allycore_ally_speed%` | Movement speed |
| `%allycore_ally_mode%` | Current combat mode |
| `%allycore_ally_alive%` | `true` / `false` |
| `%allycore_ally_summoned%` | `true` / `false` |
| `%allycore_ally_armor_tier%` | Armor upgrade tier (0–5) |
| `%allycore_ally_weapon_tier%` | Weapon upgrade tier (0–6) |
| `%allycore_mementos%` | Player's Memento balance |

---

## ⚙ Configuration Files

All configs live in `/plugins/AllyCore/`. Every value is documented inline.

| File | Controls |
|---|---|
| `config.yml` | Plugin prefix, skin settings (username or custom texture), database type (SQLite / MySQL) |
| `config/ally.yml` | Base stats, level-up bonuses, AI behavior, combat ranges, XP rates, follow distances, combat modes |
| `config/upgrades.yml` | Stat tree tiers, bonus-per-tier, vault & memento costs per tier, armor & weapon tier definitions |
| `config/messages.yml` | Every player-facing message, hologram line formats, sound effects |
| `config/economy.yml` | Mementos earn rates, revive costs, shop price |
| `config/gui.yml` | Every GUI title, button slot, material and lore line |

### Custom Ally Skin
Set a skin for all Allies in `config.yml`:
```yaml
skin:
  mode: USERNAME          # USERNAME or CUSTOM
  username: "AllyKnight"  # Skin fetched from Mojang API automatically
```
Or supply your own texture from [mineskin.org](https://mineskin.org):
```yaml
skin:
  mode: CUSTOM
  texture-value: "eyJ0Z..."
  texture-signature: "abc123..."
```

### Database
```yaml
database:
  type: SQLITE   # or MYSQL
  mysql:
    host: localhost
    port: 3306
    database: allycore
    username: root
    password: ""
```

### Color Codes
AllyCore supports full hex colors and legacy codes in **all** config strings:
```
&#6C63FF  →  Hex color
&a        →  Legacy green
&l        →  Bold
```

---

## 🗂 File Structure

```
plugins/AllyCore/
├── config.yml              ← Main config (skin, database, prefix)
├── allycore_data.db        ← SQLite database (auto-created)
└── config/
    ├── ally.yml            ← AI, stats, combat, XP
    ├── upgrades.yml        ← Stat tree & armory costs
    ├── messages.yml        ← All messages & sounds
    ├── economy.yml         ← Costs, shop, Mementos
    └── gui.yml             ← All GUI layouts
```

---

## ❓ FAQ

**Q: Can players have more than one Ally?**
No — one Ally per player. This is intentional and not configurable.

**Q: What happens to my Ally when I log off?**
Your Ally is safely saved and despawned. When you log back in, they automatically respawn next to you if they were summoned.

**Q: What happens if my Ally and I are in different worlds?**
The Ally will teleport to your location automatically.

**Q: Does the Ally attack other players?**
Only if you explicitly use `/ally attack` while targeting them, or if that player attacks you first (in Defensive/Allround mode with `defend-from-players: true`).

**Q: Can I use DeluxeMenus to sell Allies?**
Yes — the admin command `/allyadmin give <player>` is perfect for console execution from DeluxeMenus, ShopGUI+, or any plugin that runs commands on purchase.

**Q: The Ally skin isn't showing. Why?**
ProtocolLib must be installed. Without it, the Ally appears as a regular Zombie. Check your server log for `[AllyCore] ProtocolLib hooked successfully!`.

---

## 🐛 Support & Feedback

Found a bug or have a feature request? Reach out via the Pallux Discord or open an issue on the project repository.

---

*AllyCore © 2024 Pallux — All rights reserved.*
