# 配置参考

配置分为四层：

1. **`config.yml`**：服务监听、外网域名、日志（首次启动生成，修改后重启生效）。
2. **`sql.yml`**：数据库连接与邮件配置（安装向导写入，密码 `ENC:` 加密）。
3. **数据库中的系统设置**（`settings` / `settings_revision`）：站点、协议、纹理、身份、日志、邮件等业务设置，通过管理端 `/admin/system` 修改。
4. **环境/启动参数**：JVM 内存、时区与运行参数。

> 修改 `config.yml`/`sql.yml` 后需重启；系统设置保存后生成不可变快照，`SystemConfig` 立即消费（部分 CORS/域名类设置在重启后完全生效）。

## 一、`config.yml`

```yaml
services:
  user:
    enabled: true
    domain: http://127.0.0.1:35565     # 用户端对外基础 URL
    commonDomain: http://127.0.0.1:35565
    ip: 0.0.0.0
    port: 35565
    logRetentionDays: 30
  yggdrasil:
    enabled: true
    domain: http://127.0.0.1:35577     # API 对外基础 URL
    ip: 0.0.0.0
    port: 35577
    logRetentionDays: 30
  admin:
    enabled: true
    domain: http://127.0.0.1:35565
    ip: 0.0.0.0
    port: 35599
    logRetentionDays: 30
logging:
  level: INFO                           # TRACE | DEBUG | INFO | WARN | ERROR
  audit:
    enabled: true
    retentionDays: 90
  pluginSystem:
    enabled: true
    retentionDays: 30
```

| 字段 | 说明 |
|---|---|
| `services.<name>.enabled` | 是否启动该宿主（可单独关闭；关闭后其余宿主继续运行） |
| `services.<name>.domain` | 该宿主的对外基础 URL（含协议；用于生成绝对链接/纹理 URL） |
| `services.user.commonDomain` | 通用回退域名，未单独配置时复用 |
| `services.<name>.ip` / `port` | 监听地址/端口；`0.0.0.0` 表示所有网卡 |
| `services.<name>.logRetentionDays` | 业务日志保留天数 |
| `logging.level` | 全局日志级别 |
| `logging.audit.*` | 审计日志开关与保留天数 |
| `logging.pluginSystem.*` | 插件系统日志开关与保留天数 |

端口冲突时启动会失败并记录日志；管理端端口（默认 35599）与安装端口（35598）不应公开。

## 二、`sql.yml`

安装向导生成，包含数据库与邮件：

```yaml
database:
  type: sqlite            # sqlite | mysql
  host: 127.0.0.1         # mysql 时生效
  port: 3306
  database: lingyggdrasil
  username: ly
  password: ENC:...       # 安装时加密写入
mail:
  enabled: false
  host: smtp.example.com
  port: 587
  username: ""
  password: ENC:...
  from: noreply@example.com
  ssl: true
```

- **SQLite**：`sqlitePath`（安装时指定，默认 `./data.db`）。
- **MySQL/MariaDB**：`type: mysql`，连接参数如上；连接参数按实际环境填写。
- **PostgreSQL**：驱动随包但方言**明确拒绝**，不要配置为 `pgsql`。
- 密码字段支持 `ENC:` 前缀（服务端 `.lingkey` 加密）；不要手工改为明文后随意外传。
- 修改数据库配置前请备份数据；切换数据库不会自动搬迁数据。

## 三、系统设置（`settings` / `settings_revision`）

业务设置不再写入旧的 `system_settings`，而是保存在：

| 表 | 作用 |
|---|---|
| `settings(group_name, setting_key, value_json, updated_at)` | 每组每键的值 |
| `settings_revision(group_name, revision, updated_at)` | 组级 revision，用于乐观锁 |

行为：

- 管理端 `GET /admin/api/settings` 列出全部组；`GET /admin/api/settings/{group}` 读取组；`PUT` 提交 `{revision, values}`。
- revision 不匹配返回 **409 `REVISION_CONFLICT`**，需刷新后重试。
- 每次提交写入审计事件（`audit_events`），并生成 `SystemConfig` 新快照。
- 旧 2.3.16 的 `system_settings` 会在升级迁移时按组导入，并保留 `legacy` 组。

主要设置组（管理端按组展示）：

| 组 | 内容示例 |
|---|---|
| `site` | 站点名称/描述/关键词、公告、备案与外网域名 |
| `texture` | 皮肤/披风上传大小、数量、存储路径、下载与收藏限制 |
| `identity` | 注册与邮箱验证、用户名/角色名黑名单、账号与角色数量限制 |
| `logging` | 操作日志容量、保留、记录范围 |
| `mail` | SMTP 参数、邮件模板（密码存于 `secret_store`） |
| `legacy` | 旧版迁移保留组 |

> 设置由 schema 定义；未知键被拒绝，字段类型/范围由后端校验。生产变更建议在低峰进行并保留审计记录。

## 四、秘密与密钥

| 位置 | 说明 |
|---|---|
| `secret_store` | AES-GCM 秘密（名称/密文/摘要/密钥版本/AAD），由 `SecretStore` 管理 |
| `profile_credentials` | 世界树密钥密文（SHA-256 摘要 + 密钥版本），旧 token 仅兼容回退 |
| `sql.yml` 中 `ENC:` | 配置文件密码加密值 |
| `.lingkey` | 服务端本地主密钥文件（勿泄露、勿提交仓库；迁移/恢复需一并备份） |

## 五、环境变量与启动参数

| 变量/参数 | 用途 |
|---|---|
| `JAVA_HOME` | Java 25 路径 |
| `-Xms/-Xmx` | JVM 堆大小（建议 `-Xms512M -Xmx2G`） |
| `-Dfile.encoding=UTF-8` | 强制 UTF-8（Windows 建议显式指定） |

## 六、相关文件

| 文件 | 说明 |
|---|---|
| `config.yml` | 服务/日志（手工编辑） |
| `sql.yml` | 数据库/邮件（安装向导） |
| `logs/` | 业务、审计、插件日志 |
| `i18n/` | 外部语言包覆盖（可选） |
| `plugins/` | 插件目录（`spi/`、`legacy/`、传统 `plugin.yml` 插件） |
| `skins/`、`capes/` | 纹理文件存储 |

