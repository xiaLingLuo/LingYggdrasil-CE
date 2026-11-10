# 插件管理

本页面向服务器管理员，介绍三类插件与安装/启停方式。开发见[插件开发手册](../07-plugin-dev/guide.md)。

> ⚠️ **安全提示**：传统插件与旧 API 桥接插件是**完全可信代码**，可访问数据库/配置/服务器对象；仅 `plugin-spi` 插件运行在隔离类加载器（只能看到 JDK 与 SPI 接口）。请勿加载来路不明的插件。

## 三类插件

| 类型 | 目录 / 声明 | 加载方式 | 说明 |
|---|---|---|---|
| **传统插件** | `plugins/*.jar` + `plugin.yml` | 旧 `PluginManager` | 继续可用，与旧版行为一致 |
| **稳定 SPI 插件** | `plugins/spi/*.jar` + `plugin.json` | 新 `PluginHost` + 隔离类加载 | 推荐新插件使用，只暴露 `plugin-spi` |
| **旧 API 桥接插件** | `plugins/legacy/*.jar`（旧 `plugin.api`） | `LegacyPluginBridgeLoader` → 新宿主 | ADR-0018 兼容窗口，2.11.0 移除 |

启动时按顺序加载：传统插件（`plugins/*.jar`）→ SPI（`plugins/spi/`）→ 旧 API 桥接（`plugins/legacy/`）；单个插件损坏不会阻断启动，日志会记录 rejected 原因。

## 目录结构

```text
运行目录/
├── LingYggdrasil-2.10.0.jar
└── plugins/
    ├── plugins-state.json     # 传统插件启停状态（自动生成）
    ├── HelloWorld.jar         # 传统插件（plugin.yml）
    ├── HelloWorld/            # 传统插件私有数据目录
    ├── spi/
    │   └── demo-spi.jar       # SPI 插件（plugin.json）
    ├── legacy/
    │   └── old-plugin.jar     # 旧 plugin.api 插件（桥接）
    └── data/
        └── <plugin-id>/       # SPI 插件数据目录（宿主创建）
```

## 安装与启停

### 传统插件

1. 将插件 JAR 放入 `plugins/`；
2. 重启，或在管理后台「插件管理」中重载；
3. 通过后台总览启停/重载，状态保存在 `plugins-state.json`。

### SPI 插件

1. 将带 `plugin.json` 的 JAR 放入 `plugins/spi/`；
2. 重启程序（启动日志 `[Plugin] SPI plugins loaded: N`）；
3. 当前 SPI 宿主不提供后台启停 UI；升级/停用通过替换/移除文件并重启完成。

### 旧 API 桥接插件

1. 将旧插件 JAR（使用 `LingYggdrasil-plugin-api-2.10.0.jar` 编译）放入 `plugins/legacy/`；
2. 重启，启动日志 `[Plugin] Legacy plugins bridged: N`；
3. 生命周期由新宿主统一管理；旧上下文的菜单/路由注册在桥接模式不支持（记录警告）。

## 后台「插件管理」

管理后台一级菜单「插件管理」展示传统插件的图标、名称、版本、作者、介绍与运行状态，支持启停/重载；插件可注册至多一个二级菜单。

SPI 插件与桥接插件通过启动日志与插件自身日志观察；如需要后台可视化，请使用传统插件或等待后续版本。

## 运行状态（传统插件）

| 状态 | 含义 |
|---|---|
| 运行中 | 已加载并启用 |
| 已停止 | 被管理员停止；只展示元信息 |
| 依赖缺失 | 声明的硬依赖不存在 |
| 加载失败 | 主类缺失、实例化异常等 |
| 版本不兼容 | 插件声明的 API 版本与服务端不匹配 |

## 开发与迁移

- 新插件建议直接使用 `:plugin-spi`（`im.xz.cn.plugin.spi`），构建见[插件开发手册](../07-plugin-dev/guide.md)。
- 旧 `plugin.api` 插件请在 2.11.0 前迁移；桥接层仅保证生命周期与基础上下文，不保证旧菜单/路由注册。
- 插件数据：传统插件数据在 `plugins/<name>/`；SPI 插件数据在 `plugins/data/<id>/`。升级主程序不会自动迁移旧插件配置。
