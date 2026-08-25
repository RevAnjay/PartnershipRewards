<div align="center">

# ⚔️ PartnershipRewards

[![Build](https://img.shields.io/badge/Build-Gradle-blue.svg)](https://gradle.org/)
[![Paper](https://img.shields.io/badge/Paper-1.20.x-blue.svg)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/Version-1.3.0-green.svg)](#)

**A partnership system for Minecraft featuring quest-based leveling, gifts, shared homes, and rewards.**

[Features](#-features) • [Installation](#-installation) • [Commands](#-commands) • [Permissions](#-permissions) • [Placeholders](#-placeholders) • [Configuration](#%EF%B8%8F-configuration) • [Build](#-build)

</div>

---

## ✨ Features

- 💑 **Partnership System** — Invite other players to become partners with a request/accept system
- 📜 **39 Quest Types** — Ranging from light quests (eating, chatting) to heavy quests (boss kills, raids)
- ⬆️ **Leveling System** — XP & level progression with custom formulas per level
- 🎁 **Gift System** — Send items to partners and claim pending gifts
- 🏠 **Partner Home** — Set, teleport, and delete a shared home (includes warmup & cooldown)
- 🎁 **Milestone Rewards** — Automated rewards based on partnership duration and level
- 🏆 **Bonus Quests** — Rare quests offering higher XP rewards
- 💬 **Partner Chat** — Private chat exclusively for partners (toggle mode & direct message)
- ⚔️ **PvP Toggle** — Toggle PvP protection between partners
- ✨ **Partner Effects** — Automated particle effects based on level (Heart, Happy, EndRod, Cherry)
- 🔥 **Login Streak** — Daily shared login streak for bonus XP
- 🏷️ **Partner Titles** — Automated titles based on level via PlaceholderAPI
- 🕵️ **Admin Spy** — Admins can monitor partner chats
- 🖥️ **Level GUI** — Inventory GUI to view level progress
- 📊 **Top Leaderboard** — Partnership ranking based on level
- 🔗 **PlaceholderAPI Support** — 10 placeholders for scoreboards, chat, etc.
- 💾 **SQLite & MySQL** — Dual database support with HikariCP connection pooling
- ⚡ **Async Operations** — All database operations run asynchronously for zero main-thread blocking

## 📋 Requirements

- **Paper** 1.20.x+
- **Java** 21+

### Optional Dependencies

- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) — For placeholders & partner titles

## 📥 Installation

1. Download the JAR from [Releases](https://github.com/revanjay/PartnershipRewards/releases)
2. Place the `.jar` file in your `plugins/` folder
3. Restart the server
4. Edit `plugins/PartnershipRewards/config.yml`
5. Run `/partneradmin reload`

## 📝 Commands

### Player Commands

| Command | Alias | Description |
|---------|-------|-------------|
| `/partner request <player>` | | Send a partnership request |
| `/partner accept` | | Accept a request |
| `/partner reject` | | Reject a request |
| `/partner break` | | Break the partnership |
| `/partner info` | | View partnership info |
| `/partner quest` | | View active quests |
| `/partner level` | `/partner gui` | Open the level progress GUI |
| `/partner chat [message]` | | Toggle chat mode or send a direct message to partner |
| `/partner gift` | | Send the currently held item to your partner |
| `/partner gifts` | | Claim pending gifts from your partner |
| `/partner sethome` | | Set the partner home (min level required) |
| `/partner home` | | Teleport to the partner home |
| `/partner delhome` | | Delete the partner home |
| `/partner toggle pvp` | | Toggle PvP between partners |
| `/partner toggle effects` | | Toggle particle effects |
| `/partner top` | | View the top 10 partnerships |
| `/partner list` | | View all active partnerships (admin only) |

> **Tip:** `/partner` can also be accessed via `/p` or `/partnership`

### Admin Commands

| Command | Description |
|---------|-------------|
| `/partneradmin reload` | Reload the configuration |
| `/partneradmin reset <player>` | Reset a player's partnership |
| `/partneradmin set <p1> <p2>` | Forcefully create a partnership |
| `/partneradmin toggle spy` | Toggle partner chat spy mode |

> **Tip:** `/partneradmin` can also be accessed via `/pa` or `/padmin`

## 🔑 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `partnershiprewards.use` | Use player partner commands | `true` |
| `partnershiprewards.admin` | Use admin commands + `/partner list` | `op` |

## 🔗 Placeholders

Requires [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/). All placeholders use the `%partner_` prefix.

| Placeholder | Output | Example |
|-------------|--------|---------|
| `%partner_name%` | Partner's name | `Steve` |
| `%partner_level%` | Partnership level | `15` |
| `%partner_xp%` | Current XP | `450` |
| `%partner_duration%` | Partnership duration | `7d 3h` |
| `%partner_days%` | Partnership duration in days | `7` |
| `%partner_online%` | Partner's status | `Online` / `Offline` |
| `%partner_title%` | Title based on level | `[Soulmate]` |
| `%partner_quest%` | Active quest description | `Kill 20 Zombies` |
| `%partner_streak%` | Current login streak | `5` |
| `%partner_has_partner%` | Whether the player has a partner | `true` / `false` |

## ⚙️ Configuration

<details>
<summary>Click to view configuration sections</summary>

A full configuration file is automatically generated in `plugins/PartnershipRewards/config.yml` on the first run.

Configuration includes:

### Database
- Choose between **SQLite** (default) or **MySQL**
- HikariCP connection pool settings

### Quest System
- **39 quest types** that can be toggled individually
- XP per quest, reset hours, cooldowns
- Custom XP requirements per level (overriding the formula)
- Max level configuration
- Bonus quests with custom percentage chances and XP rewards

### Level Rewards
- Custom rewards per level (commands + broadcasts)

### Gift System
- Max pending gifts per player

### Partner Effects
- Particle effects based on level (Heart ≥1, Happy ≥5, EndRod ≥15, Cherry ≥25)
- Interval ticks & max distance limits
- Toggle per-partnership

### Partner Titles
- Automated titles based on level via PlaceholderAPI
- Custom formats & per-level title names

### Partner Home
- Minimum level to set a home
- Warmup seconds (stand still before teleportation)
- Cooldown seconds between teleports

### Login Streak
- Base bonus XP per streak level
- Max streak (default 7 days)
- Custom messages

### Messages
- All messages are fully customizable (prefixes, requests, errors, quests, progress)

</details>

## 🎯 Quest Types

<details>
<summary>Click to view all 39 quest types</summary>

#### 🟢 Light Quests (Low Impact)
| Quest | Description |
|-------|-------------|
| `GIVE_ITEM` | Give items to your partner |
| `SEND_MESSAGE` | Send messages in chat |
| `USE_COMMAND` | Use a specific command |
| `EAT_FOOD` | Eat food together |
| `SLEEP_TOGETHER` | Sleep in beds with your partner |
| `FISH_CATCH` | Catch fish |
| `TRADE_VILLAGER` | Trade with villagers |
| `ENCHANT_ITEM` | Enchant items |
| `ANVIL_USE` | Use anvils |
| `BREW_POTION` | Brew potions |
| `THROW_SNOWBALL_AT_PARTNER` | Throw snowballs at your partner |
| `THROW_EGG` | Throw eggs |
| `EAT_CAKE` | Eat cake together |
| `DRINK_MILK` | Drink milk |
| `LAUNCH_FIREWORK` | Launch fireworks |

#### 🟡 Medium Quests (Moderate Impact)
| Quest | Description |
|-------|-------------|
| `KILL_MOBS` | Kill mobs together |
| `CRAFT_ITEM` | Craft specific items |
| `PLACE_BLOCKS` | Place blocks |
| `HARVEST_CROPS` | Harvest crops |
| `TAME_ANIMAL` | Tame animals |
| `BREED_ANIMAL` | Breed animals |
| `SMELT_ITEMS` | Smelt items in a furnace |
| `SHOOT_ARROWS` | Shoot arrows |
| `SHEAR_SHEEP` | Shear sheep |
| `USE_ENDER_PEARL` | Use ender pearls |
| `KILL_WITH_BOW` | Kill mobs with a bow |
| `DAMAGE_EACH_OTHER` | Spar with your partner |
| `VISIT_NETHER` | Visit the Nether together |
| `RIDE_TOGETHER` | Ride a boat/minecart together |

#### 🔴 Heavy Quests (Optimized)
| Quest | Description |
|-------|-------------|
| `BREAK_BLOCKS` | Break natural blocks together |
| `PLAY_TOGETHER` | Play online together for a set duration |

#### 🏆 Bonus Quests (Rare + Higher XP)
| Quest | Default XP | Description |
|-------|------------|-------------|
| `KILL_BOSS` | 500 | Kill an Ender Dragon, Wither, or Elder Guardian |
| `MINE_ANCIENT_DEBRIS` | 300 | Mine ancient debris |
| `COMPLETE_RAID` | 400 | Complete a village raid together |
| `EARN_XP_LEVELS` | 250 | Earn XP levels |
| `MINE_DIAMOND_ORE` | 200 | Mine diamond ores |
| `MINE_DEEPSLATE_ORES` | 200 | Mine deepslate ores |
| `KILL_WITHER_SKELETONS` | 300 | Kill wither skeletons |
| `SMELT_NETHERITE` | 400 | Smelt netherite scrap |

</details>

## 🔨 Build

```bash
git clone https://github.com/revanjay/PartnershipRewards.git
cd PartnershipRewards
./gradlew build
```

Output JAR: `build/libs/PartnershipRewards-x.jar`

## 📁 Project Structure

```
src/main/java/github/revanjay/partnershiprewards/
├── PartnershipRewards.java      # Main plugin class
├── command/                      # Command handlers
├── database/                     # Database layer (HikariCP)
├── gui/                          # Inventory GUI
├── hook/                         # PlaceholderAPI integration
├── listener/                     # Event listeners
├── manager/                      # Business logic managers
├── model/                        # Data models
└── task/                         # Scheduled tasks
```

## 📦 Dependencies

| Library | Type | Description |
|---------|------|-------------|
| Paper API 1.20.1 | compileOnly | Server API |
| Lombok 1.18.46 | compileOnly | Annotation processor |
| PlaceholderAPI 2.12.2 | compileOnly (optional) | Placeholder support |
| HikariCP 5.0.1 | bundled | Connection pooling |
| SQLite JDBC 3.42.0.0 | bundled | SQLite driver |

---

Made by **RevelX**
