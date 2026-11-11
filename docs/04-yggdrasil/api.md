# Yggdrasil API 参考

泠 Yggdrasil 在 **35577** 端口实现 Yggdrasil 认证协议（authlib-injector 体系），供主流 Minecraft 启动器接入。聚合模式下由用户端宿主在 `/<userURL>/api/yggdrasil` 转发同一协议实现。

`<API>` 为 API 根地址，取决于 [API 模式](api-modes.md)：

- `default` / `standalone`：`https://api.example.com`（直连 `http://<IP>:35577`）。
- `aggregate`：`https://<用户域名>/api/yggdrasil`。

> 认证用户名为**角色名**，密码为角色的**世界树密钥**（用户端可查看/轮换）。

## 端点总览

| 分类 | 方法 | 路径 | 成功状态 |
|---|---|---|---|
| 元数据 | GET | `/` | 200 |
| 认证 | POST | `/authserver/authenticate` | 200 |
| 认证 | POST | `/authserver/refresh` | 200 |
| 认证 | POST | `/authserver/validate` | 204 |
| 认证 | POST | `/authserver/invalidate` | 204 |
| 认证 | POST | `/authserver/signout` | 204 |
| 会话 | POST | `/sessionserver/session/minecraft/join` | 204 |
| 会话 | GET | `/sessionserver/session/minecraft/hasJoined` | 200 / 204 |
| 会话 | GET | `/sessionserver/session/minecraft/profile/{uuid}` | 200 / 204 |
| 纹理 | GET | `/textures/{type}/{hash}` | 200 PNG / 404 |
| 扩展 | GET | `/api/profiles/lookup/minecraft/name/{username}` | 200 / 204 |
| 扩展 | POST | `/api/profiles/lookup/minecraft` | 200 |

## 元数据 `GET /`

```json
{
  "meta": {
    "serverName": "泠 Yggdrasil",
    "implementationName": "LingYggdrasil",
    "implementationVersion": "2.10.1",
    "links": { "homepage": "..." }
  },
  "signaturePublickey": "-----BEGIN PUBLIC KEY-----\n...",
  "skinDomains": ["mc.example.com", "api.example.com"]
}
```

- `signaturePublickey` 用于验证 profile 属性签名；
- `skinDomains` 与当前 API 模式/生效域名快照一致，决定客户端允许的纹理域名。

## 认证

### `POST /authserver/authenticate`

请求：

```json
{
  "username": "角色名或账号名",
  "password": "世界树密钥或账号密码",
  "clientToken": "可选，客户端会话标识",
  "agent": { "name": "Minecraft", "version": 1 }
}
```

行为：

- `username` 匹配角色名时按**世界树密钥**校验（新密钥表优先，旧 token 兼容）；
- 匹配账号时按账号密码校验（站点开启账号密码外置登录时）；
- 单角色自动选中并签发会话；多角色返回 `availableProfiles` 且省略 `selectedProfile`，需客户端调用 refresh 选择；
- **零角色**：返回 HTTP 404（`PlayerNotFoundException`，本项目产品定制）。

成功响应（节选）：

```json
{
  "accessToken": "…",
  "clientToken": "…",
  "selectedProfile": { "id": "32位无横线UUID", "name": "角色名" },
  "availableProfiles": [ { "id": "…", "name": "…" } ]
}
```

### `POST /authserver/refresh`

请求 `{accessToken, clientToken?, selectedProfile?}`。用于续期与多角色选择；刷新成功后旧 accessToken 失效。失败时旧 token 在协议允许范围内仍有效。

### `POST /authserver/validate`

请求 `{accessToken, clientToken?}`。有效返回 **204**，无效/过期返回 403。`clientToken` 为可选校验。

### `POST /authserver/invalidate` / `signout`

`invalidate` 注销当前 accessToken；`signout` 使用 `{username,password}`，password 可为世界树密钥或账号密码，成功返回 204。已绑定会话的失效规则与改密、轮换、删除、转移、封禁/注销保持一致。

## 会话

### `POST /sessionserver/session/minecraft/join`

```json
{ "accessToken": "…", "selectedProfile": "32位UUID", "serverId": "服务器ID" }
```

校验会话属于该角色、用户可访问、角色已选择，签发 **join ticket（约 30 秒）**，返回 204。

### `GET /sessionserver/session/minecraft/hasJoined?username=角色名&serverId=...&ip=可选`

- 有有效 ticket：200，返回 `{id, name, properties:[{name:"textures",value:"…",signature:"…"}]}`；
- 无/过期/不匹配：204。

`properties.textures` 为 Base64 编码 JSON，包含 SKIN/CAPE 的 URL 与（可选）模型 metadata；响应属性使用服务端签名。

### `GET /sessionserver/session/minecraft/profile/{uuid}`

返回角色 profile 与签名纹理属性；不存在返回 204。纹理 URL 由当前角色的 **`profile_texture_bindings`** 解析（回退旧列），与网站展示一致。

## 纹理

### `GET /textures/{type}/{hash}`

- `type`：`SKIN` / `CAPE`（大小写不敏感）；
- `hash`：64 位十六进制 SHA-256；
- 返回 `image/png`；文件不存在/未就绪 404。网页下载开关不阻断游戏协议交付。

聚合模式下该端点在 `<用户域名>/api/yggdrasil/textures/...` 提供。

## 扩展查询

- `GET /api/profiles/lookup/minecraft/name/{username}`：按角色名查询 UUID/名称；
- `POST /api/profiles/lookup/minecraft`：批量查询（body 为名称数组）；
- `GET /api/user/profile/profiles/minecraft/{uuid}`：按 UUID 查询角色资料。

## 错误格式

认证类错误返回：

```json
{ "error": "ForbiddenOperationException", "errorMessage": "Invalid credentials" }
```

常见错误：参数缺失（400）、凭证错误/会话无效（403）、角色不存在（404）、限流（429）。

## 签名模式

| 模式 | 说明 |
|---|---|
| `Ed448` | 现代算法，客户端支持有限 |
| `RSA-SHA512` | 推荐的新式兼容 |
| `RSA-SHA1` | 最广泛兼容（默认校验路径） |

模式由管理端设置切换；切换后需重新生成/加载密钥，客户端可能需要重新登录。纹理属性在签名前生成最终 URL，聚合模式同样适用。

## 客户端地址速查

| 模式 | 认证服务器地址 |
|---|---|
| `default` | `http(s)://<API主机>:35577` 或反代 API 域名 |
| `standalone` | 同 default（推荐独立 API 域名） |
| `aggregate` | `https://<用户域名>/api/yggdrasil` |

