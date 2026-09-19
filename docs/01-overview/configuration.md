# 配置参考

泠 Yggdrasil 的配置分为三部分：

1. **`config.yml`**：运行期配置，首次启动自动生成。
2. **`sql.yml`**：数据库连接与邮件服务器。
3. **系统设置**：保存在数据库 `system_settings` 表中，通过管理后台「系统管理」页面修改。

> 修改 `config.yml` 或 `sql.yml` 后需重启程序。

## 一、`config.yml`

首次启动时会在运行目录自动生成，可手动编辑；修改后重启生效。

```yaml
services:
  user:                 # 用户端服务
    enabled: true
    ip: 0.0.0.0
    port: 35565
    logRetentionDays: 30
  yggdrasil:            # 世界树 API 服务
    enabled: true
    ip: 0.0.0.0
    port: 35577
    logRetentionDays: 30
  admin:                # 管理后台服务
    enabled: true
    ip: 0.0.0.0
    port: 35599
    logRetentionDays: 30
logging:
  level: INFO           # TRACE | DEBUG | INFO | WARN | ERROR
  audit:                # 审计日志
    enabled: true
    retentionDays: 90
  pluginSystem:         # 插件系统日志
    enabled: true
    retentionDays: 30
```

| 字段                                 | 说明                                |
|--------------------------------------|-------------------------------------|
| `services.<name>.enabled`            | 是否启动该服务                      |
| `services.<name>.ip`                 | 监听地址                            |
| `services.<name>.port`               | 监听端口                            |
| `services.<name>.logRetentionDays`   | 该服务日志文件最多保留的天数（≥ 1） |
| `logging.level`                      | 全局日志等级                        |
| `logging.audit.enabled`              | 是否记录审计日志                    |
| `logging.audit.retentionDays`        | 审计日志保留天数（≥ 1）             |
| `logging.pluginSystem.enabled`       | 是否记录插件系统日志                |
| `logging.pluginSystem.retentionDays` | 插件系统日志保留天数（≥ 1）         |

## 二、`sql.yml`

### 数据库

```yaml
database:
  type: sqlite          # sqlite | mysql | pgsql
  sqlitePath: ./data.db # type=sqlite 时使用
  host: 127.0.0.1       # type=mysql/pgsql 时使用
  port: 3306
  database: lingyggdrasil
  username: root
  password: the-password    # 保存时自动加密
```

| 字段                    | 说明                            |
|-------------------------|---------------------------------|
| `type`                  | `sqlite`、`mysql`、`pgsql` 之一 |
| `sqlitePath`            | SQLite 数据库文件路径           |
| `host` / `port`         | 数据库主机与端口                |
| `database`              | 数据库名                        |
| `username` / `password` | 数据库账号                      |

> ⚠️ **PostgreSQL 暂不可用**：由于开发维护人手不足，因此自2.1.0版本起，PgSQL不再提供支持。未来可能考虑重启支持。

### 邮件

```yaml
mail:
  enabled: false
  host: smtp.example.com
  port: 465
  username: noreply@example.com
  password: the-password
  from: noreply@example.com
  tls: true
```

| 字段                    | 说明                                      |
|-------------------------|-------------------------------------------|
| `enabled`               | 是否启用邮件服务                          |
| `host` / `port`         | SMTP 主机与端口（常见：465 SSL、587 TLS） |
| `username` / `password` | SMTP 登录账号或授权码                     |
| `from`                  | 发件人地址                                |
| `tls`                   | 是否使用 TLS/SSL                          |

> 邮件服务器配置建议在管理后台「系统管理 → 邮箱服务器配置」中修改，保存后写回 `sql.yml`。

## 三、系统设置

以下设置位于管理后台「系统管理」页面，按卡片分组。每张卡片有独立的保存按钮，修改后即时生效。

### 站点信息

| 设置     | 键                 | 说明                           |
|----------|--------------------|--------------------------------|
| 站点名称 | `site_name`        | 显示在导航栏、页脚、邮件等位置 |
| 站点描述 | `site_description` | 站点副标题                     |

### 功能开关

