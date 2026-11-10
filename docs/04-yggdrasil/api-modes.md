# API 模式

LingYggdrasil 采用用户系统、管理系统、世界树系统三套服务相互独立的现代架构。而早期外置登录系统通常把认证、会话与纹理全部挤进同一个域名，用户已经习惯了「一个地址走天下」。为照顾这部分习惯，同时保留独立架构的性能与安全优势，程序提供三种 API 模式。

> API 模式属于**系统设置**（`yggdrasil` 组，管理端 `/admin/system`），保存后生成不可变配置快照；`skinDomains`、纹理公开 URL 与 `/api/yggdrasil` 挂载路由均由同一生效快照生成。路由开关类变更需重启后完全生效。

## 模式对比

| 模式                       | 快速指南展示的地址    | `/api/yggdrasil` 路由点 | 元数据 skinDomains                    | 适用场景                          |
|----------------------------|-----------------------|-------------------------|---------------------------------------|-----------------------------------|
| `default`（默认）          | 世界树 API 地址       | 开启                    | API 域名 → homeURL/api/yggdrasil 域名 | 新手开箱即用                      |
| `standalone`（独立，推荐） | 世界树 API 地址       | 关闭                    | 仅 API 域名                           | 结构最清晰、性能与安全性最佳      |
| `aggregate`（聚合）        | homeURL/api/yggdrasil | 开启                    | homeURL/api/yggdrasil 域名 → API 域名 | 兼容老旧外置登录系统的单 URL 习惯 |

其中 `homeURL` 指用户端外网域名。

## 各模式说明

### `default`（默认）

- 快速使用指南展示世界树 API 地址。
- 用户端的 `/api/yggdrasil` 路由点开启。
- 元数据 `skinDomains` 依次列出 API 域名、`homeURL/api/yggdrasil` 域名。
- 适合需要同时保留两种入口的过渡部署。

适用于希望开箱即用、不特别在意架构划分的新手。

### `standalone`（独立，推荐）

- 快速使用指南展示世界树 API 地址。
- 关闭用户端的 `/api/yggdrasil` 路由点。
- 元数据 `skinDomains` 仅保留 API 域名。

三服务完全分离，是 LingYggdrasil 设计之初即采用的方式，性能与安全性最佳。

### `aggregate`（聚合）

- 快速使用指南展示 `homeURL/api/yggdrasil`。
- 保留 `/api/yggdrasil` 路由点。
- 元数据 `skinDomains` 依次列出 `homeURL/api/yggdrasil` 域名、API 域名。

在用户服务域名下额外挂载 `/api/yggdrasil` 路由点，把认证、会话与纹理请求反向代理到世界树服务，使习惯「单 URL」的老系统可以无缝迁移。该路由点与用户主页（homeURL）本身无关，仅作为兼容入口存在。

## 反向代理行为

- `/api/yggdrasil` 为透明反向代理：转发到固定的本机 API 端口，不接收外部目标。
- 该路由完全绕过用户端自身的会话、CSRF、邮箱与限流校验，使代理后的 API 与独立 Yggdrasil 服务**行为完全一致**。
- 该路由点无法被插件占用（`/api` 属于保留命名空间）。

## 反向代理配置

部署前请先在管理后台确定采用哪种模式，再据此配置反向代理（示例见[部署与运维 → 反向代理](../01-overview/deployment.md#反向代理)）：

- `standalone`：将 API 域名指到 `35577`，用户端域名指到 `35565`。
- `aggregate`：只需把唯一域名指到 `35565`，其 `/api/yggdrasil/*` 会由用户端内部代理到 `35577`。

> 返回[④ 世界树系统](README.md)或[文档总览](../README.md)。
