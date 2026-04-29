<div align="center">

# ⚔️ PartnershipRewards

[![Build](https://img.shields.io/badge/Build-Gradle-blue.svg)](https://gradle.org/)
[![Paper](https://img.shields.io/badge/Paper-1.20.x-blue.svg)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/Version-1.2.0-green.svg)](#)


**Sistem partnership untuk Minecraft dengan quest-based leveling, gifts, home, dan rewards.**

[Fitur](#-fitur) • [Instalasi](#-instalasi) • [Commands](#-commands) • [Permissions](#-permissions) • [Placeholders](#-placeholders) • [Konfigurasi](#%EF%B8%8F-konfigurasi) • [Build](#-build)

</div>

---

## ✨ Fitur

- 💑 **Partnership System** — Ajak player lain menjadi partner dengan sistem request/accept
- 📜 **39 Quest Types** — Dari quest ringan (makan, chat) sampai quest berat (bunuh boss, raid)
- ⬆️ **Leveling System** — XP & level progression dengan formula kustom per-level
- 🎁 **Gift System** — Kirim item ke partner, claim gifts dengan sistem pending
- 🏠 **Partner Home** — Set, teleport, dan hapus home bersama (warmup & cooldown)
- 🎁 **Milestone Rewards** — Reward otomatis berdasarkan durasi partnership dan level
- 🏆 **Bonus Quests** — Quest langka dengan XP reward lebih tinggi
- 💬 **Partner Chat** — Private chat khusus antar partner (toggle mode & direct message)
- ⚔️ **PvP Toggle** — Toggle PvP protection antar partner
- ✨ **Partner Effects** — Particle effects otomatis berdasarkan level (Heart, Happy, EndRod, Cherry)
- 🔥 **Login Streak** — Daily login streak bersama partner untuk bonus XP
- 🏷️ **Partner Titles** — Title otomatis berdasarkan level via PlaceholderAPI
- 🕵️ **Admin Spy** — Admin bisa memantau partner chat
- 🖥️ **Level GUI** — GUI inventory untuk melihat progress level
- 📊 **Top Leaderboard** — Ranking partnership berdasarkan level
- 🔗 **PlaceholderAPI Support** — 10 placeholders untuk scoreboard, chat, dll
- 💾 **SQLite & MySQL** — Dual database support dengan HikariCP connection pooling
- ⚡ **Async Operations** — Semua database operations berjalan async, zero main-thread blocking

## 📋 Requirements

- **Paper** 1.20.x+
- **Java** 21+

### Optional Dependencies

- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) — Untuk placeholders & partner titles

## 📥 Instalasi

1. Download JAR dari [Releases](https://github.com/revanjay/PartnershipRewards/releases)
2. Taruh file `.jar` di folder `plugins/`
3. Restart server
4. Edit `plugins/PartnershipRewards/config.yml`
5. Jalankan `/partneradmin reload`

## 📝 Commands

### Player Commands

| Command | Alias | Deskripsi |
|---------|-------|-----------|
| `/partner request <player>` | | Kirim permintaan partnership |
| `/partner accept` | | Terima permintaan |
| `/partner reject` | | Tolak permintaan |
| `/partner break` | | Putuskan partnership |
| `/partner info` | | Lihat info partnership |
| `/partner quest` | | Lihat quest aktif |
| `/partner level` | `/partner gui` | Buka GUI level progress |
| `/partner chat [pesan]` | | Toggle chat mode / kirim pesan ke partner |
| `/partner gift` | | Kirim item yang dipegang ke partner |
| `/partner gifts` | | Claim gifts dari partner |
| `/partner sethome` | | Set partner home (min level required) |
| `/partner home` | | Teleport ke partner home |
| `/partner delhome` | | Hapus partner home |
| `/partner toggle pvp` | | Toggle PvP antar partner |
| `/partner toggle effects` | | Toggle particle effects |
| `/partner top` | | Lihat top 10 partnership |
| `/partner list` | | Lihat semua partnerships (admin only) |

> **Tip:** `/partner` juga bisa diakses via `/p` atau `/partnership`

### Admin Commands

| Command | Deskripsi |
|---------|-----------|
| `/partneradmin reload` | Reload config |
| `/partneradmin reset <player>` | Reset partnership player |
| `/partneradmin set <p1> <p2>` | Buat partnership paksa |
| `/partneradmin toggle spy` | Toggle spy mode partner chat |

> **Tip:** `/partneradmin` juga bisa diakses via `/pa` atau `/padmin`

## 🔑 Permissions

| Permission | Deskripsi | Default |
|------------|-----------|---------|
| `partnershiprewards.use` | Gunakan partner commands | `true` |
| `partnershiprewards.admin` | Admin commands + `/partner list` | `op` |

## 🔗 Placeholders

Memerlukan [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/). Semua placeholder menggunakan prefix `%partner_`.

| Placeholder | Output | Contoh |
|-------------|--------|--------|
| `%partner_name%` | Nama partner | `Steve` |
| `%partner_level%` | Level partnership | `15` |
| `%partner_xp%` | XP saat ini | `450` |
| `%partner_duration%` | Durasi partnership | `7d 3h` |
| `%partner_days%` | Jumlah hari partnership | `7` |
| `%partner_online%` | Status partner | `Online` / `Offline` |
| `%partner_title%` | Title berdasarkan level | `[Soulmate]` |
| `%partner_quest%` | Deskripsi quest aktif | `Kill 20 Zombies` |
| `%partner_streak%` | Login streak saat ini | `5` |
| `%partner_has_partner%` | Punya partner atau tidak | `true` / `false` |

## ⚙️ Konfigurasi

<details>
<summary>Klik untuk melihat config sections</summary>

File konfigurasi lengkap akan di-generate otomatis saat pertama kali plugin dijalankan di `plugins/PartnershipRewards/config.yml`.

Konfigurasi mencakup:

### Database
- Pilihan **SQLite** (default) atau **MySQL**
- HikariCP connection pool settings

### Quest System
- **39 tipe quest** yang bisa di-enable/disable per-tipe
- XP per quest, reset hours, cooldown
- Custom XP requirement per level (override formula)
- Max level configuration
- Bonus quests dengan custom chance% dan XP reward

### Level Rewards
- Reward kustom per level (commands + broadcast)

### Gift System
- Max pending gifts per player

### Partner Effects
- Particle effects berdasarkan level (Heart ≥1, Happy ≥5, EndRod ≥15, Cherry ≥25)
- Interval ticks & max distance
- Toggle per-partnership

### Partner Titles
- Title otomatis berdasarkan level via PlaceholderAPI
- Custom format & per-level title names

### Partner Home
- Minimum level untuk set home
- Warmup seconds (harus diam sebelum teleport)
- Cooldown seconds antar teleport

### Login Streak
- Base bonus XP per streak level
- Max streak (default 7 hari)
- Custom messages

### Messages
- Semua pesan bisa dikustomisasi (prefix, requests, errors, quests, progress)

</details>

## 🎯 Quest Types

<details>
<summary>Klik untuk melihat semua 39 quest types</summary>

#### 🟢 Light Quests (Low Impact)
| Quest | Deskripsi |
|-------|-----------|
| `GIVE_ITEM` | Berikan item ke partner |
| `SEND_MESSAGE` | Kirim pesan di chat |
| `USE_COMMAND` | Gunakan command tertentu |
| `EAT_FOOD` | Makan bersama |
| `SLEEP_TOGETHER` | Tidur bersama partner |
| `FISH_CATCH` | Memancing ikan |
| `TRADE_VILLAGER` | Trading dengan villager |
| `ENCHANT_ITEM` | Enchant item |
| `ANVIL_USE` | Gunakan anvil |
| `BREW_POTION` | Brew potion |
| `THROW_SNOWBALL_AT_PARTNER` | Lempar snowball ke partner |
| `THROW_EGG` | Lempar telur |
| `EAT_CAKE` | Makan kue bersama |
| `DRINK_MILK` | Minum susu |
| `LAUNCH_FIREWORK` | Luncurkan kembang api |

#### 🟡 Medium Quests (Moderate Impact)
| Quest | Deskripsi |
|-------|-----------|
| `KILL_MOBS` | Bunuh mob bersama partner |
| `CRAFT_ITEM` | Craft item tertentu |
| `PLACE_BLOCKS` | Pasang block |
| `HARVEST_CROPS` | Panen tanaman |
| `TAME_ANIMAL` | Jinakkan hewan |
| `BREED_ANIMAL` | Breeding hewan |
| `SMELT_ITEMS` | Smelt di furnace |
| `SHOOT_ARROWS` | Tembak panah |
| `SHEAR_SHEEP` | Cukur domba |
| `USE_ENDER_PEARL` | Gunakan ender pearl |
| `KILL_WITH_BOW` | Kill mob dengan bow |
| `DAMAGE_EACH_OTHER` | Sparring dengan partner |
| `VISIT_NETHER` | Kunjungi Nether bersama |
| `RIDE_TOGETHER` | Naik boat/minecart bersama |

#### 🔴 Heavy Quests (Optimized)
| Quest | Deskripsi |
|-------|-----------|
| `BREAK_BLOCKS` | Hancurkan block bersama |
| `PLAY_TOGETHER` | Main bersama partner (waktu) |

#### 🏆 Bonus Quests (Rare + Higher XP)
| Quest | Default XP | Deskripsi |
|-------|-----------|-----------|
| `KILL_BOSS` | 500 | Kill Ender Dragon/Wither/Elder Guardian |
| `MINE_ANCIENT_DEBRIS` | 300 | Tambang ancient debris |
| `COMPLETE_RAID` | 400 | Selesaikan raid bersama |
| `EARN_XP_LEVELS` | 250 | Naik XP level |
| `MINE_DIAMOND_ORE` | 200 | Tambang diamond ore |
| `MINE_DEEPSLATE_ORES` | 200 | Tambang deepslate ore |
| `KILL_WITHER_SKELETONS` | 300 | Kill wither skeleton |
| `SMELT_NETHERITE` | 400 | Smelt netherite scrap |

</details>

## 🔨 Build

```bash
git clone https://github.com/revanjay/PartnershipRewards.git
cd PartnershipRewards
./gradlew build
```

Output JAR: `build/libs/PartnershipRewards-1.2.0.jar`

## 📁 Struktur Project

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

| Library | Tipe | Deskripsi |
|---------|------|-----------|
| Paper API 1.20.1 | compileOnly | Server API |
| Lombok 1.18.38 | compileOnly | Annotation processor |
| PlaceholderAPI 2.11.6 | compileOnly (optional) | Placeholder support |
| HikariCP 5.0.1 | bundled | Connection pooling |
| SQLite JDBC 3.42.0.0 | bundled | SQLite driver |

---

Made by **RevelX**