| 设置     | 键                           | 默认 | 说明                          |
|----------|------------------------------|------|-------------------------------|
| 开放注册 | `registration_enabled`       | 是   | 关闭后禁止新用户注册          |
| 邮箱验证 | `email_verification_enabled` | 否   | 开启后注册/改邮箱需邮箱验证码 |

### 域名

| 设置       | 键              | 说明                                                 |
|------------|-----------------|------------------------------------------------------|
| 用户端域名 | `user_domain`   | 用户端外部访问地址                                   |
| 管理端域名 | `admin_domain`  | 管理端外部访问地址                                   |
| API 域名   | `api_domain`    | Yggdrasil API 外部访问地址（也用于生成纹理公开 URL） |
| 通用域名   | `common_domain` | 通用外部访问地址                                     |

### 邮箱域名控制

| 设置     | 键                  | 说明                                                                  |
|----------|---------------------|-----------------------------------------------------------------------|
| 域名列表 | `email_domain_list` | 每行一个域名，支持通配符 `*`（如 `*.evil.com`）                       |
| 过滤模式 | `email_domain_mode` | `blacklist`（黑名单，命中拒绝）或 `whitelist`（白名单，仅允许列表内） |

### 邮箱服务器配置

包含 SMTP 连接信息（见上文 `sql.yml` 的 mail 段）以及：

| 设置             | 键                              | 说明                                      |
|------------------|---------------------------------|-------------------------------------------|
| 注册验证邮件内容 | `mail_template_verify`          | 支持 HTML，使用 `{code}` 占位符表示验证码 |
| 邮箱变更邮件内容 | `mail_template_email_change`    | 同上                                      |
| 密码变更邮件内容 | `mail_template_password_change` | 同上                                      |
| 测试邮件内容     | `mail_test_content`             | 「发送测试邮件」使用的正文                |

邮箱服务器配置分为 **5 个独立保存区域**，各自有独立的保存按钮：

1. 连接配置（邮箱验证开关、邮件服务开关、SMTP 主机/端口/用户名/密码、发件人、TLS）
2. 测试邮件（目标邮箱、发送测试邮件、测试邮件内容）
3. 注册验证邮件内容
4. 邮箱变更邮件内容
5. 密码变更邮件内容

### 名称黑名单

用户名、角色名、皮肤名、披风名各自独立配置，均支持通配符 `*` 与「大小写严格」选项。

| 设置         | 键                                                                 |
|--------------|--------------------------------------------------------------------|
| 用户名黑名单 | `username_blacklist` / `username_blacklist_case_sensitive`         |
| 角色名黑名单 | `profile_name_blacklist` / `profile_name_blacklist_case_sensitive` |
| 皮肤名黑名单 | `skin_name_blacklist` / `skin_name_blacklist_case_sensitive`       |
| 披风名黑名单 | `cape_name_blacklist` / `cape_name_blacklist_case_sensitive`       |

> 用户名、角色名命中即拒绝；皮肤/披风**别名**命中即拒绝；皮肤/披风**原名**命中时静默替换为 `x`（保留扩展名），不中断上传。

### 皮肤 / 披风设置

| 设置           | 键                                            | 默认              | 说明                 |
|----------------|-----------------------------------------------|-------------------|----------------------|
| 单文件大小上限 | `skin_max_size` / `cape_max_size`             | 64                | 单位 KB              |
| 数量上限       | `skin_max_count` / `cape_max_count`           | 10                | 每个用户可拥有的数量 |
| 总大小上限     | `skin_max_total_size` / `cape_max_total_size` | 640               | 单位 KB              |
| 频率限制       | `skin_rate_limit` / `cape_rate_limit`         | 24                | 每周期操作次数上限   |
| 存储路径       | `skin_storage_path` / `cape_storage_path`     | `skins` / `capes` | 纹理存储目录         |
| 允许下载       | `allow_download_skin` / `allow_download_cape` | 是                | 是否允许下载纹理     |

### 加密等级

| 设置     | 键                 | 默认 | 说明                                |
|----------|--------------------|------|-------------------------------------|
| 加密等级 | `encryption_level` | 1    | 1–6，等级越高密码哈希越强、耗时越长 |

