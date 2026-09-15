# Yggdrasil API 参考

泠 Yggdrasil 在 **35577** 端口实现 Yggdrasil 认证协议，兼容 authlib 体系，可供主流 Minecraft 启动器接入。

下文中的 `<API>` 指 API 根地址，例如 `https://example.com`（经反向代理同源）或 `http://127.0.0.1:35577`。

> 需要先创建角色并获取 Token 才能完成认证，见[用户功能指南](user-guide.md#角色管理)。

## 端点总览

| 分类   | 方法   | 路径                                              |
|--------|--------|---------------------------------------------------|
| 元数据 | `GET`  | `/`                                               |
| 认证   | `POST` | `/authserver/authenticate`                        |
| 认证   | `POST` | `/authserver/refresh`                             |
| 认证   | `POST` | `/authserver/validate`                            |
| 认证   | `POST` | `/authserver/invalidate`                          |
| 认证   | `POST` | `/authserver/signout`                             |
| 会话   | `POST` | `/sessionserver/session/minecraft/join`           |
| 会话   | `GET`  | `/sessionserver/session/minecraft/hasJoined`      |
| 会话   | `GET`  | `/sessionserver/session/minecraft/profile/{uuid}` |
| 纹理   | `GET`  | `/textures/{type}/{hash}`                         |
| 扩展   | `GET`  | `/api/profiles/lookup/minecraft/name/{username}`  |
| 扩展   | `POST` | `/api/profiles/lookup/minecraft`                  |
| 扩展   | `GET`  | `/api/user/profile/profiles/minecraft/{uuid}`     |

## 元数据

### `GET /`

返回 API 元数据，启动器据此获取签名公钥与纹理域名。

响应示例：

```json
{
  "meta": {
    "serverName": "LingYggdrasil",
    "implementationName": "LingYggdrasil",
    "implementationVersion": "2.0.1"
  },
  "skinDomains": ["example.com", "api.example.com"],
  "signaturePublickey": "-----BEGIN PUBLIC KEY-----\n..."
}
```

## 认证服务器（authserver）

### `POST /authserver/authenticate`

使用角色名与 Yggdrasil Token 登录，获取 accessToken。

请求：

```json
{
  "username": "<角色名>",
  "password": "<Yggdrasil Token>",
  "clientToken": "<客户端令牌，可选>",
  "agent": { "name": "Minecraft", "version": 1 },
  "requestUser": true
}
```

- `username` 也支持**邮箱式用户名**（单个 `@` 归一化，例如 `player@example.com` 视为 `player`）。
- `password` 为该角色的 Yggdrasil Token。

响应：

```json
{
  "accessToken": "<令牌>",
  "clientToken": "<客户端令牌>",
  "availableProfiles": [
    { "id": "<无连字符 UUID>", "name": "<角色名>" }
  ],
  "selectedProfile": { "id": "<无连字符 UUID>", "name": "<角色名>" },
  "user": {
    "id": "<用户 ID>",
    "properties": []
  }
}
```

失败时返回错误对象：

```json
{ "error": "ForbiddenOperationException", "errorMessage": "Invalid credentials." }
```

> 若账户被禁用、邮箱未验证（在开启邮箱验证时）或角色不存在，均会返回错误。

### `POST /authserver/refresh`

用 accessToken 换取新的 accessToken。

请求：

```json
{
  "accessToken": "<旧令牌>",
  "clientToken": "<客户端令牌，可选>",
  "selectedProfile": { "id": "<UUID>", "name": "<角色名>" },
  "requestUser": true
}
```

响应与 `authenticate` 相同。

### `POST /authserver/validate`

校验 accessToken 是否有效。有效返回 `204 No Content`，无效返回 `403`。

```json
{ "accessToken": "<令牌>", "clientToken": "<客户端令牌，可选>" }
```

### `POST /authserver/invalidate`

使 accessToken 失效。成功返回 `204 No Content`。

```json
{ "accessToken": "<令牌>", "clientToken": "<客户端令牌，可选>" }
```

### `POST /authserver/signout`

登出，使指定用户名对应的令牌失效。

```json
{ "username": "<角色名>", "password": "<Yggdrasil Token>" }
```

## 会话服务器（sessionserver）

### `POST /sessionserver/session/minecraft/join`

客户端进入服务器时，向会话服务器登记加入。

```json
{
  "accessToken": "<令牌>",
  "selectedProfile": "<无连字符 UUID>",
  "serverId": "<服务器 ID>"
}
```

成功返回 `204 No Content`。

### `GET /sessionserver/session/minecraft/hasJoined`

服务器校验玩家是否已加入。查询参数：

| 参数       | 说明              |
|------------|-------------------|
| `username` | 角色名            |
| `serverId` | 服务器 ID         |
| `ip`       | 客户端 IP（可选） |

已加入时返回角色档案（含 `textures` 属性）；未加入返回 `204 No Content`。

```json
{
  "id": "<无连字符 UUID>",
  "name": "<角色名>",
  "properties": [
    {
      "name": "textures",
      "value": "<Base64 纹理数据>",
      "signature": "<签名>"
    }
  ]
}
```

### `GET /sessionserver/session/minecraft/profile/{uuid}`

按 UUID 获取角色档案（含签名后的纹理属性）。

## 纹理端点

### `GET /textures/{type}/{hash}`

公开的纹理访问端点，`{type}` 为 `skin` 或 `cape`，`{hash}` 为纹理哈希。游戏客户端据此加载皮肤与披风。

> 该端点的公开 URL 由「API 域名 / 通用域名」设置生成。请确保这些域名指向 35577（或经反向代理可访问到 `/textures/*`）。

## 扩展查询端点

以下端点为便利性扩展，非 Yggdrasil 官方协议。

| 方法   | 路径                                             | 说明                                   |
|--------|--------------------------------------------------|----------------------------------------|
| `GET`  | `/api/profiles/lookup/minecraft/name/{username}` | 按角色名查询单个档案                   |
| `POST` | `/api/profiles/lookup/minecraft`                 | 批量按名查询（受「批量查询上限」限制） |
| `GET`  | `/api/user/profile/profiles/minecraft/{uuid}`    | 按 UUID 查询用户可见档案               |

批量查询请求：

```json
{ "names": ["Alice", "Bob"] }
```

- 空数组返回 `[]`。
- 超过「批量查询上限」（默认 6）返回 `400 Too many profiles requested`。

## 客户端接入示例

以 HMCL / 支持自定义 Yggdrasil 的启动器为例：

1. 打开启动器的「账户」或「登录方式」设置，选择「自定义 Yggdrasil / 外置登录」。
2. 认证服务器地址填写 API 根地址，例如 `https://example.com`（若直连则 `http://<IP>:35577`）。
3. 用户名填写**角色名**，密码填写该角色的 **Yggdrasil Token**。

> 关于如何创建角色、获取 Token，见[用户功能指南](user-guide.md#角色管理)。

## 常见错误

| 错误                                                | 含义                                      |
|-----------------------------------------------------|-------------------------------------------|
| `ForbiddenOperationException: Invalid credentials.` | 用户名/密码错误，或账户被禁用、邮箱未验证 |
| `ForbiddenOperationException: Token expired.`       | accessToken 过期，请刷新或重新登录        |
| `403`                                               | 令牌无效或会话校验失败                    |
| `204 No Content`                                    | 校验/加入成功（无正文）                   |
