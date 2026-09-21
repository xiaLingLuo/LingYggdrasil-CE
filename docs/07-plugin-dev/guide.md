# 插件开发指南

泠 Yggdrasil 支持以 jar 形式扩展功能。插件在**正常模式**启动时从运行目录的 `plugins/` 文件夹加载，管理员可在后台「插件管理」中查看与启停。面向管理员的安装与运维说明见[插件管理](../05-plugins/management.md)。

> ⚠️ **安全提示**：插件是**完全可信代码**，拥有与主程序相同的权限（可访问数据库、配置与服务器对象）。请勿加载来路不明的插件，加载第三方插件后果自负。类加载器仅用于隔离，**不是沙箱**。类加载采用**父优先（parent-first）**委派，插件无法覆盖主程序或依赖库中的同名类；如需特定版本的第三方库，请自行重定位（shade/relocate）包名。

---

## 1. 目录结构

```text
运行目录/
├── LingYggdrasil-2.2.6.jar
├── plugins/
│   ├── plugins-state.json   # 启停状态（自动生成，只增不减）
│   ├── HelloWorld.jar       # 你的插件
│   └── HelloWorld/          # 插件私有数据目录（由插件自行读写）
└── ...
```

- `plugins/` 不存在时，正常模式启动会自动创建。
- **安装模式不会创建目录、也不会加载任何插件。**
- 每个插件拥有独立数据目录 `plugins/<name>/`。

---

## 2. plugin.yml

每个插件 jar 的**根目录**必须包含 `plugin.yml`（或 `plugin.yaml`）。缺失或缺少必填字段的 jar 会被视为**非插件**而忽略。

### 必填字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | 唯一内部标识，匹配 `^[A-Za-z0-9_.-]+$` |
| `ver` | string | 插件版本 |
| `main` | string | 主类全限定名，须继承 `im.xz.cn.plugin.api.LingPlugin` |
| `apiVer` | string | 兼容的插件 API 版本（见下） |
| `hotReloadable` | boolean | 是否支持在后台热启停 |

### 选填字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `friendlyName` | string | 人类可读名称（可含中文），优先展示；`name` 仍是唯一标识 |
| `authors` | list | 作者列表 |
| `website` | string | 网站 |
| `description` | string | 介绍（总览展示） |
| `icon` | string | jar 内图标路径，支持 png / jpg / svg / webp |
| `depend` | list | 硬依赖插件名 |
| `softdepend` | list | 软依赖插件名（仅影响加载顺序，缺失忽略） |
| `perms` | map | 权限节点声明，见第 5 节 |

### 保留名称

以下名称（忽略大小写）被系统保留，作为插件名会被视为**无法识别的 jar**：

```
lingyggdrasil, lingyggdrasil-main, lingyggdrasil-api,
yggdrasil, yggdrasil-main, yggdrasil-api, ling
```

### 完整示例

```yaml
name: HelloWorld
ver: '1.0.0'
main: com.example.helloworld.HelloWorld
apiVer: '2.2.6'
hotReloadable: true
friendlyName: 你好世界
authors: [ LingYggdrasilTeam ]
website: example.com
description: 一个示例插件
icon: icon.png
depend:
  - SomeLib
softdepend:
  - OptionalThing
perms:
  helloworld.example:
    description: 示例权限节点
    type: admin          # user 或 admin
```

---

## 3. apiVer 兼容规则

插件 API 版本与主程序版本**同步发布**，当前为 `2.2.6`（取自 `pom.xml`）。

- 单值 `2.2.6` → 必须**精确等于**服务端 API 版本才兼容。
- 区间 `2.2.0to2.2.6` → 服务端版本落在闭区间 `[2.2.0, 2.2.6]` 内即兼容。

不兼容的插件会显示为「版本不兼容」，只展示元信息，不加载代码。

---

## 4. 开发与构建

### 4.1 依赖

构建主程序时会同步生成插件开发 API jar：

```text
target/LingYggdrasil-plugin-api-2.2.6.jar
```

该 jar **自包含**（含 `im.xz.cn.plugin.api` 及公开签名引用的服务端类型），插件只需依赖它即可编译。

### 4.2 主类

```java
package com.example.helloworld;

import im.xz.cn.plugin.api.LingPlugin;
import im.xz.cn.plugin.api.PluginContext;
import im.xz.cn.plugin.api.PluginMenu;

public class HelloWorld extends LingPlugin {

    private PluginContext context;

    @Override
    public void onLoad(PluginContext context) {
        this.context = context;
        context.getLogger().info("HelloWorld loaded");
    }

    @Override
    public void onEnable() {
        context.registerMenu(new PluginMenu("hello", "HelloWorld", request ->
                "<div class=\"settings-card card\"><div class=\"card-body\">Hello!</div></div>"));
    }

    @Override
    public void onDisable() {
        context.getLogger().info("HelloWorld disabled");
    }
}
```

### 4.3 打包

将编译后的类与 `plugin.yml`（位于 jar 根）一起打包：

```bash
javac -cp LingYggdrasil-plugin-api-2.2.6.jar -d out src/**/*.java
jar cf HelloWorld.jar -C out . -C resources plugin.yml
```

把 `HelloWorld.jar` 放入 `plugins/`，重启（或热重载）即可。

---

## 5. 权限节点声明

插件可通过 `perms` 声明自己使用的权限节点，使其出现在后台权限组页面中，便于分配：

