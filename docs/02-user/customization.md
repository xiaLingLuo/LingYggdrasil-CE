# 用户端界面自定义

本页介绍如何自定义用户端（35565）的首页内容、语言与主题。管理端界面自定义见[管理端界面自定义](../03-admin/customization.md)。

## 首页内容

首页正文不由后台配置，而是由各语言的 HTML 模板渲染：

- 内置模板位于 `src/main/resources/i18n/user/index-page/{语言代码}.html`。
- 程序启动时会释放到运行目录的 `i18n/user/index-page/` 文件夹（若不存在）。
- 外部文件优先于内置模板；当前语言缺少模板时回退到默认语言（`zh-CN`）。

模板即首页主内容区（`<div class="main-container">` 内部）的完整 HTML，可直接编辑其中的文案与结构。支持两类占位符：

- `{{siteName}}`：站点名称（来自「系统管理 → 基本设置」）。
- `{{account}}`：登录状态相关的按钮区块（未登录为「登录 / 注册」，已登录为仪表盘与登出）。

此外，任意 `{{i18n.key}}` 标记都会按当前语言包替换，例如 `{{common.save}}`。

> 模板以原始 HTML 注入，请仅使用可信内容，避免引入脚本。

## 多语言

泠 Yggdrasil 内置简体中文（`zh-CN`）、繁体中文（`zh-TW`）、中文（華夏，`zh-XIA`）、英文（`en-US`）、俄文（`ru-RU`）、德文（`de-DE`）、法文（`fr-FR`）、意大利文（`it-IT`）、日文（`ja-JP`）、韩文（`ko-KR`）等语言，语言包为 JSON 格式。

### 外部语言包

程序启动时会：

1. 将内置语言包与首页模板释放到运行目录的 `i18n/user/` 文件夹（若不存在）。
2. 读取其中的语言包，并与内置包**深度合并**：外部文件中存在的键覆盖内置，缺失的键回退内置。

因此，你可以在不改动程序的情况下修改文案：

```text
运行目录/
└── i18n/
    └── user/
        ├── zh-CN.json
        ├── en-US.json
        └── index-page/
            ├── zh-CN.html
            └── en-US.html
```

编辑对应 JSON 中某个键的值，重启程序（或重新打开页面）即可生效。

### 新增语言

新增语言需要修改源码：

1. 在 `I18n.SUPPORTED_LOCALES` 中添加一条 `LocaleOption`（语言代码与显示名）。
2. 在 `src/main/resources/i18n/user/` 下添加对应语言的 JSON 语言包。
3. 在 `src/main/resources/i18n/user/index-page/` 下添加对应语言的首页 HTML 模板。

语言下拉菜单会由 `I18n.supportedLocales()` 自动生成。

## 主题

用户端支持浅色 / 深色主题切换，选择随账户持久化，也可在未登录时通过浏览器本地存储记忆。
