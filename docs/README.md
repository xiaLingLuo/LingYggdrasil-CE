# 泠 Yggdrasil 官方文档

> 轻盈、安全、优雅的 Minecraft Yggdrasil 外置鉴权系统

## 七大板块

| 板块 | 说明 |
|---|---|
| [① 概述](01-overview/README.md) | 项目简介、快速开始、部署运维、配置参考、常见问题 |
| [② 用户系统](02-user/README.md) | 用户功能、[权限](02-user/permissions.md)、[界面自定义](02-user/customization.md) |
| [③ 管理系统](03-admin/README.md) | 管理后台、[权限](03-admin/permissions.md)、[界面自定义](03-admin/customization.md) |
| [④ 世界树系统](04-yggdrasil/README.md) | Yggdrasil 协议、[API 模式](04-yggdrasil/api-modes.md)、[接口参考](04-yggdrasil/api.md) |
| [⑤ 插件系统](05-plugins/README.md) | 插件安装、查看与启停 |
| [⑥ 安全](06-security/README.md) | 加密、会话隔离、请求防护与安全清单 |
| [⑦ 插件开发手册](07-plugin-dev/README.md) | SPI、旧 API 桥接与示例 |

## 按角色阅读

| 你是谁 | 建议阅读顺序 |
|---|---|
| 第一次部署 | [快速开始](01-overview/getting-started.md) → [部署与运维](01-overview/deployment.md) → [配置参考](01-overview/configuration.md) |
| 服务器管理员 | [管理后台指南](03-admin/guide.md) → [权限](03-admin/permissions.md) → [插件系统](05-plugins/README.md) → [安全](06-security/security.md) |
| 普通玩家 | [用户功能指南](02-user/guide.md) → [常见问题](01-overview/faq.md) |
| 接入启动器 | [Yggdrasil API](04-yggdrasil/api.md) → [用户功能指南](02-user/guide.md#角色管理) |
| 插件开发者 | [插件开发手册](07-plugin-dev/guide.md) |

## 项目定位

泠 Yggdrasil 是一个为 Minecraft 打造的 **Yggdrasil 外置鉴权系统**，完整兼容 authlib 体系，提供账户注册、登录、角色管理与皮肤/披风托管能力。内置 Web 安装向导与二次元风格管控面板，开箱即用。

- **单 JAR + 三宿主**：user（35565）/ Yggdrasil（35577）/ admin（35599）独立路由、会话与生命周期，安装向导 35598。
- **多数据库**：SQLite（零配置）/ MySQL / MariaDB。
- **完整 Yggdrasil 协议**：默认 / 独立 / 聚合三种 API 模式，Ed448 / RSA-SHA512 / RSA-SHA1 三种签名。
- **插件系统**：稳定 SPI、旧 API 桥接与传统 `plugin.yml` 插件。
- **多语言与主题**：内置简体中文，浅色/深色主题。

## 相关链接

- 仓库：[github.com/xiaLingLuo/LingYggdrasil-CE](https://github.com/xiaLingLuo/LingYggdrasil-CE)
- 在线预览：[ygg.minecraft.ist](https://ygg.minecraft.ist/)
- 许可协议：[AGPL-3.0](../LICENSE)
