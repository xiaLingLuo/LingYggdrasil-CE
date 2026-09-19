# 泠 Yggdrasil 官方文档

> 轻盈、安全、优雅的 Minecraft Yggdrasil 外置鉴权系统

本目录是泠 Yggdrasil 的官方文档，按功能划分为**七大板块**。

## 七大板块

| 板块 | 说明 |
|------|------|
| [① 概述](01-overview/README.md) | 项目简介、快速开始、部署运维、配置参考、开发构建、常见问题 |
| [② 用户系统](02-user/README.md) | 用户功能、[用户权限系统](02-user/permissions.md)、[用户端界面自定义](02-user/customization.md) |
| [③ 管理系统](03-admin/README.md) | 管理后台、[管理员权限系统](03-admin/permissions.md)、[管理端界面自定义](03-admin/customization.md) |
| [④ 世界树系统](04-yggdrasil/README.md) | Yggdrasil 认证、会话与纹理协议 |
| [⑤ 插件系统](05-plugins/README.md) | 插件的安装、查看与启停 |
| [⑥ 安全](06-security/README.md) | 加密体系、会话隔离、请求防护与安全清单 |
| [⑦ 插件开发手册](07-plugin-dev/README.md) | `plugin.yml`、插件 API、路由与示例 |

## 按角色阅读

| 你是谁       | 建议阅读顺序                                                                                              |
|--------------|-----------------------------------------------------------------------------------------------------------|
| 第一次部署   | [快速开始](01-overview/getting-started.md) → [部署与运维](01-overview/deployment.md) → [配置参考](01-overview/configuration.md) |
| 服务器管理员 | [管理后台指南](03-admin/guide.md) → [管理员权限系统](03-admin/permissions.md) → [插件系统](05-plugins/README.md) → [安全](06-security/security.md) |
| 普通玩家     | [用户功能指南](02-user/guide.md) → [常见问题](01-overview/faq.md)                                          |
| 接入启动器   | [Yggdrasil API](04-yggdrasil/api.md) → [用户功能指南](02-user/guide.md#角色管理)                           |
| 插件开发者   | [插件开发手册](07-plugin-dev/guide.md) → [开发与构建](01-overview/development.md)                          |

## 项目定位

泠 Yggdrasil 是一个为 Minecraft 打造的 **Yggdrasil 外置鉴权系统**，完整兼容 authlib 体系，提供账户注册、登录、角色管理与皮肤/披风托管能力。它内置 Web 安装向导与二次元风格的管控面板，开箱即用。

- **单 JAR 部署**：一个可执行 JAR 放入服务器即可运行。
- **多数据库支持**：SQLite / MySQL（PostgreSQL 代码保留，暂不可用）。
- **完整 Yggdrasil 协议**：兼容主流 Minecraft 启动器。
- **三种签名模式**：Ed448 与 RSA-SHA512 / RSA-SHA1。
- **插件系统**：外部插件加载、动态权限节点与插件开发 API。
- **十种语言**：简体中文、繁体中文、中文（華夏）、English、Русский、Deutsch、Français、Italiano、日本語、한국어。
- **深色主题**：浅色 / 深色一键切换。

## 版本

当前文档对应 **2.1.0**（正式版）。构建目标为 **Java 25**。

## 相关链接

- 仓库：[github.com/xiaLingLuo/LingYggdrasil-CE](https://github.com/xiaLingLuo/LingYggdrasil-CE)
- 在线预览：[multimc.cn](https://multimc.cn/)
- 许可协议：[AGPL-3.0](../LICENSE)
