<h1 align="center">LingYggdrasil</h1>

<p align="center">
  <em>A lightweight, secure and elegant external Yggdrasil authentication system for Minecraft</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-2.2.2-blueviolet?style=flat-square" alt="Version" />
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

**LingYggdrasil** is an **external Yggdrasil authentication system** built for Minecraft. It is fully compatible with the authlib ecosystem and provides account registration, login, character management, and skin/cape hosting. With a built-in web installation wizard, an anime-style control panel, and a multi-layered security system, you can stand up a secure and good-looking external login service in just a few minutes.

> **Online preview**: [multimc.cn](https://multimc.cn/) — a site running the latest stable release. You can try it here at zero cost before deploying it yourself.

<p align="center">
  <img src="img/player-card.png" alt="Character Editing" width="820" />
</p>

---

## Quick Start

How many steps does it take to put an elephant into a fridge? Getting LingYggdrasil running is about as simple.

**Requirements**

| Item   | Requirement                                                                    |
|--------|--------------------------------------------------------------------------------|
| Java   | **Java 25** (recommended; Java 21+ should work in theory, only Java 25 is tested) |
| Memory | At least 512 MB of available memory recommended                                |
| Disk   | Depends on the number of textures                                              |

**Three-step launch**

```bash
# 1. Confirm the Java version
java --version

# 2. Download LingYggdrasil-2.2.2.jar and put it in a standalone empty directory

# 3. Launch (this directory becomes the data directory)
java -Xms512M -Xmx2G -jar LingYggdrasil-2.2.2.jar
```

On first launch it automatically enters the **installation wizard** (`http://<server-address>:35598`). Just follow the prompts to set up the administrator account, database, and mail configuration. After installation, the program provides three services at the same time:

| Port    | Purpose                                                          | Access          |
|---------|------------------------------------------------------------------|-----------------|
| `35565` | User portal (registration, login, dashboard, skins, friends, …)  | Public          |
| `35577` | Yggdrasil API (called by Minecraft launchers)                    | Public          |
| `35599` | Admin panel                                                      | **Keep private** |
| `35598` | Installation wizard                                              | First install only |

See the **[Getting Started guide](docs/01-overview/getting-started.md)** for the full walkthrough, and **[Deployment & Operations](docs/01-overview/deployment.md)** for production setups.

> **Security note**: the admin panel (35599) and the installation port (35598) should not be exposed directly to the public internet. It is recommended to restrict access via a reverse proxy, or to reach them only over an intranet / SSH tunnel.

---

## Features

### Ready to Use

- **Web installation wizard** — guides you through the administrator account, database, and mail service on first launch, entirely through a visual interface.
- **Multi-database support** — supports **SQLite**, **MySQL**, and **PostgreSQL**, choose as needed with no extra configuration.
- **Single JAR deployment** — packaged as a single executable JAR; drop it on the server and run.

### Plugin System

- **External plugin loading** — scans the `plugins/` directory and loads plugin jars on startup; nothing is loaded in installation mode.
- **`plugin.yml` descriptor** — Paper-style, declaring name, version, authors, dependencies, icon, and permission nodes.
- **Admin plugin management** — a top-level "Plugin Management" menu whose overview shows icon, name, version, authors, description, and running state, with manual start/stop.
- **Plugin secondary menus** — a plugin may register at most one secondary menu in the admin panel to host its own controls.
- **Dynamic permission nodes** — built-in and plugin nodes are registered dynamically by source (`LingYggdrasil` / each plugin), and the permission group page groups them by source.
- **Plugin development API** — the build also produces `LingYggdrasil-plugin-api-2.2.2.jar`, self-contained, for plugin developers to compile against.

> Plugins are **fully trusted code**. Do not load plugins from untrusted sources; you bear the consequences of loading third-party plugins. See [Plugin System](docs/05-plugins/management.md).

### Security

- **Argon2 password hashing** — all passwords are stored using Argon2 hashes, with 6 adjustable strength levels.
- **Email verification** — accounts can be activated via email verification codes after registration, with domain allow/deny list control.
- **Same-IP registration limit** — limits how many accounts can be registered from a single IP to prevent bulk registration.
- **Name blacklists** — usernames, character names, and skin/cape names can all be blacklisted, with wildcard support.
- **Isolated session system** — the user portal, admin panel, and API use mutually isolated session cookies.
- **Root integrity protection** — validates the `root_info` table to prevent bypassing Root checks by tampering with the database directly.
- **Login and operation rate limiting** — key operations such as login, registration, texture upload, and log download are rate limited.

### User Portal

- **Character management** — create multiple game characters, each with its own Yggdrasil Token and UUID; character cards show a face-crop preview.
- **Skins & capes** — upload and manage personal skins and capes, with alias naming and controllable public/private visibility.
- **Texture library** — a public texture plaza sorted by popularity (likes + favorites × 5), with infinite-scroll lazy loading.
- **Likes & favorites** — like textures to boost their popularity, and favorite them into your personal shared texture library.
- **Shared textures** — two sections, "Shared by Friends" and "My Favorites", with one-click application of a texture to a character.
- **Friend system** — add friends by friend code, friend detail cards, blocklist management, and checkbox-based texture sharing.
- **3D skin preview** — integrated with skinview3d; the detail dialog supports 360° rotation preview.
- **Security settings** — self-service changes to password, email, and nickname, and viewing/regenerating Tokens.
- **Action logs** — view, download, and clear your own action logs.
- **Theme & language** — light/dark theme switching, with 10 languages persisted per account.

<p align="center">
  <img src="img/user-manager.png" alt="User Interface" width="820" />
</p>

### Admin Panel

- **Dashboard overview** — core data such as user count, character count, and skin/cape counts at a glance, with drag-to-reorder widgets.
- **User management** — view, search, create, edit, and ban users, and manage email verification status and user permission groups.
- **Character management** — global CRUD, with name changes, ownership transfer, and form switching.
- **Skin & cape management** — globally manage texture resources, and configure upload size, count, storage path, and rate limits.
- **Security settings** — a visual card for the 6 encryption levels, adjusting Argon2 parameters as needed.
- **System management** — grouped configuration for site info, feature toggles, domains, announcements, filings, action logs, and more; each card saves independently.
- **Permission groups** — admin permission groups and user permission groups, giving fine-grained control over menu visibility and feature availability.
- **Yggdrasil settings** — protocol configuration such as signature algorithm, Token validity, rate limits, and UUID version.

<p align="center">
  <img src="img/admin-page.png" alt="Admin Interface" width="820" />
</p>

### Yggdrasil Protocol

- **Complete protocol implementation** — compatible with the Yggdrasil authentication used by mainstream Minecraft launchers.
- **Three signature modes** — **Ed448** (modern), **RSA-SHA512**, and **RSA-SHA1** (compatible), switchable as needed.
- **Token system** — each character automatically generates a 64-character high-strength Token as the game login credential.
- **Texture hosting** — publicly exposes the `/textures/{type}/{hash}` endpoint, so game clients can access skins and capes directly.
- **Session management** — a complete join, leave, and session validation flow.
- **Convenience query extensions** — provides name-based lookup, batch lookup, and other extended endpoints.

### Internationalization

Ten built-in languages: Simplified Chinese, Traditional Chinese, Chinese (Huaxia), English, Русский, Deutsch, Français, Italiano, 日本語, and 한국어. Language packs are JSON and support external overrides with deep merging, so you can adjust the text without changing the program.

---

## Documentation

The full documentation is organized into **seven sections**. Index: **[docs/README.md](docs/README.md)**.

| Section | Description |
|---------|-------------|
| [① Overview](docs/01-overview/README.md) | Project introduction, quick start, deployment & operations, configuration reference, development & build, FAQ |
| [② User System](docs/02-user/README.md) | User features, user permission system, user UI customization |
| [③ Admin System](docs/03-admin/README.md) | Admin panel, admin permission system, admin UI customization |
| [④ Yggdrasil System](docs/04-yggdrasil/README.md) | Yggdrasil authentication, session, and texture protocol |
| [⑤ Plugin System](docs/05-plugins/README.md) | Installing, viewing, and toggling plugins |
| [⑥ Security](docs/06-security/README.md) | Cryptography, session isolation, request protection, and security checklist |
| [⑦ Plugin Development](docs/07-plugin-dev/README.md) | `plugin.yml`, plugin API, routes, and examples |

---

## Client Setup

Using a launcher that supports custom Yggdrasil (e.g. HMCL) as an example:

1. In the launcher's "Accounts" or "Login method" settings, choose "Custom Yggdrasil / External Login".
2. Set the authentication server address to the **API root** (e.g. `https://example.com`, or `http://<IP>:35577` when connecting directly).
3. Enter the **character name** as the username, and that character's **Yggdrasil Token** as the password.

> For how to create characters and obtain Tokens, see the [User Guide](docs/02-user/guide.md#角色管理); for protocol details, see the [Yggdrasil API](docs/04-yggdrasil/api.md).

---

## Tech Stack

| Component       | Technology                                                                 |
|-----------------|----------------------------------------------------------------------------|
| Language        | Java 25                                                                    |
| Web framework   | [Javalin](https://javalin.io/)                                             |
| DB connection   | [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pool    |
| Database        | SQLite / MySQL / PostgreSQL                                                |
| Cryptography    | [Bouncy Castle](https://www.bouncycastle.org/) (Argon2, Ed448, RSA)        |
| Mail            | [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/)           |
| Logging         | [Logback](https://logback.qos.ch/) + SLF4J                                 |
| Serialization   | Jackson                                                                    |
| Build tool      | Maven                                                                      |

---

## Contributing

Issues and pull requests are welcome. Before you start, please read [Development & Build](docs/01-overview/development.md) to learn about the source layout and conventions.

### Internationalization

We ship 10 built-in languages. Except for Simplified Chinese, all of them are currently AI-translated and not yet human-reviewed. The table below shows the translation progress of each language — **internationalization PRs are very welcome!**

| Language | AI Translation | Human Translation |
|----------|----------------|-------------------|
| Simplified Chinese (zh-CN)      | Native | Native |
| Traditional Chinese (zh-TW)     | 100%   | 0%     |
| Chinese (Huaxia) (zh-XIA)       | 100%   | 0%     |
| English (en-US)                 | 100%   | 0%     |
| Русский (ru-RU)                 | 100%   | 0%     |
| Deutsch (de-DE)                 | 100%   | 0%     |
| Français (fr-FR)                | 100%   | 0%     |
| Italiano (it-IT)                | 100%   | 0%     |
| 日本語 (ja-JP)                  | 100%   | 0%     |
| 한국어 (ko-KR)                  | 100%   | 0%     |

Language packs live in `src/main/resources/i18n/`, split into `user/` (user portal) and `admin/` (admin panel) JSON sets; home page templates are under `i18n/user/index-page/`. When submitting a PR, please update all keys for the target language. See [Interface Customization](docs/02-user/customization.md).

## License

Licensed under the [GNU Affero General Public License v3.0](LICENSE).
