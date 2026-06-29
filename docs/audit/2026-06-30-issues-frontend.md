# 前端 Issue 清单 — 功能验证（2026-06-30）

> 来源：前端 `.env.local` 修复后的端到端功能验证。
> 范围：用户反馈的 4 个问题中，定位为**前端缺失**的问题。后端侧问题见 [2026-06-30-issues-backend.md](2026-06-30-issues-backend.md)。

---

## 本次验证已闭环（非 bug）

### P3 ｜ 邮箱注册链路正常

用户反馈"前端邮箱注册没有接入后端接口"，实测**已接入**：

- 注册页 `/register` 表单完整：邮箱 / 验证码（+ 发送验证码按钮）/ 昵称 / 密码 / 确认密码
- "发送验证码"按钮在填入合法邮箱后从 `disabled` 启用 → 点击 → `POST /api/auth/email-code` 返回 **200**
- `frontend/src/services/authApi.ts` 注册三件套齐全：
  - `sendEmailCode`（`POST /auth/email-code`，body `{email, scene}`）
  - `register`（`POST /auth/register`，body `{email, code, nickname, password, confirmPassword}`）
  - `resetPassword`（`POST /auth/reset-password`）
  - 路径与后端 `AuthController` 完全匹配（`/email-code`、`/register`、`/reset-password`）
- 此前"没接入"的观感，很可能是 `.env.local` 未配导致请求打回前端自身（404 HTML），或测试邮箱被 Resend bounce
- **提示**：`MAIL_PROVIDER=resend` 时验证码真发邮件；本地调试若不想配 Resend，可在 `.env` 设 `MAIL_PROVIDER=log`，验证码会打印在后端日志里（H11 后为 DEBUG 级别，需开 debug 日志）

---

## F-AICONFIG-1 ｜ 管理后台缺 AI 参数配置页

- **优先级**：P1（功能缺失，但后端接口已就绪，纯前端工作）
- **现象**：
  - 管理后台侧边栏无"AI 配置"入口
  - 无法在后台修改 LLM 模型（如切换 `deepseek-v4-flash`）、apiKey、超时、开关 —— 只能改 `.env` 后重启后端
- **根因 / 证据**：
  - **后端接口已就绪**（PR#37 已合并 main）：`GET/PUT /api/admin/ai/llm-config`，AES-GCM 加密存储 apiKey，DB 优先 + env fallback（`DynamicLlmConfigProvider`）
  - 前端 `frontend/src/services/adminApi.ts` 全文**无** `/admin/ai/llm-config` 调用（只有 flights / orders / users / reports）
  - 前端管理后台 `frontend/src/app/admin/(app)/` 无 AI 配置页路由（只有 dashboard / flights / orders / users）
- **后端契约**（已就绪，PR#37，前端照此对接）：

  `GET /api/admin/ai/llm-config`（需 admin Token）→ `AiLlmConfigVO`：
  ```json
  {
    "enabled": true,
    "baseUrl": "https://api.deepseek.com",
    "apiKey": "sk-***（脱敏，非明文）",
    "model": "deepseek-v4-flash",
    "timeoutMs": 8000,
    "maxRetries": 1,
    "source": "DB",
    "updatedBy": 1,
    "updatedAt": "2026-06-30T16:47:00"
  }
  ```

  `PUT /api/admin/ai/llm-config`（需 admin Token）← `AiLlmConfigDTO`：
  ```json
  {
    "enabled": true,
    "baseUrl": "https://api.deepseek.com",
    "apiKey": "sk-新密钥",
    "model": "deepseek-v4-flash",
    "timeoutMs": 8000,
    "maxRetries": 1
  }
  ```
  - 错误码 `10022`：AI 配置相关（如后端缺 `AI_CONFIG_ENC_KEY` 导致 PUT 失败）
  - `apiKey` 在 DB 用 AES-GCM 加密存储；**GET 返回脱敏值，不回显明文**；PUT 时若 `apiKey` 留空通常表示不改（按后端实际实现确认）
- **建议修复**（纯前端）：
  1. `frontend/src/services/adminApi.ts` 增加：
     ```ts
     export function getLlmConfig() {
       return get<LlmConfigVO>("/admin/ai/llm-config", undefined, { auth: "admin" })
     }
     export function updateLlmConfig(data: LlmConfigDTO) {
       return put<LlmConfigVO>("/admin/ai/llm-config", data, { auth: "admin" })
     }
     ```
  2. `frontend/src/types/admin.ts` 增加 `LlmConfigVO` / `LlmConfigDTO` 类型（字段见上方契约）
  3. `frontend/src/app/admin/(app)/ai-config/page.tsx` 新建配置页：
     - 表单字段：`enabled`（开关）、`baseUrl`、`model`、`apiKey`（密码框，回填脱敏值，留空表示不改）、`timeoutMs`、`maxRetries`
     - 加载时 `GET` 回填，保存时 `PUT`
     - 处理 `10022` 错误（提示需后端配 `AI_CONFIG_ENC_KEY`）
  4. 管理后台侧边栏（admin 布局组件）加"AI 配置"入口 → `/admin/ai-config`
- **验证**：
  - admin 登录后能在后台改 AI 模型 / apiKey / 开关
  - 改后**无需重启后端**，AI 助手立即用新配置（DB 优先，`DynamicLlmConfigProvider` 实时读）
  - GET 时 `apiKey` 字段脱敏，不回显明文
- **影响面**：纯前端（adminApi + types + 新页面 + 侧边栏），**后端无需改动**
- **排期**：中等（一个页面 + api + 类型，约半天）

---

## F-CABIN-UI-1 ｜ 多舱位选择 UI（依赖后端 B-CABIN-1）

- **优先级**：P1（与后端 B-CABIN-1 配套，后端先行）
- **现象**：booking 页只能选经济舱（因后端只生成 ECONOMY 座位）
- **依赖**：后端 [B-CABIN-1](2026-06-30-issues-backend.md)（`flight_cabin` 表 + `FlightVO.cabins`）落地后，前端才能做
- **建议修复**（后端 B-CABIN-1 完成后）：
  - booking 页根据 `FlightVO.cabins` 渲染舱位选择（经济/公务/头等 + 各自价格/余座）
  - 选舱后座位图按舱位分区过滤
  - admin flights 页加"舱位库存设置"（调 `PUT /admin/flights/{id}/cabins`）

---

## 附：前端组件无障碍警告（非阻塞，建议修）

- **现象**：`Header` / `HomePage` / `AiFlightCard` 的 Base UI `<Button nativeButton>` 用了非原生 `<button>`（`<a>` / `<Link>` 作为 render prop），console 报错：
  > Base UI: A component that acts as a button expected a native `<button>` because the `nativeButton` prop is true. Rendering a non-`<button>` removes native button semantics, which can impact forms and accessibility.
- **影响**：丢原生 button 语义，影响表单提交/无障碍，**不影响功能**
- **修复**：将 `nativeButton` 设为 `false`，或 render prop 改用真实 `<button>`

---

## 排期建议

| Issue | 优先级 | 工作量 | 备注 |
|---|---|---|---|
| F-AICONFIG-1 | P1 | 中（半天） | 后端就绪，纯前端对接 |
| F-CABIN-UI-1 | P1 | 中 | 依赖后端 B-CABIN-1 先落地 |
| Base UI Button 警告 | P3 | 小 | 无障碍增强，非阻塞 |

> 注：用户反馈的 P3（注册未接入）已验证为正常，已闭环。
