# 快速开始

本页帮助你从零开始运行泠 Yggdrasil。

> 已部署完成？跳转到[部署与运维](deployment.md)、[配置参考](configuration.md)或[管理后台指南](../03-admin/guide.md)。

## 1. 环境要求

| 项目 | 要求 |
|---|---|
| 操作系统 | Linux / Windows / macOS |
| Java | **Java 25（必须）**；构建产物为 `--release 25`，低版本无法启动 |
| 内存 | 至少 512 MB；生产建议 1–2 GB |
| 磁盘 | 视纹理数量而定，建议预留 1 GB 以上 |
| 数据库 | SQLite（默认、零配置）或 MySQL / MariaDB 8.0+ |

检查 Java：

```bash
java --version
```

未安装 Java 25 参考[部署与运维 · 安装 Java 25](deployment.md#安装-java-25)。

## 2. 下载与放置

从 Releases 获取：

```text
LingYggdrasil-2.10.0.jar
```

将 JAR 放入一个**独立空目录**（该目录会成为数据目录：数据库、配置、`skins/`、`capes/`、`plugins/`、`logs/` 等）。

## 3. 首次启动

```bash
java -Xms512M -Xmx2G -jar LingYggdrasil-2.10.0.jar
```

首次启动不会进入正常服务，而是打开**安装向导**：

```text
http://<服务器IP或域名>:35598
```

安装向导需要填写：

| 字段 | 说明 |
|---|---|
| 管理员用户名 / 密码 / 邮箱 | Root 管理账户（密码强度受校验） |
| 用户端地址（userDomain） | 如 `https://mc.example.com` 或 `http://<IP>:35565` |
| API 地址（apiDomain） | 如 `https://api.example.com` 或 `http://<IP>:35577` |
| 数据库类型 | `sqlite` 或 `mysql` |
| SQLite 路径 | 选择 SQLite 时生效，默认 `./data.db` |
| MySQL 连接 | host / port / database / username / password（选择 MySQL 时生效） |
| 邮件 | 可关闭；开启后配置 SMTP |

> 安装过程中会初始化 2.10.0 数据模型与旧结构兼容表。**生产环境请勿把 35599/35598 暴露公网。**

## 4. 重启并访问

安装成功后按页面提示重启程序：

```bash
java -Xms512M -Xmx2G -jar LingYggdrasil-2.10.0.jar
```

| 入口 | 地址 | 说明 |
|---|---|---|
| 用户端 | `http://<IP>:35565` | 注册/登录/角色/纹理/好友 |
| Yggdrasil API | `http://<IP>:35577` | 启动器认证地址（default/standalone 模式） |
| 管理后台 | `http://<IP>:35599/admin/login` | Root 账户登录 |

## 5. 创建角色并接入启动器

1. 用户端注册账户并登录。
2. 在「角色」页创建角色（Profile），系统生成 UUID。
3. 在角色编辑弹窗中获取**世界树密钥**（默认虚化，可复制/轮换）。
4. 启动器选择「自定义 Yggdrasil / 外置登录」：
   - `default` / `standalone`：API 根地址（`http://<IP>:35577` 或反代域名）
   - `aggregate`：`https://<用户域名>/api/yggdrasil`
5. 用户名填**角色名**，密码填**世界树密钥**。

## 6. 下一步

- 上传皮肤/披风并绑定到角色：用户端「纹理」页（`/skins`、`/capes`）。
- 好友与分享：`/friends`、`/shared`；公共画廊：`/world`。
- 管理端系统设置：`/admin/system`；纹理名册：`/admin/skins`、`/admin/capes`；纹理设置与黑名单：`/admin/texture-settings`、`/admin/texture-blacklist`。
- 部署到生产：systemd、反向代理、MySQL、备份与迁移见[部署与运维](deployment.md)。

## 常见启动问题

| 现象 | 处理 |
|---|---|
| 启动报 `UnsupportedClassVersionError` | 使用了低于 Java 25 的运行时 |
| 35598 打不开 | 已安装完成（存在 `.INSTALLED`）时安装向导不再启动；直接访问 35565/35599 |
| 端口被占用 | 修改 `config.yml` 中对应服务端口，或停止占用进程 |
| MySQL 连接失败 | 检查 `sql.yml`、数据库/用户权限、MySQL 是否监听 |
