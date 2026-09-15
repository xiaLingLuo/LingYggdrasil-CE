# 泠 Yggdrasil 官方文档

> 轻盈、安全、优雅的 Minecraft Yggdrasil 外置鉴权系统

本目录是泠 Yggdrasil 的官方文档。如果你是第一次接触本项目，请从[快速开始](getting-started.md)读起；如果你已经部署完成，可以按需查阅其他章节。

## 按角色阅读

| 你是谁       | 建议阅读顺序                                                                                |
|--------------|---------------------------------------------------------------------------------------------|
| 第一次部署   | [快速开始](getting-started.md) → [部署与运维](deployment.md) → [配置参考](configuration.md) |
| 服务器管理员 | [管理后台指南](admin-guide.md) → [权限系统](permissions.md) → [安全说明](security.md)       |
| 普通玩家     | [用户功能指南](user-guide.md) → [常见问题](faq.md)                                          |
| 接入启动器   | [Yggdrasil API](yggdrasil-api.md) → [用户功能指南](user-guide.md#角色管理)                  |
| 二次开发     | [开发与构建](development.md) → [自定义](customization.md)                                   |

## 文档导航

| 文档                              | 说明                                        |
|-----------------------------------|---------------------------------------------|
| [快速开始](getting-started.md)    | 环境要求、下载、首次启动与安装向导          |
| [部署与运维](deployment.md)       | 端口规划、反向代理、数据目录、备份、systemd |
| [配置参考](configuration.md)      | `sql.yml` 与所有系统设置项的完整说明        |
| [Yggdrasil API](yggdrasil-api.md) | 认证服务器、会话服务器、纹理端点的协议参考  |
| [管理后台指南](admin-guide.md)    | 各管理模块的功能与操作说明                  |
| [用户功能指南](user-guide.md)     | 注册、登录、角色、皮肤、好友等使用说明      |
| [权限系统](permissions.md)        | 管理员权限组与用户权限组                    |
| [自定义](customization.md)        | 图标覆盖、首页内容、多语言、邮件模板        |
| [安全说明](security.md)           | 加密体系、会话隔离、已知限制与注意事项      |
| [开发与构建](development.md)      | 源码结构、构建方式、二次开发                |
| [常见问题](faq.md)                | FAQ 与排错                                  |

## 项目定位

泠 Yggdrasil 是一个为 Minecraft 打造的 **Yggdrasil 外置鉴权系统**，完整兼容 authlib 体系，提供账户注册、登录、角色管理与皮肤/披风托管能力。它内置 Web 安装向导与二次元风格的管控面板，开箱即用。

- **单 JAR 部署**：一个可执行 JAR 放入服务器即可运行。
- **多数据库支持**：SQLite / MySQL / PostgreSQL。
- **完整 Yggdrasil 协议**：兼容主流 Minecraft 启动器。
- **三种签名模式**：Ed448 与 RSA-SHA512 / RSA-SHA1。
- **十种语言**：简体中文、繁体中文、中文（華夏）、English、Русский、Deutsch、Français、Italiano、日本語、한국어。
- **深色主题**：浅色 / 深色一键切换。

## 版本

当前文档对应 **2.0.1**（正式版）。构建目标为 **Java 25**。

## 相关链接

- 仓库：[github.com/xiaLingLuo/LingYggdrasil-CE](https://github.com/xiaLingLuo/LingYggdrasil-CE)
- 在线预览：[multimc.cn](https://multimc.cn/)
- 许可协议：[AGPL-3.0](../LICENSE)
