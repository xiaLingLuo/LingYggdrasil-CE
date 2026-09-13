<h1 align="center">Ling Yggdrasil</h1>

<p align="center">
  <em>A lightweight, secure, and elegant external Yggdrasil authentication system for Minecraft</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-2.0.0-blueviolet?style=flat-square" alt="Version" />
  <img src="https://img.shields.io/badge/Java-25-orange?style=flat-square" alt="Java" />
  <a href="https://www.gnu.org/licenses/agpl-3.0">
    <img src="https://img.shields.io/badge/License-AGPL--3.0-blue.svg?style=flat-square" alt="License: AGPL-3.0" />
  </a>
</p>

<p align="center">
  <a href="README.md">中文</a> | <a href="README_EN.md">English</a>
</p>

<p align="center">
  <img src="img/user-panel.png" alt="User Panel" width="820" />
</p>

---

## Introduction

**Ling Yggdrasil** is an **external Yggdrasil authentication system** built for Minecraft. It is fully compatible with the authlib ecosystem and provides account registration, login, character management, and skin/cape hosting. With a built-in web installation wizard, an anime-style control panel, and a multi-layered security system, you can stand up a secure and good-looking external login service in minutes.

> **Live demo**: [multimc.cn](https://multimc.cn/) — a site running the latest stable release. Try it at zero cost before you deploy your own.

<p align="center">
  <img src="img/player-card.png" alt="Character Editing" width="820" />
</p>

---

## Quick Start

**Requirements**

| Item | Requirement |
| --- | --- |
| Java | **Java 25** (recommended; Java 21+ is theoretically supported but only Java 25 is tested) |
| Memory | At least 512 MB available is recommended |
| Disk | Depends on texture count; 1 GB+ is recommended for SQLite mode |

**Three steps**

```bash
# 1. Check the Java version
java --version

# 2. Download LingYggdrasil-2.0.0.jar and put it in an empty, dedicated directory

# 3. Start it (this directory becomes the data directory)
java -Xms512M -Xmx2G -jar LingYggdrasil-2.0.0.jar
```

On first launch the program enters the **installation wizard** (`http://<server>:35598`). Follow the prompts to configure the admin account, database, and email. Once installed, three services are available:

| Port | Purpose | Exposure |
| --- | --- | --- |
| `35565` | User dashboard (registration, login, skins, friends, etc.) | Public |
| `35577` | Yggdrasil API (for Minecraft launchers) | Public |
| `35599` | Admin panel | **Keep private** |
| `35598` | Installation wizard | First install only |

See **[Getting Started](docs/getting-started.md)** for the full walkthrough and **[Deployment](docs/deployment.md)** for production setups.

> **Security note**: The admin panel (35599) and install port (35598) should never be exposed directly to the internet. Restrict them with a reverse proxy, or access them only over a private network / SSH tunnel.

---

## Features

### Out of the Box

- **Web Installation Wizard** — Guides you through the admin account, database, and email setup on first launch with a fully visual interface.
- **Multi-Database Support** — **SQLite**, **MySQL**, and **PostgreSQL**, chosen as needed with no extra configuration.
- **Single JAR Deployment** — Packaged as one executable JAR. Drop it on your server and run.

### Security

- **Argon2 Password Hashing** — All passwords are hashed with Argon2, with 6 adjustable strength levels.
- **Email Verification** — Optional verification-code activation after registration, with domain whitelist/blacklist control.
- **Same-IP Registration Limit** — Caps the number of accounts registrable from a single IP to deter bulk sign-ups.
- **Name Blacklists** — Configurable blacklists for usernames, character names, and skin/cape names, with wildcard support.
- **Isolated Sessions** — The user site, admin panel, and API use mutually isolated session cookies.
- **Root Integrity Guard** — Validates the `root_info` table to prevent bypassing Root checks by tampering with the database.
- **Rate Limiting** — Login, registration, texture uploads, and log downloads are all rate limited.

### User Features

- **Character Management** — Create multiple characters, each with its own Yggdrasil Token and UUID; profile cards show face-extraction previews.
- **Skins & Capes** — Upload and manage personal skins and capes with aliases and public/private visibility.
- **Material Library (World Tree)** — A public gallery sorted by popularity (likes + favorites × 5), with infinite scroll and lazy loading.
- **Likes & Favorites** — Like textures to boost them, and favorite them into your personal shared library.
- **Shared Materials** — Friend-shared and My Favorites sections, with one-click apply to a character.
- **Friend System** — Add friends by friend code, friend detail cards, block management, and checkbox-based texture sharing.
- **3D Skin Preview** — Powered by skinview3d, with 360° rotation in detail modals.
- **Security Settings** — Self-service password, email, and nickname changes, plus Token viewing and regeneration.
- **Action Logs** — View, download, and clear your own action logs.
- **Theme & Language** — Light/dark theme toggle and 10 languages, persisted to your account.

### Admin Panel

- **Dashboard** — Core metrics such as users, characters, and skins/capes at a glance, with drag-and-drop widgets.
- **User Management** — View, search, create, edit, and ban users; manage email verification and user permission groups.
- **Character Management** — Global CRUD with name changes, ownership transfer, and model switching.
- **Skin & Cape Management** — Global texture management with upload size, quantity, storage path, and rate-limit settings.
- **Security Settings** — Visual cards for the 6 encryption levels, adjusting Argon2 parameters on demand.
- **System Management** — Grouped settings for site info, feature toggles, domains, announcements, ICP filing, and action logs, each card saved independently.
- **Permission Groups** — Separate admin and user permission groups for fine-grained menu and feature control.
- **World Tree Settings** — Signature algorithm, Token lifetimes, rate limits, UUID version, and other protocol settings.

### Yggdrasil Protocol

- **Complete Implementation** — Compatible with Yggdrasil authentication for mainstream Minecraft launchers.
- **Three Signature Modes** — **Ed448** (modern), **RSA-SHA512**, and **RSA-SHA1** (compatibility), switchable on demand.
- **Token System** — A 64-character high-strength Token is generated for every character as the game login credential.
- **Texture Hosting** — Publicly exposes `/textures/{type}/{hash}` so game clients can load skins and capes directly.
- **Session Management** — Complete join, logout, and session validation flows.
- **Lookup Extensions** — Convenience endpoints for single and batch lookups.

### Internationalization

Ten built-in languages: Simplified Chinese, Traditional Chinese, Chinese (Huaxia), English, Русский, Deutsch, Français, Italiano, 日本語, and 한국어. Language packs are JSON and can be overridden and deep-merged externally without touching the program.

---

## Documentation

| Document | Description |
| --- | --- |
| [Getting Started](docs/getting-started.md) | Requirements, download, first launch, and installation wizard |
| [Deployment & Operations](docs/deployment.md) | Port planning, reverse proxy, data directory, backups, systemd |
| [Configuration Reference](docs/configuration.md) | Complete reference for `sql.yml` and all system settings |
| [Yggdrasil API](docs/yggdrasil-api.md) | Protocol reference for authserver, sessionserver, and texture endpoints |
| [Admin Guide](docs/admin-guide.md) | Features and operations of each admin module |
| [User Guide](docs/user-guide.md) | Registration, login, characters, skins, friends, and more |
| [Permissions](docs/permissions.md) | Admin and user permission groups |
| [Customization](docs/customization.md) | Icon overrides, home page content, languages, email templates |
| [Security](docs/security.md) | Cryptography, session isolation, known limitations, and notes |
| [Development](docs/development.md) | Source layout, build, and extending the project |
| [FAQ](docs/faq.md) | Frequently asked questions and troubleshooting |

Full documentation index: **[docs/README.md](docs/README.md)**.

---

## Client Setup

Using a launcher that supports custom Yggdrasil (e.g. HMCL):

1. Open the launcher's account or login settings and choose **Custom Yggdrasil / External Login**.
2. Set the authentication server to the **API root** (e.g. `https://example.com`, or `http://<IP>:35577` when connecting directly).
3. Enter the **character name** as the username and that character's **Yggdrasil Token** as the password.

> See the [User Guide](docs/user-guide.md#角色管理) for creating characters and obtaining Tokens, and the [Yggdrasil API](docs/yggdrasil-api.md) for protocol details.

---

## Roadmap

Done:

- [x] Friend system
- [x] Material library with private / public / friend-shared / specific-friend sharing
- [x] Likes and favorites with popularity sorting
- [x] Grouped saving for admin settings
- [x] Admin and user permission groups
- [x] Full internationalization and dark theme
- [x] Email-style username support for Yggdrasil login

Planned:

- [ ] Richer account info cards, e.g. last login time and IP
- [ ] More granular user permission controls
- [ ] Additional languages and theme styles

---

## Tech Stack

| Component | Technology |
| --- | --- |
| Language | Java 25 |
| Web Framework | [Javalin](https://javalin.io/) |
| Database Connection | [HikariCP](https://github.com/brettwooldridge/HikariCP) pool |
| Database | SQLite / MySQL / PostgreSQL |
| Cryptography | [Bouncy Castle](https://www.bouncycastle.org/) (Argon2, Ed448, RSA) |
| Email | [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) |
| Logging | [Logback](https://logback.qos.ch/) + SLF4J |
| Serialization | Jackson |
| Build Tool | Maven |

---

## Contributing

Issues and pull requests are welcome. Please read [Development](docs/development.md) first for the source layout and conventions.

## License

Licensed under the [GNU Affero General Public License v3.0](LICENSE).
