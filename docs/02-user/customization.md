# 用户端界面自定义

本页介绍如何自定义用户端（35565）的图标、首页模板与多语言。管理端界面自定义见[管理端界面自定义](../03-admin/customization.md)。

## 图标

程序内置了一套图标（打包在 JAR 内），同时支持通过运行目录下的 `icons/` 文件夹覆盖。

### 覆盖规则

- 程序**启动时**检查运行目录下是否存在 `icons/` 文件夹。
- **存在**：`/icons/*` 会优先返回 `icons/` 中的同名文件；若某个文件缺失，则回退到内置图标。
- **不存在**：全部使用内置图标。
- 程序**不会**自动创建或释放 `icons/` 文件夹，需要**手动创建**。

### 使用步骤

1. 在运行目录（与 JAR 同级）创建 `icons/` 文件夹。
2. 将你的图标文件放入其中（如站点图标 `app.ico`、语言图标 `lang-zh-CN.svg` 等）。
3. 重启程序。

```text
运行目录/
├── LingYggdrasil-2.10.0.jar
└── icons/
    └── app.ico        # 你的自定义图标
```

### 内置图标始终可用

无论是否存在外部覆盖，内置图标始终可通过 `/builtin-icons/*` 访问：

- `/icons/app.ico`：外部优先，缺失回退内置。
- `/builtin-icons/app.ico`：始终为内置图标。

### 支持的文件类型

按扩展名自动设置 Content-Type，支持 `ico`、`png`、`svg`、`jpg`/`jpeg`、`gif`、`webp`。文件名仅允许字母、数字、`.`、`_`、`-`，且不得包含 `..`。

## 首页模板

首页（`/` 与 `/meow`）的内容由 `i18n/user/index-page/<语言代码>.html` 控制。程序启动时会把内置模板释放到运行目录：

```text
运行目录/
└── i18n/
    └── user/
        └── index-page/
            └── zh-CN.html
```

编辑对应语言的 HTML 模板即可修改首页内容，修改后重启（或强制刷新）生效。模板内可使用 `{{i18n.key}}` 标记按当前语言包替换，例如 `{{common.save}}`。

> 模板以原始 HTML 注入，请仅使用可信内容，避免引入脚本。

## 多语言

泠 Yggdrasil 目前内置简体中文（`zh-CN`），语言包为 JSON 格式。语言清单由 `i18n/lang.json` 描述。

### 语言清单

程序启动时会把内置的 `lang.json` 释放到运行目录（若不存在）：

```json
{
  "default": "zh-CN",
  "languages": {
    "zh-CN": "简体中文"
  }
}
```

- `default`：默认语言代码，必须存在于 `languages` 中。
- `languages`：语言代码到显示名的映射；只有**存在对应语言包**的语言才会出现在切换菜单中。

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
        └── index-page/
            └── zh-CN.html
```

编辑对应 JSON 中某个键的值，重启程序（或重新打开页面）即可生效。

### 新增语言

新增语言需要修改源码：

1. 在 `core/src/main/resources/i18n/lang.json` 的 `languages` 中登记语言代码与显示名，并在 `default` 中指定默认语言。
2. 在 `core/src/main/resources/i18n/user/` 下添加对应语言的 JSON 语言包。
3. 在 `core/src/main/resources/i18n/user/index-page/` 下添加对应语言的首页 HTML 模板。
4. 如该语言也要用于管理端，同步在 `core/src/main/resources/i18n/admin/` 下添加对应 JSON。
5. （可选）在 `core/src/main/resources/icons/` 下添加语言图标 `lang-<语言代码>.svg`；缺失时会显示通用语言图标。

语言下拉菜单由 `i18n/lang.json` 驱动，并只展示实际存在语言包的语言。

## 主题

用户端支持浅色 / 深色主题切换，选择随账户持久化，也可在未登录时通过浏览器本地存储记忆。