```yaml
perms:
  helloworld.example:
    description: 示例权限
    type: admin     # user = 用户权限组；admin = 管理员权限组
```

- 声明**仅用于展示与放行**（让节点可被勾选保存），实际鉴权由插件代码自行实现。
- 即使不声明，节点在代码中仍然有效，只是后台不会列出。
- 权限组页面按**来源**分组：内置节点来源为 `LingYggdrasil`，插件节点来源为其 `friendlyName`（未设置时用 `name`）。

---

## 6. 后台菜单与 API

### 6.1 二级菜单（可选，至多一个）

插件可在 `onEnable` 中调用 `context.registerMenu(...)` 注册**至多一个**二级菜单，显示在后台「插件管理」下。菜单内容为 **HTML 片段**，会被嵌入标准后台布局。

- 出于安全考虑（CSP 已移除 `unsafe-inline`），片段内不要使用内联 `onclick`，请使用 `data-action` 属性配合 `common.js` 的事件委托。
- 重复注册只保留第一次。
- **权限风险提示**：后台**不会**对插件二级菜单施加任何内置权限校验（仅要求管理员已登录，并保留 CSRF 防护）。是否允许某管理员访问完全由**插件自行判断**——可在菜单渲染函数中调用 `request.hasAdminPermission(node)` 决定渲染内容或返回拒绝页面。若不做校验，则任何已登录管理员均可访问该菜单，请务必自行实现鉴权。

### 6.2 命名空间 API

插件可注册自己的 API 路由：

```java
context.registerApiRoute("ping", (request, response) ->
        response.json(Map.of("success", true, "message", "pong")));
```

对应 URL 为：

```text
/admin/api/plugins/<name>/api/ping
```

这些路由受管理后台登录与 CSRF 保护，但**不受**任何内置插件权限校验；是否鉴权由插件自行实现（可通过 `request.hasAdminPermission(node)` 判断）。

### 6.3 用户端与世界树路由

插件可以在用户端（35565）与世界树 API（35577）上注册**未使用**的路由点，并完全接管该路由的响应：

```java
context.registerUserRoute("GET", "/hello/status", (request, response) ->
        response.json(Map.of("success", true, "plugin", context.getName())));

context.registerYggdrasilRoute("GET", "/hello/hello", (request, response) ->
        response.json(Map.of("hello", "world")));
```

- 方法可填 `GET`/`POST`/`PUT`/`DELETE`/`PATCH`，或 `*` 表示全部。
- 路径必须为**具体路径**（不含 `{}`、`*` 等占位符），以 `/` 开头。
- **保留前缀**：以下前缀为 LingYggdrasil 内置命名空间，插件路由不得使用，注册会被拒绝并记入插件系统日志：`/api`、`/authserver`、`/sessionserver`、`/admin`、`/css`、`/js`、`/img`、`/icons`、`/builtin-icons`、`/favicon.ico`。请使用自有前缀（如示例的 `/hello`）。
- **冲突处理**：多个插件注册同一方法+路径时，先注册者生效，后注册者被拒绝并记入日志。
- 插件可自行设置状态码、响应头与响应体（`text`/`html`/`json`），即完全管理该页面。
- 请求对象提供 `hasUserPermission(node)`（用户端）与 `hasAdminPermission(node)`（管理端）用于鉴权。

### 6.4 PluginContext 能力

| 方法 | 说明 |
|------|------|
| `getName()` / `getFriendlyName()` / `getVersion()` | 元信息 |
| `getDataFolder()` | 插件私有目录 `plugins/<name>/`（按需创建） |
| `getLogger()` | 统一日志（写入 `plugins/<name>/logs/<name>-<日期>.log`） |
| `getDatabaseManager()` | 数据库访问 |
| `getSystemConfig()` | 系统配置 |
| `getServerDirectory()` | 服务器运行目录 |
| `registerMenu(PluginMenu)` | 注册二级菜单 |
| `registerApiRoute(path, handler)` | 注册管理后台命名空间 API |
| `registerUserRoute(method, path, handler)` | 注册用户端路由 |
| `registerYggdrasilRoute(method, path, handler)` | 注册世界树 API 路由 |

---

## 7. 加载与状态

| 状态 | 含义 |
|------|------|
| 运行中 | 已加载并启用 |
| 已停止 | 被管理员停止；只展示元信息，不加载代码 |
| 依赖缺失 | 声明的硬依赖不存在；点击状态旁 `?` 可查看依赖列表 |
| 加载失败 | 主类缺失、实例化异常等 |
| 版本不兼容 | `apiVer` 与服务端不匹配 |

- 启停状态保存在 `plugins/plugins-state.json`，**只增不减**，不会自动清理。删除的 jar 对应记录会保留但被忽略，可手动清理。
- 首次发现的插件默认启用。
- `hotReloadable: false` 的插件无法在后台启停，点击按钮会提示「作者声明本插件无法热重载」；其首次发现后默认启用，需随进程停止而关闭。
- 硬依赖缺失 / 版本不兼容 / 加载失败的插件，其「期望启用」状态会被保留，补齐依赖或升级后重启即可自动加载。
- 日志文件：插件系统日志写入 `logs/plugin/plugin-<日期>.log`；插件自身日志写入 `plugins/<name>/logs/<name>-<日期>.log`。

---

## 8. 示例

完整示例见仓库 [`examples/helloworld/`](../../examples/helloworld/)。