加密等级通过管理后台「安全设置」以可视化卡片调整。修改只影响**之后**创建或修改的密码，已有密码保持原强度。

### Yggdrasil / 世界树设置

| 设置              | 键                       | 默认       | 说明                                                |
|-------------------|--------------------------|------------|-----------------------------------------------------|
| 签名模式          | `signature_mode`         | `rsa-sha1` | `ed448`（现代） / `rsa-sha512` / `rsa-sha1`（兼容） |
| 临时 Token 有效期 | `token_temp_expiry`      | 4320       | 单位分钟                                            |
| 永久 Token 有效期 | `token_permanent_expiry` | 10080      | 单位分钟                                            |
| 单角色 Token 上限 | `max_tokens_per_profile` | 12         |                                                     |
| 认证频率限制      | `auth_rate_limit`        | 1000       |                                                     |
| 批量查询上限      | `batch_query_max_count`  | 6          | 批量按名查询角色的单次上限                          |
| UUID 版本         | `uuid_version`           | `v4`       | 角色 UUID 生成版本                                  |

> 切换签名模式或重新生成密钥后，已下发的 Token 可能需要重新登录。

### 用户限制

| 设置           | 键                      | 默认 | 说明 |
|----------------|-------------------------|------|------|
| 单用户角色上限 | `max_profiles_per_user` | 10   |      |
| 单 IP 注册上限 | `max_accounts_per_ip`   | 3    |      |
| 拉黑上限       | `max_blocked_users`     | 2000 |      |
| 收藏上限       | `max_favorites`         | 32   |      |

### 公告

| 设置     | 键                     | 说明                             |
|----------|------------------------|----------------------------------|
| 公告模式 | `announcement_mode`    | 展示方式（关闭 / 弹窗 / 横幅等） |
| 生效区域 | `announcement_scope`   | 用户端 / 管理端 / 全部           |
| 公告内容 | `announcement_content` | 支持 Markdown 子集               |

### 备案信息

| 设置       | 键                       | 说明       |
|------------|--------------------------|------------|
| ICP 备案号 | `icp_record`             | 显示在页脚 |
| 公安备案号 | `public_security_record` | 显示在页脚 |

### 用户操作日志

| 设置               | 键                                          | 默认 | 说明                                |
|--------------------|---------------------------------------------|------|-------------------------------------|
| 启用操作日志       | `user_action_log_enabled`                   | 是   |                                     |
| 单用户日志大小上限 | `user_action_log_max_kib`                   | 100  | 单位 KB，最小 8                     |
| 保留天数           | `user_action_log_retention_days`            | 30   | 最小 1                              |
| 记录的操作类型     | `user_action_log_actions`                   | 全部 | CSV，如 `login,profile,texture,...` |
| 下载间隔           | `user_action_log_download_interval_minutes` | 480  | 单位分钟，最小 1                    |

用户操作日志写入数据库表 `user_logs`，格式为：

```text
[yyyy-MM-dd HH:mm:ss]<操作> 于 <IP>
```

### 其他

| 设置     | 键                 | 说明                                       |
|----------|--------------------|--------------------------------------------|
| 默认语言 | `default_language` | 见「多语言」支持的语言代码（默认 `zh-CN`） |
| 遥测上报 | `treasure_enabled` | 是否启用匿名遥测（默认关闭）               |
| 安装时间 | `installed_at`     | 安装时间戳                                 |

### 名称黑名单通配符

用户名、角色名、皮肤/披风名的黑名单均支持通配符 `*`：

```text
admin*
*test*
badword
```

- 用户名、角色名：命中即拒绝。
- 皮肤/披风**别名**：命中即拒绝。
- 皮肤/披风**原名**：命中时静默替换为 `x`（保留扩展名），不中断上传。

可启用「大小写严格」模式以区分大小写。

> 多语言与外部语言包的详细说明见[用户端界面自定义 → 多语言](../02-user/customization.md#多语言)与[管理端界面自定义 → 多语言](../03-admin/customization.md#多语言)。
