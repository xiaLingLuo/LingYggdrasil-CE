# 管理后台指南

管理后台运行在 **35599** 端口（**必须私密**），使用安装时创建的 Root 账户登录：

```text
http://<服务器地址>:35599/admin/login
```

> 部署/反向代理/端口安全见[部署与运维](../01-overview/deployment.md)；权限节点见[权限系统](permissions.md)。

## 导航与权限

侧边栏按权限节点显示，无权限的菜单不会出现。Root 拥有全部权限；其他管理员按权限组授权。无权限的页面/接口会被拒绝（401/403）。

2.10.0 主要入口：

| 入口 | 说明 |
|---|---|
| `/admin/dashboard` | 仪表盘 |
| `/admin/users` | 用户管理 |
| `/admin/profiles` | 角色管理 |
| `/admin/skins`、`/admin/capes` | **纹理名册**（皮肤 / 披风） |
| `/admin/texture-settings` | **纹理设置**（皮肤与披风共用） |
| `/admin/texture-blacklist` | **纹理 HASH 黑名单** |
| `/admin/system` | **系统设置** |
| `/admin/plugins` | 插件管理 |
| `/admin/audit` | 审计事件（新 API `/admin/api/audit`） |
| `/admin/texture-assets` | 旧入口，自动重定向到皮肤或披风名册 |

## 仪表盘

用户数、角色数、纹理资产等核心数据与快捷入口；组件支持拖拽排序。

## 用户管理

- 用户名册：分页（50/100/200/500/1000）与按用户名/邮箱/昵称搜索。
- 编辑邮箱/昵称、邮箱验证状态、用户权限组。
- 创建用户（用户名、邮箱、密码、昵称、验证状态）与删除用户。
- 用户资料、角色名与日志配置已统一迁移到系统设置；本页只保留用户与权限组操作。

## 角色管理

- 角色名册：按名称/UUID 搜索、分页。
- 修改名称、切换模型、删除角色。
- **事务化所有权转移**：`POST /admin/api/profiles/transfer`（body：`fromUserId`、`toUserId`）。转移会校验目标用户可访问、轮换世界树密钥、撤销相关会话、按新 owner 重校验纹理绑定，并写审计事件。


## 纹理名册（`/admin/skins`、`/admin/capes`）

- 分页/搜索资产：类型、尺寸、管理名称、作者（默认「佚名」）、首次上传信息。
- **修改管理名称**：`POST /admin/api/texture-assets/{id}/name`（管理名不影响用户别名）。
- **修改作者**：`POST /admin/api/texture-assets/{id}/author`（仅管理员可改；用户别名不覆盖作者）。
- **删除**：`POST /admin/api/texture-assets/{id}/delete`（物理删除资产、关联记录；无其他引用时删除原始文件，不可撤销）。
- **存储对账**：
  - `GET /admin/api/texture-storage/orphans` 扫描未注册文件/未引用 blob/缺失文件；
  - `POST /admin/api/texture-storage/cleanup` 清理（复核绑定/来源/注销数据，可重复执行）。
- 顶部子菜单在「皮肤名册」「披风名册」「纹理设置」「纹理黑名单」之间切换；
  纹理设置与系统管理的 `texture` 分区共用同一套 settings API。
- 纹理黑名单每行一条完整 SHA-256 HASH；命中后拒绝新上传，已有纹理不受影响。
- 旧 `/admin/texture-assets` 会自动重定向到有权限的名册页。

## 系统设置（`/admin/system`）

- 页面由 `GET /admin/api/settings/schema` 动态渲染，分区为：
  `site`（基础与公告）、`identity`（注册与访问）、`mail`（邮件）、
  `logging`（日志与审计）、`texture`（纹理设置）、`legacy`（其他）。
- `GET /admin/api/settings` 列出全部组与 revision；`GET /admin/api/settings/{group}` 读取单组。
- 保存：`PUT /admin/api/settings/{group}`，body `{revision, values}`。
- **乐观锁**：revision 不匹配返回 `409 REVISION_CONFLICT`，页面提示刷新后重试。
- 每次成功提交写审计事件并立即应用运行时配置；域名/CORS 等少数设置可能需重启完全生效。
- SMTP 密码、世界树私钥等敏感值写入 `secret_store`，API 不回显明文。
- 迁移后旧 `system_settings` 会按组导入，`legacy` 组保留未识别键。

## 审计

- `GET /admin/api/audit`（分页/过滤）查询审计事件：actor、action、target、result、reason、correlation_id、occurred_at。
- 关键操作（设置提交、角色转移、敏感管理动作）自动写入；敏感操作同时记录业务日志。

## 插件管理

- 查看插件列表、状态、版本、作者、描述；启停/重载。
- 支持传统 `plugin.yml` 插件、新 `plugin-spi` 插件（`plugins/spi/`）与旧 `plugin.api` 桥接插件（`plugins/legacy/`）。
- 插件运行在受控上下文（storage/scheduler/logger），详见[插件系统](../05-plugins/management.md)。

## 权限组 / Root / 邮件 / 公告 / 日志

- **权限组**：管理员/用户权限组，节点按来源（内置/插件）分组；`cancelled` 为内置不可删组。
- **Root**：完整性保护（`root_info` 必须恰好一条）；异常会锁定管理端。
- **邮件**：SMTP 配置与测试发送（密码可用 `ENC:` 加密存储）。
- **公告/备案**：站点展示配置。
- **日志**：业务/审计/插件日志查询与保留策略。

## 安全建议

1. 35599 仅内网/白名单/SSH 隧道访问；
2. Root 使用强密码并定期更换；
3. 设置、用户、角色、资产等关键操作定期检查审计；
4. 生产变更前备份数据库与 `.lingkey`；
