# 部署与运维

本页介绍在生产环境中部署、运行与维护泠 Yggdrasil 的推荐方式。

> 尚未安装？请先阅读[快速开始](getting-started.md)。

## 安装 Java 25

### Debian / Ubuntu

```bash
sudo apt update
sudo apt install openjdk-25-jre -y
```

### Red Hat / CentOS / Fedora

```bash
sudo dnf install java-25-openjdk -y
# 或
sudo yum install java-25-openjdk -y
```

### Windows

从浏览器下载 Java 25 安装包（`.exe` 或 `.msi`），双击按向导安装。

安装完成后可用以下命令确认：

```bash
java --version
```

## 端口与网络

| 端口    | 用途          | 建议暴露范围           |
|---------|---------------|------------------------|
| `35565` | 用户端        | 公网（经反向代理）     |
| `35577` | Yggdrasil API | 公网（经反向代理）     |
| `35599` | 管理后台      | **仅内网 / 白名单 IP** |
| `35598` | 安装向导      | 仅首次安装时临时开放   |

**重要安全建议**

- 管理后台（35599）**不要**直接暴露到公网，使用反向代理做 IP 白名单或 Basic Auth 保护。
- 安装向导（35598）安装完成后即不再启动；若需重新安装，请先妥善备份。
- 程序默认监听 `0.0.0.0`，请配合防火墙或反向代理限制来源。

## 反向代理

### 推荐拓扑：用户端与 API 同源

Minecraft 启动器会访问 Yggdrasil API，而网页端会访问用户端接口。将两者放在**同一个域名**下（按路径区分）可以避免跨域问题，推荐配置如下：

- `/authserver/*`、`/sessionserver/*`、`/textures/*`、`/api/profiles/lookup/*`、`/api/user/profile/*` → 转发到 `35577`
- 其余路径 → 转发到 `35565`

### nginx 示例

```nginx
# 用户端 + Yggdrasil API（同源）
server {
    listen 443 ssl;
    server_name example.com;

    ssl_certificate     /etc/nginx/ssl/example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/example.com.key;

    # Yggdrasil API 路径转发到 35577
    location ~ ^/(authserver|sessionserver|textures|api/profiles/lookup|api/user/profile)/ {
        proxy_pass http://127.0.0.1:35577;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 其余转发到用户端 35565
    location / {
        proxy_pass http://127.0.0.1:35565;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# 管理后台（限制来源）
server {
    listen 443 ssl;
    server_name admin.example.com;

    ssl_certificate     /etc/nginx/ssl/admin.example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/admin.example.com.key;

    allow 203.0.113.0/24;   # 替换为你的办公网/家庭 IP
    deny all;

    location / {
        proxy_pass http://127.0.0.1:35599;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

> **CORS 说明**：程序内置的跨域白名单为 `http://localhost:35565/35577/35599/35598`。若通过多个不同域名跨域访问，浏览器可能因跨域被拒。推荐按上述「同源」拓扑部署；如确需跨子域，请自行在反向代理层统一来源。

### 关于客户端 IP

程序通过 `X-Forwarded-For` / `X-Real-IP` 请求头识别客户端 IP（用于用户操作日志、同 IP 注册限制、限流等）。请确保反向代理正确设置这些头，且**不要让客户端可以伪造**它们（反向代理应覆盖而非透传客户端自带值）。

## 以 systemd 运行（Linux）

创建服务文件 `/etc/systemd/system/lingyggdrasil.service`：

```ini
[Unit]
Description=LingYggdrasil
After=network.target

[Service]
Type=simple
User=lingyggdrasil
WorkingDirectory=/opt/lingyggdrasil
ExecStart=/usr/bin/java -Xms512M -Xmx2G -jar /opt/lingyggdrasil/LingYggdrasil-2.2.2.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now lingyggdrasil
sudo systemctl status lingyggdrasil
```

`WorkingDirectory` 必须指向 JAR 所在目录，程序会在此目录读写数据库、配置、纹理与日志。

## 数据目录与备份

| 路径                              | 内容                               | 是否需备份 |
|-----------------------------------|------------------------------------|------------|
| `sql.yml`                         | 数据库与邮件配置（含加密后的密码） | 是         |
| `.INSTALLED`                      | 安装标记                           | 是         |
| SQLite 数据库文件（如 `data.db`） | 全部业务数据                       | 是         |
| `skins/`、`capes/`                | 纹理文件                           | 是         |
| `i18n/`                           | 外部语言包（可重建）               | 否         |
| `icons/`                          | 自定义图标（可选）                 | 视情况     |
| `logs/`                           | 运行日志（按服务分目录）           | 否         |

备份示例：

```bash
tar czf lingyggdrasil-backup-$(date +%F).tar.gz \
  sql.yml .INSTALLED data.db skins capes icons
```

> 使用 MySQL / PostgreSQL 时，请用相应的数据库备份工具导出数据。

## 升级

1. 停止服务。
2. 备份运行目录。
3. 用新版本的 JAR 替换旧 JAR。
4. 启动服务。程序会在启动时自动执行数据库结构迁移。

```bash
sudo systemctl stop lingyggdrasil
cp LingYggdrasil-2.2.2.jar /opt/lingyggdrasil/
sudo systemctl start lingyggdrasil
```

> 升级前请务必备份。若切换了签名模式或重新生成了密钥，客户端可能需要重新登录。

## 日志

- 运行日志由 Logback 输出，配置见 `logback.xml`。
- 用户操作日志写入数据库表 `user_logs`，可在用户端「日志」页查看与下载。
- 用户操作日志的开关、大小上限、保留天数、记录范围与下载间隔可在管理后台配置，详见[配置参考](configuration.md#用户操作日志)。
