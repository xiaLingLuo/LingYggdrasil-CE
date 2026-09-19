# 管理员权限系统

管理员权限组控制管理后台（35599）的菜单可见性与操作权限，与[用户权限系统](../02-user/permissions.md)相互独立。

Root 账户不隶属于任何管理员权限组，始终拥有全部权限。

> 权限组的创建与编辑在管理后台完成，见[管理后台指南](guide.md#权限组仅-root)。

## 权限列表

| 分类       | 权限键                      | 说明                |
|------------|-----------------------------|---------------------|
| 仪表盘     | `admin.dashboard.view`      | 查看仪表盘          |
| 用户       | `admin.users.view`          | 查看用户            |
| 用户       | `admin.users.create`        | 创建用户            |
| 用户       | `admin.users.edit`          | 编辑用户            |
| 用户       | `admin.users.ban`           | 封禁用户            |
| 用户       | `admin.users.verify`        | 管理邮箱验证状态    |
| 用户       | `admin.users.delete`        | 删除用户            |
| 角色       | `admin.profiles.view`       | 查看角色            |
| 角色       | `admin.profiles.create`     | 创建角色            |
| 角色       | `admin.profiles.edit`       | 编辑角色            |
| 角色       | `admin.profiles.transfer`   | 转移角色所有权      |
| 角色       | `admin.profiles.reset`      | 重置角色材质        |
| 角色       | `admin.profiles.delete`     | 删除角色            |
| 皮肤       | `admin.skins.view`          | 查看皮肤            |
| 皮肤       | `admin.skins.download`      | 下载皮肤            |
| 皮肤       | `admin.skins.alias`         | 修改皮肤别名        |
| 皮肤       | `admin.skins.delete`        | 删除皮肤            |
| 皮肤       | `admin.skins.orphans`       | 清理无主皮肤        |
| 披风       | `admin.capes.view`          | 查看披风            |
| 披风       | `admin.capes.download`      | 下载披风            |
| 披风       | `admin.capes.alias`         | 修改披风别名        |
| 披风       | `admin.capes.delete`        | 删除披风            |
| 披风       | `admin.capes.orphans`       | 清理无主披风        |
| 管理员     | `admin.admins.view`         | 查看管理员          |
| 管理员     | `admin.admins.create`       | 创建管理员          |
| 管理员     | `admin.admins.edit`         | 编辑管理员          |
| 管理员     | `admin.admins.delete`       | 删除管理员          |
| 权限组     | `admin.groups.view`         | 查看管理员权限组    |
| 权限组     | `admin.groups.edit`         | 编辑管理员权限组    |
| 用户权限组 | `admin.usergroups.view`     | 查看用户权限组      |
| 用户权限组 | `admin.usergroups.edit`     | 编辑用户权限组      |
| 安全       | `admin.security.view`       | 查看安全设置        |
| 安全       | `admin.security.edit`       | 修改安全设置        |
| 世界树     | `admin.yggdrasil.view`      | 查看 Yggdrasil 设置 |
| 世界树     | `admin.yggdrasil.edit`      | 修改 Yggdrasil 设置 |
| 世界树     | `admin.yggdrasil.keys`      | 生成/切换密钥       |
| 系统       | `admin.system.view`         | 查看系统设置        |
| 系统       | `admin.system.edit`         | 修改系统设置        |
| 应用信息   | `admin.appinfo.view`        | 查看应用信息        |
| 插件管理   | `admin.plugin.overall.view` | 查看插件总览        |
| 插件管理   | `admin.plugin.overall.edit` | 启停/重载插件       |

## 高危权限

以下权限允许持有者提升自己或他人为近乎 Root 的账户，界面上以红色警告图标标注，请谨慎授予：

- `admin.admins.create`
- `admin.admins.edit`
- `admin.admins.delete`
- `admin.groups.edit`
- `admin.usergroups.edit`

## 内置管理员组

| 组名 | 权限                  | 说明             |
|------|-----------------------|------------------|
| `op` | `*`（通配，全部权限） | 内置运维管理员组 |

## 组名规则与别名

- 组名必须匹配 `^[a-z0-9]+$`（仅小写字母与数字）。
- 组名**创建后不可修改**；如需改名，请删除后重建。
- 显示别名由语言包中的 `permGroup.<组名>` 定义（缺失时回退为组名本身），不在后台编辑。

## 动态权限节点

权限节点由来源（source）动态注册，权限组编辑页面按**来源分组**展示：

- 内置节点来源为 `LingYggdrasil`，组内再按分类细分。
- 插件可通过 `plugin.yml` 的 `perms` 声明自己的节点，来源为插件的 `friendlyName`（未设置时用 `name`），直接平铺展示。

详见[插件开发手册](../07-plugin-dev/guide.md#5-权限节点声明)。

## 使用建议

- 遵循最小权限原则，仅授予管理员完成工作所需的权限。
- 定期审查权限组成员，尤其是持有高危权限的账户。
