# 部署与运维

本页介绍生产环境部署、数据库、反向代理、备份、升级与排错。未安装请先读[快速开始](getting-started.md)。

## 1. 安装 Java 25

运行 LingYggdrasil 必须使用 **Java 25**。

### Debian / Ubuntu（含 Debian 13 trixie）

```bash
sudo apt update
sudo apt install -y openjdk-25-jdk-headless     # 或 openjdk-25-jre-headless
java --version
```

> Debian 13 仓库提供 OpenJDK 25。

### Red Hat / CentOS / Fedora

```bash
sudo dnf install -y java-25-openjdk
```

### Windows

下载 Zulu / Temurin 25 安装包，安装后确认 `java --version` 输出 25.x。

## 2. 目录与权限

建议使用独立系统用户与目录：

```bash
sudo useradd -r -m -d /opt/lingyggdrasil lingyggdrasil
sudo -u lingyggdrasil mkdir -p /opt/lingyggdrasil
sudo -u lingyggdrasil cp LingYggdrasil-2.10.1.jar /opt/lingyggdrasil/
```

数据目录内容：

```text
/opt/lingyggdrasil/
├── LingYggdrasil-2.10.1.jar
├── config.yml        # 服务/日志配置
├── sql.yml           # 数据库/邮件（密码 ENC: 加密）
├── data.db           # SQLite 模式
├── skins/  capes/    # 纹理文件
├── plugins/          # 插件（spi/ 与 legacy/ 子目录，及传统插件）
├── i18n/             # 外部语言包覆盖（可选）
└── logs/             # 业务/审计日志
```

## 3. 数据库

### SQLite（默认）

安装向导选择 SQLite 即可；数据保存在数据目录 `data.db`。备份时停机复制整个目录最稳妥。

### MySQL / MariaDB

支持 MySQL 8.0+ 与 MariaDB 10.6+。

```sql
CREATE DATABASE lingyggdrasil CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ly'@'127.0.0.1' IDENTIFIED BY '强密码';
CREATE USER 'ly'@'localhost' IDENTIFIED BY '强密码';
GRANT ALL PRIVILEGES ON lingyggdrasil.* TO 'ly'@'127.0.0.1';
GRANT ALL PRIVILEGES ON lingyggdrasil.* TO 'ly'@'localhost';
FLUSH PRIVILEGES;
```

安装向导中选择 MySQL 并填写 host/port/database/username/password；生成后的连接配置位于 `sql.yml`（密码 `ENC:` 加密）。

> MySQL 8.0+ / MariaDB 10.6+；时区使用 UTC 语义，程序写入 `yyyy-MM-dd HH:mm:ss.SSS`。

## 4. systemd

```ini
# /etc/systemd/system/lingyggdrasil.service
[Unit]
Description=LingYggdrasil 2.10.1
After=network-online.target
Wants=network-online.target

[Service]
User=lingyggdrasil
Group=lingyggdrasil
WorkingDirectory=/opt/lingyggdrasil
ExecStart=/usr/bin/java -Xms512M -Xmx2G -jar LingYggdrasil-2.10.1.jar
Restart=on-failure
RestartSec=5
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now lingyggdrasil
sudo journalctl -u lingyggdrasil -f
```

## 5. 端口与防火墙

| 端口 | 说明 | 建议 |
|---|---|---|
| 35565 | 用户端 | 公网开放（或反代） |
| 35577 | Yggdrasil API | 公网开放（或反代） |
| 35599 | 管理端 | **仅内网/白名单/SSH 隧道** |
| 35598 | 安装向导 | 仅首次安装时临时开放 |

```bash
# ufw 示例
sudo ufw allow 35565/tcp
sudo ufw allow 35577/tcp
# 不要放行 35599/35598
```

云服务器还需在安全组放行 35565/35577。

## 6. 反向代理（Nginx 示例）

```nginx
server {
    listen 443 ssl http2;
    server_name mc.example.com;

    location / {
        proxy_pass http://127.0.0.1:35565;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    location / {
        proxy_pass http://127.0.0.1:35577;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

聚合模式只需暴露用户域名：`https://mc.example.com/api/yggdrasil` 由用户端宿主转发，API 端口可只监听回环。

## 7. 备份与恢复

### SQLite

```bash
sudo systemctl stop lingyggdrasil
sudo tar czf lingyggdrasil-$(date +%F).tar.gz -C /opt lingyggdrasil
sudo systemctl start lingyggdrasil
```

### MySQL / MariaDB

```bash
mysqldump --single-transaction --routines --triggers lingyggdrasil > lingyggdrasil-$(date +%F).sql
# 恢复
mysql lingyggdrasil < lingyggdrasil-YYYY-MM-DD.sql
```

纹理文件位于 `skins/`、`capes/`（MySQL 模式下与数据库需同时备份）。

## 8. 日常维护

- **日志**：`logs/` 下按业务/审计/插件分文件；管理端可查询审计事件。
- **纹理对账**：管理端「纹理资产」→ 存储孤儿扫描/清理（`/admin/api/texture-storage/orphans`）。
- **设置**：管理端 `/admin/system` 按组编辑，冲突会提示刷新（409）。
- **密钥**：世界树密钥由用户自行查看/轮换；管理员可在用户管理/角色管理中处理。
- **监控**：通过 `GET /`（用户端）与 `GET /`（API 元数据）及 `hasJoined` 探测可用性。

## 9. 排错

| 现象 | 排查 |
|---|---|
| 启动即退出 | 检查 `java --version` 是否为 25；查看控制台/`logs/` |
| 公网端口不可达 | 云安全组/防火墙是否放行 35565/35577 |
| MySQL 报 datetime 截断 | 确认使用最新构建且库为 utf8mb4 |
| 上传 413/400 | 检查反向代理 body 限制与 PNG 尺寸/格式 |
| 登录后立刻 401 | 反代是否正确传递 Cookie/HTTPS；三端 Cookie 相互隔离 |
| 迁移失败 | 先看 `detect`/`plan` 输出与迁移日志；恢复备份后重试（步骤幂等） |

