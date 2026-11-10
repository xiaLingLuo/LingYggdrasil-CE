# 常见问题

未找到答案？查看[部署与运维](deployment.md)、[配置参考](configuration.md)或[安全说明](../06-security/security.md)，也可提交 Issue。

## 启动

### 启动报 `UnsupportedClassVersionError`
运行时低于 Java 25。升级到 Java 25（构建目标为 `--release 25`）。

### 端口被占用
35565/35577/35599/35598 任一被占用会导致对应宿主启动失败：

```bash
# Linux
ss -lntp | grep -E '35565|35577|35599|35598'
# Windows
netstat -ano | findstr 35565
```

停止占用进程，或修改 `config.yml` 中对应服务端口后重启。

### 35598 安装向导打不开
已安装完成（数据目录存在 `.INSTALLED`）时不会启动安装向导；直接使用 35565（用户端）与 35599（管理端）。

### 如何重新安装
1. 停止程序并备份数据；
2. 移走 `sql.yml`、`config.yml`、`.INSTALLED`、`.lingkey`（以及 SQLite 的 `data.db`）；
3. 重启进入安装向导。

### 忘记 Root 密码
Root 密码无法在界面找回。请用备份恢复，或在数据库 `root_info` 表中按程序 Argon2 参数写入新密码哈希（操作前务必备份；删除 Root 记录会触发完整性保护并锁定管理端）。

### 管理端提示「数据库核心遭到篡改」
`root_info` 记录数不为 1。恢复为恰好一条记录后重启。

## 数据库

### 是否支持 MySQL？
支持。安装向导可直接选择 MySQL/MariaDB，配置写入 `sql.yml`。PostgreSQL 驱动随包但方言被明确拒绝。

### 数据库连接失败
- 检查 `sql.yml`（host/port/database/username/password）。
- 确认数据库已启动、网络可达、账号拥有该库权限；MySQL 需提前 `CREATE DATABASE`。
- SQLite 需运行目录可写。

### 修改数据库/邮件密码后服务异常
`sql.yml` 中密码为 `ENC:` 加密值，依赖运行目录的 `.lingkey`。丢失/替换 `.lingkey` 将无法解密，需要重新填写密码并重启；请将 `.lingkey` 纳入备份。

### 数据库表名前缀为什么没有 `ly_`？

## 设置与版本化配置

### 修改设置后何时生效？
管理端 `/admin/system` 按组保存后立即生成配置快照；域名/CORS 类设置可能需重启完全生效。

### 保存设置提示 409 / REVISION_CONFLICT
这是系统设置的乐观锁：页面数据已过时。刷新页面重新读取最新 revision 后再提交。每次提交都会写审计事件。

### 旧版 `system_settings` 去哪了？
升级时按组导入到 `settings`/`settings_revision`，并保留 `legacy` 组。新设置请统一在 `/admin/system` 修改。

## 认证与客户端

### Minecraft 客户端无法登录
1. 认证地址：`default`/`standalone` 填 API 根地址（35577）；`aggregate` 填 `https://<用户域名>/api/yggdrasil`。
2. 用户名填**角色名**（不是账号名），密码填该角色的**世界树密钥**。
3. 确认用户未封禁、邮箱已验证（若启用）、权限组包含 `user.accessible`。
4. 查看日志中的认证错误。

### 提示 Invalid credentials
角色名或世界树密钥错误；用户被移入封禁组；邮箱未验证；密钥已被轮换/角色已转移。

### 皮肤/披风不显示
- 确认 API 域名/通用域名正确，且纹理 URL（`/textures/{type}/{hash}` 或聚合模式下 `<用户域名>/api/yggdrasil/textures/...`）公网可达。
- 纹理需已发布（publication）或属于当前角色的合法绑定。
- 确认签名模式与客户端兼容。

### 如何查看/更换世界树密钥
用户端角色卡片 → 编辑弹窗 → 世界树密钥（默认虚化，可复制/轮换）。轮换后旧密钥立即失效，需要更新启动器密码。

## 部署与网络

### 公网打不开 35565/35577
检查云安全组/防火墙是否放行；管理端 35599 与安装端 35598 不应放行公网。反向代理需正确传递 `Host`、`X-Forwarded-For`、`X-Forwarded-Proto`。

### 反向代理后日志 IP 都是代理地址
确保代理覆盖 `X-Forwarded-For` / `X-Real-IP`，且程序信任该代理来源。

### 跨域失败
确认 `config.yml` 中三个服务域名均为正确对外地址；CORS 白名单启动时加载，修改需重启。

### 自定义图标/语言包不生效
- 图标：运行目录 `icons/`（启动时检测，需重启）。
- 语言包：运行目录 `i18n/`，外部与内置深度合并、外部优先；`lang.json` 控制可选语言。改完重启或强刷。

## 其他

### 程序会自动创建哪些文件/目录
`config.yml`、`sql.yml`、`.INSTALLED`、`.lingkey`、SQLite `data.db`、`skins/`、`capes/`、`plugins/`、`logs/`、`i18n/`。`icons/` 需手动创建。

### 如何关闭遥测
遥测默认关闭；可在管理端设置中确认「遥测上报」开关。

### 支持哪些客户端？
兼容 authlib-injector 体系的主流启动器（HMCL/PCL 等）。
