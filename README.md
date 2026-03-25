<div align="center">

# ⚔️ PartnershipRewards

[![Build](https://img.shields.io/badge/Build-Gradle-blue.svg)](https://gradle.org/)
[![Paper](https://img.shields.io/badge/Paper-1.20.x-blue.svg)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)


**Sistem partnership untuk Minecraft dengan quest-based leveling dan rewards.**

[Fitur](#-fitur) • [Instalasi](#-instalasi) • [Commands](#-commands) • [Permissions](#-permissions) • [Konfigurasi](#%EF%B8%8F-konfigurasi) • [Build](#-build)

</div>

---

## ✨ Fitur

- 💑 **Partnership System** — Ajak player lain menjadi partner dengan sistem request/accept
- 📜 **30+ Quest Types** — Dari quest ringan (makan, chat) sampai quest berat (bunuh boss, raid)
- ⬆️ **Leveling System** — XP & level progression dengan formula kustom per-level
- 🎁 **Milestone Rewards** — Reward otomatis berdasarkan durasi partnership dan level
- 🏆 **Bonus Quests** — Quest langka dengan XP reward lebih tinggi
- 💬 **Partner Chat** — Private chat khusus antar partner
- ⚔️ **PvP Toggle** — Toggle PvP protection antar partner
- 🕵️ **Admin Spy** — Admin bisa memantau partner chat
- 🖥️ **Level GUI** — GUI inventory untuk melihat progress level
- 📊 **Top Leaderboard** — Ranking partnership berdasarkan level
- 💾 **SQLite & MySQL** — Dual database support dengan HikariCP connection pooling
- ⚡ **Async Operations** — Semua database operations berjalan async, zero main-thread blocking

## 📋 Requirements

- **Paper** 1.20.x+
- **Java** 17+

## 📥 Instalasi

1. Download JAR dari [Releases](https://github.com/revanjay/PartnershipRewards/releases)
2. Taruh file `.jar` di folder `plugins/`
3. Restart server
4. Edit `plugins/PartnershipRewards/config.yml`
5. Jalankan `/partneradmin reload`

## 📝 Commands

### Player Commands

| Command | Deskripsi |
|---------|-----------|
| `/partner request <player>` | Kirim permintaan partnership |
| `/partner accept` | Terima permintaan |
| `/partner reject` | Tolak permintaan |
| `/partner break` | Putuskan partnership |
| `/partner info` | Lihat info partnership |
| `/partner quest` | Lihat quest aktif |
| `/partner level` | Buka GUI level progress |
| `/partner chat <pesan>` | Kirim pesan ke partner |
| `/partner toggle pvp` | Toggle PvP antar partner |
| `/partner top` | Lihat top 10 partnership |

### Admin Commands

| Command | Deskripsi |
|---------|-----------|
| `/partneradmin reload` | Reload config |
| `/partneradmin reset <player>` | Reset partnership player |
| `/partneradmin set <p1> <p2>` | Buat partnership paksa |
| `/partneradmin toggle spy` | Toggle spy mode partner chat |

## 🔑 Permissions

| Permission | Deskripsi | Default |
|-----------|-----------|---------|
| `partnershiprewards.use` | Gunakan partner commands | `true` |
| `partnershiprewards.admin` | Admin commands | `op` |

## ⚙️ Konfigurasi

<details>
<summary>Klik untuk melihat config.yml</summary>

File konfigurasi lengkap akan di-generate otomatis saat pertama kali plugin dijalankan di `plugins/PartnershipRewards/config.yml`.

Konfigurasi mencakup:
- **Database** — Pilihan SQLite (default) atau MySQL
- **Quest Types** — 30+ tipe quest yang bisa di-enable/disable
- **Quest Settings** — XP per quest, reset hours, cooldown
- **Level Rewards** — Reward kustom per level (commands + broadcast)
- **Messages** — Semua pesan bisa dikustomisasi
- **Bonus Quests** — Quest langka dengan chance% dan XP reward kustom

</details>

## 🔨 Build

```bash
git clone https://github.com/revanjay/PartnershipRewards.git
cd PartnershipRewards
./gradlew build
```

Output JAR: `build/libs/PartnershipRewards-<version>.jar`

## 📁 Struktur Project

```
src/main/java/github/revanjay/partnershiprewards/
├── PartnershipRewards.java      # Main plugin class
├── command/                      # Command handlers
├── database/                     # Database layer (HikariCP)
├── gui/                          # Inventory GUI
├── listener/                     # Event listeners
├── manager/                      # Business logic
├── model/                        # Data models
└── task/                         # Scheduled tasks
```

---

Made by **RevelX**
