# 插件开发指南

当前版本提供两套插件 API：

| API | 适用 | 加载目录 | 状态 |
|---|---|---|---|
| **稳定 SPI**（`im.xz.cn.plugin.spi`） | 新插件 | `plugins/spi/*.jar` + `plugin.json` | **推荐**，本项目主推 |
| **旧 `plugin.api`**（`im.xz.cn.plugin.api.LingPlugin`） | 既有插件 | `plugins/legacy/*.jar` | 桥接兼容至 2.11.0 |

传统 `plugins/*.jar` + `plugin.yml` 模式仍可用（见[插件管理](../05-plugins/management.md)），但新开发请使用 SPI。

> **安全模型**：SPI 插件运行在隔离类加载器：插件类只从自身 JAR 加载；只能看到 **JDK** 与 `im.xz.cn.plugin.spi`，看不到 `DatabaseManager`/`SystemConfig`/DAO/Javalin。第三方库必须自行 shade 进插件 JAR。传统与桥接插件仍是完全可信代码。

---

## 1. SPI 项目结构

```text
my-plugin/
├── plugin.json
└── src/main/java/com/example/MyPlugin.java
```

构建产物（JAR）根目录必须包含 `plugin.json`：

```text
my-plugin.jar
├── plugin.json
└── com/example/MyPlugin.class
```

依赖 `LingYggdrasil-plugin-spi-2.10.0.jar`（由主程序构建产出，或从发布包获取）；不要依赖 `:core` 内部类。

Gradle 示例：

```kotlin
dependencies {
    compileOnly(files("libs/LingYggdrasil-plugin-spi-2.10.0.jar"))
}
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
```

Maven 示例：

```xml
<dependency>
  <groupId>im.xz.cn</groupId>
  <artifactId>LingYggdrasil-plugin-spi</artifactId>
  <version>2.10.0</version>
  <scope>provided</scope>
</dependency>
```

---

## 2. `plugin.json`

```json
{
  "id": "my-plugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "mainClass": "com.example.MyPlugin",
  "apiVersion": "2.10.0",
  "author": "you",
  "description": "demo plugin",
  "dependencies": [],
  "permissions": []
}
```

| 字段 | 必填 | 约束 |
|---|---|---|
| `id` | 是 | `^[a-z][a-z0-9_-]{1,63}$`，全局唯一 |
| `name` | 是 | 展示名称 |
| `version` | 是 | `\d+(\.\d+){0,3}([-+][0-9A-Za-z.-]+)?` |
| `mainClass` | 是 | 实现 `im.xz.cn.plugin.spi.Plugin` 的类全名 |
| `apiVersion` | 是 | 与运行时 API 兼容：**同主版本**且不高于当前（当前版本） |
| `author` / `description` | 否 | 展示信息 |
| `dependencies` / `permissions` | 否 | 依赖插件 id 列表 / 权限节点声明 |

加载时校验：缺 `plugin.json`、字段非法、主类不存在/未实现 `Plugin`、API 版本不兼容 → 该 JAR 被拒绝并记录日志，不影响其他插件与主程序启动。

---

## 3. 最小插件示例

```java
package com.example;

import im.xz.cn.plugin.spi.Plugin;
import im.xz.cn.plugin.spi.PluginContext;

public final class MyPlugin implements Plugin {

    private PluginContext context;

    @Override
    public void onLoad(PluginContext context) {
        this.context = context;
        context.logger().info("loaded: {} v{}", context.pluginId(), "1.0.0");
        context.storage().put("enabledAt", Long.toString(System.currentTimeMillis()));
    }

    @Override
    public void onEnable() {
        context.scheduler().scheduleAtFixedRate(() -> {
            context.logger().info("heartbeat, dataDir={}", context.dataDirectory());
        }, 0, 60, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        context.logger().info("disabled");
    }
}
```

生命周期由宿主调用：`onLoad(context)` → `onEnable()` → （运行）→ `onDisable()`。任何异常由宿主捕获并记录，插件不得调用 `System.exit` 或长时间阻塞宿主线程。

---

## 4. 插件上下文 API

| 方法 | 说明 |
|---|---|
| `pluginId()` | 插件 id（与 `plugin.json` 一致） |
| `dataDirectory()` | 插件专属可写目录（`plugins/data/<id>/`） |
| `logger()` | 作用域日志（info/warn/error） |
| `storage()` | 键值存储（`get/put/remove`），仅本插件命名空间 |
| `scheduler()` | `runAsync` / `schedule(delay)` / `scheduleAtFixedRate(initial, period)`，返回可取消句柄 |

SPI 故意不暴露数据库、配置、Javalin 或内部 DAO；需要持久化请用 `storage()`，需要周期任务用 `scheduler()`，通过日志与管理员沟通。

---

## 5. 构建与安装

```bash
# 产物：my-plugin.jar（含 plugin.json 与类）
jar cf my-plugin.jar plugin.json -C build/classes/java/main .
```

1. 将 JAR 放入运行目录 `plugins/spi/`；
2. 重启程序；
3. 启动日志确认：`[Plugin] SPI plugins loaded: N (rejected=M)`；拒绝原因会打印在日志中。

当前 SPI 没有后台启停 UI；停用/升级 = 替换/移除 JAR 后重启。

---

## 6. 调试建议

- 日志中搜索 `[Plugin]`；单个插件失败不影响其他插件。
- 确认 `plugin.json` 在 JAR 根目录（不是 `META-INF/` 或子目录）。
- 确认 `mainClass` 有无参构造器且 `implements Plugin`。
- 插件依赖的第三方库必须 shade 进 JAR（隔离加载下不可见主程序类路径”。
- `apiVersion` 主版本必须与运行时一致（当前 2.10.x）。

---

## 7. 旧 `plugin.api` 插件（桥接迁移）

旧插件使用 `im.xz.cn.plugin.api.LingPlugin` 与 `LingYggdrasil-plugin-api-2.10.0.jar` 编译。放入 `plugins/legacy/` 后由桥接加载：

```java
public class OldPlugin extends LingPlugin {
    @Override public void onLoad(PluginContext context) { /* 旧上下文 */ }
    @Override public void onEnable() {}
    @Override public void onDisable() {}
}
```

桥接提供：`getName/getVersion/getDataFolder/getLogger/getDatabaseManager/getSystemConfig/getServerDirectory`；**菜单/路由注册在桥接模式不支持**（调用会记录警告）。生命周期仍为 bind → onLoad → onEnable → onDisable。

`im.xz.cn.plugin.api` 与桥接层将在 **2.11.0 移除**，请尽快迁移到 SPI。

---

## 8. 传统 `plugin.yml` 插件

旧 `plugins/*.jar` + `plugin.yml`（`name/ver/main/apiVer/hotReloadable`）继续由旧 `PluginManager` 加载，可注册菜单/API/用户端/世界树路由，支持后台启停。该模式为完全可信代码，后续不再扩展；新插件请使用 SPI。
