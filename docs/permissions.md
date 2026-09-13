# 权限系统

泠 Yggdrasil 拥有两套独立的权限组：

- **管理员权限组**：控制管理后台（35599）的菜单可见性与操作权限。
- **用户权限组**：控制用户端（35565）功能的可用性。

Root 账户不隶属于任何管理员权限组，始终拥有全部权限。

> 权限组的创建与编辑在管理后台完成，见[管理后台指南](admin-guide.md#权限组仅-root)。

## 管理员权限组

### 权限列表

| 分类       | 权限键                    | 说明                |
|------------|---------------------------|---------------------|
| 仪表盘     | `admin.dashboard.view`    | 查看仪表盘          |
| 用户       | `admin.users.view`        | 查看用户            |
| 用户       | `admin.users.create`      | 创建用户            |
| 用户       | `admin.users.edit`        | 编辑用户            |
| 用户       | `admin.users.ban`         | 封禁用户            |
| 用户       | `admin.users.verify`      | 管理邮箱验证状态    |
| 用户       | `admin.users.delete`      | 删除用户            |
| 角色       | `admin.profiles.view`     | 查看角色            |
| 角色       | `admin.profiles.create`   | 创建角色            |
| 角色       | `admin.profiles.edit`     | 编辑角色            |
| 角色       | `admin.profiles.transfer` | 转移角色所有权      |
| 角色       | `admin.profiles.reset`    | 重置角色材质        |
| 角色       | `admin.profiles.delete`   | 删除角色            |
| 皮肤       | `admin.skins.view`        | 查看皮肤            |
| 皮肤       | `admin.skins.download`    | 下载皮肤            |
| 皮肤       | `admin.skins.alias`       | 修改皮肤别名        |
| 皮肤       | `admin.skins.delete`      | 删除皮肤            |
| 皮肤       | `admin.skins.orphans`     | 清理无主皮肤        |
| 披风       | `admin.capes.view`        | 查看披风            |
| 披风       | `admin.capes.download`    | 下载披风            |
| 披风       | `admin.capes.alias`       | 修改披风别名        |
| 披风       | `admin.capes.delete`      | 删除披风            |
| 披风       | `admin.capes.orphans`     | 清理无主披风        |
| 管理员     | `admin.admins.view`       | 查看管理员          |
| 管理员     | `admin.admins.create`     | 创建管理员          |
| 管理员     | `admin.admins.edit`       | 编辑管理员          |
| 管理员     | `admin.admins.delete`     | 删除管理员          |
| 权限组     | `admin.groups.view`       | 查看管理员权限组    |
| 权限组     | `admin.groups.edit`       | 编辑管理员权限组    |
| 用户权限组 | `admin.usergroups.view`   | 查看用户权限组      |
| 用户权限组 | `admin.usergroups.edit`   | 编辑用户权限组      |
| 安全       | `admin.security.view`     | 查看安全设置        |
| 安全       | `admin.security.edit`     | 修改安全设置        |
| 世界树     | `admin.yggdrasil.view`    | 查看 Yggdrasil 设置 |
| 世界树     | `admin.yggdrasil.edit`    | 修改 Yggdrasil 设置 |
| 世界树     | `admin.yggdrasil.keys`    | 生成/切换密钥       |
| 系统       | `admin.system.view`       | 查看系统设置        |
| 系统       | `admin.system.edit`       | 修改系统设置        |
| 应用信息   | `admin.appinfo.view`      | 查看应用信息        |

### 高危权限

以下权限允许持有者提升自己或他人为近乎 Root 的账户，界面上以红色警告图标标注，请谨慎授予：

- `admin.admins.create`
- `admin.admins.edit`
- `admin.admins.delete`
- `admin.groups.edit`
- `admin.usergroups.edit`

### 内置管理员组

| 组名 | 权限                  | 说明             |
|------|-----------------------|------------------|
| `op` | `*`（通配，全部权限） | 内置运维管理员组 |

### 组名规则与别名

- 组名必须匹配 `^[a-z0-9]+$`（仅小写字母与数字）。
- 组名**创建后不可修改**；如需改名，请删除后重建。
- 显示别名由语言包中的 `permGroup.<组名>` 定义（缺失时回退为组名本身），不在后台编辑。

## 用户权限组

### 权限列表

| 分类 | 权限键            | 说明                                   |
|------|-------------------|----------------------------------------|
| 访问 | `user.accessible` | 是否允许访问用户端（未授予则整体拒绝） |
| 功能 | `user.profiles`   | 角色管理                               |
| 功能 | `user.skins`      | 皮肤管理                               |
| 功能 | `user.capes`      | 披风管理                               |
| 功能 | `user.friends`    | 好友系统                               |
| 功能 | `user.world`      | 材质库（世界树）                       |
| 功能 | `user.settings`   | 账户设置                               |

### 内置用户组

| 组名      | 权限                  | 说明                   |
|-----------|-----------------------|------------------------|
| `default` | `*`（通配，全部权限） | 默认用户组             |
| `banned`  | 无                    | 封禁组，不可修改或删除 |

> `user.accessible` 是用户端的总开关。若某用户所在组未授予该权限，则无法访问用户端及其 Yggdrasil 认证。

## 使用建议

- 遵循最小权限原则，仅授予管理员完成工作所需的权限。
- 定期审查权限组成员，尤其是持有高危权限的账户。
- 对普通用户，若需关闭某项功能（如材质库），从 `default` 组中移除对应权限，或为其分配自定义组。
