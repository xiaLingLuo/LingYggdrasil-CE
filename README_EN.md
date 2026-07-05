<h1 align="center">Ling Yggdrasil</h1>

<p align="center">
  <em>A lightweight, secure, and elegant Minecraft Yggdrasil external authentication system</em>
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

**Ling Yggdrasil** is a **Yggdrasil external authentication server** built for Minecraft ,Fully compatible with authlib.jar, providing complete account registration, login, character management, and skin/cape hosting capabilities. With an out-of-the-box web installation wizard, an anime-style control panel, and a multi-layered encryption system, it makes your external login system both secure and visually appealing.

> **🌐 [Live Demo](https://multimc.cn/)** — A site running the latest stable version of LingYggdrasil. Try it out here at zero cost before deploying your own.

<p align="center">
  <img src="img/player-card.png" alt="Character Edit" width="800" />
</p>

---

## Features

### 🚀 Out of the Box

- **Web Installation Wizard** — Automatically launches the setup page on first run, guiding you through admin account creation, database and email service configuration with a fully visual workflow
- **Multi-Database Support** — Supports **SQLite**, **MySQL**, and **PostgreSQL** — choose what you need with no extra configuration required
- **Single JAR Deployment** — Packaged as a single executable JAR, just drop it on your server and run

### 🎨 Refined User Interface

- **Anime-Style Design** — Pink-and-white color scheme, rounded card layouts, and delicate CSS animations
- **Responsive Layout** — Fully adapted for both desktop and mobile devices
- **3D Character Preview** — Integrated skinview3d for real-time skin and cape preview while editing characters

### 🔐 Security System

- **Argon2 Password Encryption** — All user passwords are encrypted using the Argon2 algorithm, with 6 adjustable security levels
- **Login Rate Limiting** — Prevents brute-force attacks by automatically limiting login attempt frequency
- **Email Verification** — Requires email verification code activation after registration, with domain whitelist/blacklist support
- **Per-IP Registration Limit** — Restricts the number of accounts that can be registered from the same IP to prevent mass registration
- **Username Blacklist** — Administrators can configure a list of prohibited usernames
- **Independent Session Management** — Admin and user sessions are completely isolated from each other

### 👤 User Features

- **Character Management** — Create multiple game characters, each with its own independent Yggdrasil Token
- **Skins & Capes** — Upload and manage personal skin and cape resources, with alias naming support
- **Security Settings** — Self-manage password changes, email verification, Token viewing, and regeneration
- **Dashboard Guide** — Built-in quick-start guide so even new users can get up to speed easily

### 🛡️ Admin Panel

- **Dashboard Overview** — Core statistics such as user count, character count, and skin/cape count at a glance
- **User Management** — View, edit, and ban users, with email verification status management
- **Character Management** — Full CRUD operations, with support for character name changes and ownership transfers
- **Skin & Cape Management** — Globally manage all texture resources, configure upload size, quantity, storage path, and rate limits
- **Security Settings** — 6-level encryption visualization with card display, adjust Argon2 parameters as needed
- **Email Domain Control** — Whitelist/blacklist mode for precise control over which email domains can register
- **System Settings** — Global configuration for site name, ICP filing number, registration toggle, and more
- **Yggdrasil Settings** — Dedicated Yggdrasil protocol configuration, including signature algorithm, Token validity, and rate limiting

### 🌳 Yggdrasil Protocol

- **Complete Protocol Implementation** — Compatible with mainstream Minecraft launchers for Yggdrasil authentication
- **Dual Signature Mode** — Supports **Ed448** (modern mode) and **RSA-SHA512** (compatibility mode), switchable on demand
- **Token System** — Automatically generates a 64-character high-strength Token per character as the game login credential
- **Texture Hosting** — Publicly exposes the `/textures/{type}/{hash}` endpoint for direct access to skins and capes by game clients
- **Session Management** — Complete login/logout and session validation flow

---

## Installation Guide

How many steps does it take to put an elephant in a fridge?

Just download the latest stable release from the releases page, drop it anywhere on your server, and run

`java -jar LingYggdrasil.jar` 

to start it up~ 

(Tip: adding `-Xms` and `-Xmx` JVM parameters is recommended for a better experience)

---

## Tech Stack

| Component       | Technology                                     |
| --------------- | ---------------------------------------------- |
| Language        | Java 25                                        |
| Web Framework   | [Javalin](https://javalin.io/)                 |
| Database Access | [HikariCP](https://github.com/brettwooldridge/HikariCP) Connection Pool |
| Database        | SQLite / MySQL / PostgreSQL                    |
| Cryptography    | [Bouncy Castle](https://www.bouncycastle.org/) (Argon2, Ed448, RSA) |
| Email           | [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) |
| Logging         | [Logback](https://logback.qos.ch/) + SLF4J     |
| Serialization   | Jackson                                        |
| Build Tool      | Maven                                          |
