<h1 align="center">Ling Yggdrasil</h1>

<p align="center">
  <em>A lightweight, secure, and elegant external Yggdrasil authentication system for Minecraft</em>
</p>

<p align="center">
  <img src="img/user-panel.png" alt="User Panel" width="800" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-orange?style=flat-square" alt="Java" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="MIT License" />
</p>

<p align="center">
  <a href="README.md">中文</a> | <a href="README_EN.md">English</a>
</p>

---

## Introduction

**Ling Yggdrasil** is an **external Yggdrasil authentication system** built for Minecraft. It is fully compatible with the `authlib.jar` ecosystem, providing comprehensive account registration, login, character management, and skin/cape hosting capabilities. With an out-of-the-box web installation wizard, an anime-style control panel, and a multi-layered encryption system, it makes your external login system both secure and visually appealing.

> **🌐 [Live Demo](https://multimc.cn/)** — This is a site running the latest stable version of LingYggdrasil. You can experience it instantly at zero cost before deploying it yourself.

<p align="center">
  <img src="img/player-card.png" alt="Character Editing" width="800" />
</p>

---

## Features

### 🚀 Out-of-the-Box

- **Web Installation Wizard** — Automatically enters the installation page on first launch, guiding you through setting up the admin account, database, and email service with a fully visual interface.
- **Multi-Database Support** — Supports **SQLite**, **MySQL**, and **PostgreSQL**. Choose as needed without extra configuration.
- **Single JAR Deployment** — Packaged as a single executable JAR file. Just drop it on your server and run.

### 🎨 Exquisite User Interface

- **Anime-Style Design** — Pink and white color scheme, rounded card layouts, and delicate CSS animations.
- **Responsive Layout** — Fully adapted for both desktop and mobile devices.
- **3D Character Preview** — Integrated with skinview3d for real-time skin and cape previews during character editing.

### 🔐 Security System

- **Argon2 Password Encryption** — All user passwords are encrypted and stored using the Argon2 algorithm, with 6 adjustable encryption strength levels.
- **Login Rate Limiting** — Prevents brute-force attacks by automatically limiting login attempt frequencies.
- **Email Verification** — Requires email verification code activation after registration, supporting domain whitelist/blacklist control.
- **Same-IP Registration Limit** — Restricts the number of accounts that can be registered from the same IP to prevent bulk registrations.
- **Username Blacklist** — Administrators can configure a list of prohibited usernames for registration.
- **Independent Session Management** — Administrators and users have completely independent session systems, ensuring no interference.

### 👤 User Features

- **Character Management** — Create multiple game characters, each with its own independent Yggdrasil Token.
- **Skins & Capes** — Upload and manage personal skin and cape resources, supporting alias naming.
- **Security Settings** — Self-manage password changes, email verification, and Token viewing/regeneration.
- **Dashboard Guide** — Built-in quick start guide, making it easy for new users to get started.

### 🛡️ Admin Dashboard

- **Dashboard Overview** — Core data such as user count, character count, and skin/cape count at a glance.
- **User Management** — View, edit, and ban users, with support for email verification status management.
- **Character Management** — Global CRUD operations, supporting character name changes and ownership transfers.
- **Skin & Cape Management** — Global management of all texture resources, configuring upload size, quantity, storage paths, and rate limits.
- **Security Settings** — Visual card display of 6 encryption levels, allowing on-demand adjustment of Argon2 parameters.
- **Email Domain Control** — Whitelist/blacklist mode for precise control over registrable email domains.
- **System Settings** — Global configurations such as site name, ICP filing number, and registration toggle.
- **Yggdrasil Settings** — Yggdrasil protocol-specific configurations, including signature algorithms, Token expiration, and rate limits.

### 🌳 Yggdrasil Protocol

- **Complete Protocol Implementation** — Compatible with Yggdrasil authentication integration for mainstream Minecraft launchers.
- **Dual Signature Mode** — Supports **Ed448** (modern mode) and **RSA-SHA512** (compatibility mode), switchable as needed.
- **Token System** — Automatically generates a 64-character high-strength Token for each character as the game login credential.
- **Texture Hosting** — Publicly exposes the `/textures/{type}/{hash}` endpoint for direct skin and cape access by game clients.
- **Session Management** — Complete login/logout and session validation workflows.

---

## Installation Guide

How many steps does it take to put an elephant in a refrigerator? Starting LingYggdrasil is just about as simple.

### 1. Check Java Environment

First, run the following command in your server or local terminal:

```bash
java --version
```

Please ensure that Java 25 is installed.

> Theoretically, this program supports Java 21 and above; however, it has currently only been tested in a Java 25 environment.
> If you encounter issues running it in a non-Java 25 environment, it is recommended to switch to Java 25.

### 2. Install Java 25

If Java 25 is already installed on your system, you can skip this step.

#### Debian / Ubuntu and other Linux distributions

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install openjdk-25-jre -y
```

#### Red Hat / CentOS / Fedora and other Linux distributions

Choose one of the following commands based on your system:

```bash
sudo dnf install java-25-openjdk -y
```

Or:

```bash
sudo yum install java-25-openjdk -y
```

#### Windows

Please search for and download the Java 25 installation package in your browser, which is usually an `.exe` or `.msi` file.

Once downloaded, double-click the installer and follow the installation wizard to complete the setup.

### 3. Download LingYggdrasil

Go to the Releases page and download the latest stable version:

```text
LingYggdrasil.jar
```

Place it in any directory on your server or local machine.

### 4. Start the Program

Execute the following command in the directory where `LingYggdrasil.jar` is located:

```bash
java -jar LingYggdrasil.jar
```

You can also add JVM memory parameters according to your server configuration, for example:

```bash
java -Xms512M -Xmx2G -jar LingYggdrasil.jar
```

Once started, you can begin using it.

---

## Tech Stack

| Component          | Technology                                                                 |
| ------------------ | -------------------------------------------------------------------------- |
| Language           | Java 25                                                                    |
| Web Framework      | [Javalin](https://javalin.io/)                                             |
| Database Connection| [HikariCP](https://github.com/brettwooldridge/HikariCP) Connection Pool    |
| Database           | SQLite / MySQL / PostgreSQL                                                |
| Cryptography       | [Bouncy Castle](https://www.bouncycastle.org/) (Argon2, Ed448, RSA)        |
| Email              | [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/)           |
| Logging            | [Logback](https://logback.qos.ch/) + SLF4J                                 |
| Serialization      | Jackson                                                                    |
| Build Tool         | Maven                                                                      |