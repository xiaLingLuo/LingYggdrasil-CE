# 开发与构建

本页面向希望自行构建、二次开发或调试泠 Yggdrasil 的开发者。

## 环境要求

| 工具  | 版本 |
|-------|------|
| JDK   | 25   |
| Maven | 3.9+ |

## 构建

在项目根目录执行：

```bash
mvn clean package
```

跳过测试：

```bash
mvn -DskipTests package
```

构建产物：

```text
target/LingYggdrasil-2.2.7.jar              # 主程序（含依赖，可执行）
target/LingYggdrasil-plugin-api-2.2.7.jar   # 插件开发 API（自包含）
```

`LingYggdrasil-2.2.7.jar` 通过 `maven-assembly-plugin` 打包为**含依赖的可执行 JAR**，可直接运行：

```bash
java -jar target/LingYggdrasil-2.2.7.jar
```

`LingYggdrasil-plugin-api-2.2.7.jar` 由 `maven-antrun-plugin` 在 `package` 阶段生成，仅包含插件开发所需的 API 类型及其公开签名引用的服务端类型，供插件开发者编译使用。版本号统一取自 `pom.xml` 的 `${project.version}`，无需单独维护。

## 主要依赖

| 依赖                                              | 用途               |
|---------------------------------------------------|--------------------|
| Javalin                                           | Web 框架           |
| HikariCP                                          | 数据库连接池       |
| SQLite JDBC / MySQL Connector/J / PostgreSQL JDBC | 数据库驱动         |
| Bouncy Castle                                     | Argon2、Ed448、RSA |
| Eclipse Angus Mail                                | 邮件发送           |
| Jackson                                           | JSON 序列化        |
| Logback + SLF4J                                   | 日志               |
| SnakeYAML                                         | `sql.yml` 解析     |

## 源码结构

```text
src/main/java/im/xz/cn/
├── Yggdrasil.java                 # 程序入口
├── auth/                          # 认证：Argon2、AuthService、会话、登录限流
├── bootstrap/                     # ServerFactory：各服务共用的创建与安全头、图标路由
├── common/                        # 通用工具：AppIcons、FooterInfo、IpUtil、Treasure 等
├── config/                        # 配置：AppConfig、SystemConfig、MailConfig、DatabaseConfig
├── database/                      # 数据库：DatabaseManager、DatabaseSchema、dao/
├── i18n/                          # 国际化：I18n、LocaleContext、LocaleResolver
├── logging/                       # 日志：logApi 门面、AuditLogger、UserActionLogger
├── mail/                          # 邮件：MailService
├── model/                         # 实体模型与枚举
├── permission/                    # 动态权限节点注册中心与内置节点
├── plugin/                        # 插件系统（PluginManager、plugin.yml 解析、类加载器）
│   └── api/                       #   插件开发 SDK（打入 plugin-api jar）
├── rate/                          # 频率限制
├── security/                      # 安全：权限、Root 完整性、配置加密、密钥管理
├── server/                        # 四个服务与各 handler
│   ├── UserServer.java            #   用户端（35565）
│   ├── YggdrasilServer.java       #   Yggdrasil API（35577）
│   ├── AdminServer.java           #   管理端（35599）
│   ├── InstallServer.java         #   安装向导（35598）
│   └── handler/                   #   user / admin / yggdrasil / install
├── texture/                       # 纹理：TextureService、PngNormalizer
├── web/                           # 页面渲染：PageRenderer、Shared、Csp、view/
└── yggdrasil/                     # Yggdrasil 协议工具
```

前端资源位于 `src/main/resources/static/`：

```text
static/
├── css/       # tokens.css（设计令牌）、admin.css、user.css、install.css、all.min.css
├── js/        # common.js（公共逻辑与事件委托）、各页面脚本
├── fonts/     # 字体
└── img/       # 图片
```

国际化语言包位于 `src/main/resources/i18n/`（各语言 `{locale}.json`），首页 HTML 模板位于 `src/main/resources/i18n/index-page/`（各语言 `{locale}.html`）。

## 开发约定

- **日志**：业务代码统一使用 `logApi` 门面，不直接依赖 SLF4J / JUL / `System.out`。
- **国际化**：新增文案时在所有语言的 JSON 中同步添加键；首页文案改动同步 `i18n/index-page/{locale}.html`。
- **前端事件**：页面按钮通过 `data-action` 属性 + `common.js` 的事件委托绑定，不要使用内联 `onclick`（CSP 已移除 `'unsafe-inline'`）。支持 `data-args`（JSON 数组）、`data-this`、`data-event`、`data-prevent`、`data-stop`。
- **保存类按钮**：`data-action` 以 `save`/`submit` 开头的按钮会自动获得加载状态（转圈 + 完成打勾），无需额外代码。
- **安全**：所有数据库访问使用参数化查询；输出到 HTML 时进行转义。
- **权限节点**：不再硬编码，统一由 `permission/PermissionRegistry` 动态管理。内置节点以内置插件形式在启动时以来源 `LingYggdrasil` 注册；插件节点从 `plugin.yml` 的 `perms` 注册。新增内置节点请修改 `permission/BuiltinPermissions`。
- **插件**：仅在正常模式加载，安装模式不触碰 `plugins/`。插件启停状态保存在 `plugins/plugins-state.json`（文件持久化，无数据库迁移）。

## 数据库结构

首次启动或升级时，`DatabaseSchema` 会自动创建/迁移以下表：

`admins`、`root_info`、`op_perm_group`、`users`、`user_perm_group`、`player_profiles`、`auth_tokens`、`cache_store`、`system_settings`、`textures`、`friends`、`confirming_friends`、`blocked_users`、`texture_meta`、`texture_likes`、`texture_favorites`、`texture_visibility`、`friend_shared_textures`。

## 调试建议

- 将运行目录设为项目内的临时目录，避免污染源码目录。
- 首次运行会进入安装向导；也可手动准备 `sql.yml` 与 `.INSTALLED` 直接进入正式模式（仅供开发调试）。
- 数据库结构变更请编写迁移逻辑，兼容 SQLite / MySQL / PostgreSQL 三种方言。

## 相关文档

- 自定义图标、语言包与首页模板：见[用户端界面自定义](../02-user/customization.md)与[管理端界面自定义](../03-admin/customization.md)。
- 安全机制与限制：见[安全说明](../06-security/security.md)。
