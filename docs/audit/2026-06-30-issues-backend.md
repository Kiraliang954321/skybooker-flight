# 后端 Issue 清单 — 功能验证（2026-06-30）

> 来源：前端 `.env.local` 修复后（PR#47 CORS + 本地 `.env.local` 指向 8080）的端到端功能验证。
> 范围：用户反馈的 4 个问题中，定位为**后端缺失**的问题。前端侧问题见 [2026-06-30-issues-frontend.md](2026-06-30-issues-frontend.md)。
> 优先级：P1=功能缺失影响业务完整性 / P2=体验增强

---

## 本次验证已闭环（非 bug）

### P1 ｜ AI 助手链路正常

用户反馈"AI 助手不可用"，实测**完全正常**：

- 浏览器实测（user1 登录态）：`POST /api/ai/chat` → **200**
- DeepSeek 真实解析意图，返回航班推荐卡片：南方航空 CA1564，上海 03:10 → 北京 06:10，余 180 座，¥500
- 前端正确渲染 `AiFlightCard` 组件（console 仅报 Base UI Button 无障碍警告，不影响功能）
- 此前"不可用"是前端 `.env.local` 未配置导致请求打回前端自身（404 HTML），**非 AI 本身问题**

> 证据截图：`.playwright-mcp/ai-assistant-working.png`

---

## B-CABIN-1 ｜ 多舱位（公务舱/头等舱）支持完全缺失

- **优先级**：P1（功能缺失，影响业务完整性）
- **现象**：
  - 用户端机票预订页（`/booking/[flightId]`）只能购买经济舱座位，公务舱/头等舱显示"卖完"或根本不存在
  - 管理后台创建/编辑航班后，只能"一键生成座位"，**无法分别设置经济/公务/头等舱的座位数量和价格**
- **根因 / 证据**：
  - `backend/src/main/java/com/skybooker/admin/service/AdminFlightService.java:111` —— `generateSeats` 硬编码 `seat.setCabinClass("ECONOMY")`，所有生成的座位**全部是经济舱**：
    ```java
    for (int row = 1; seatCount < totalSeats; row++) {
        for (String letter : letters) {
            ...
            seat.setCabinClass("ECONOMY");        // ← 硬编码,无视舱位
            seat.setPrice(flight.getBasePrice()); // ← 单一价格
            ...
        }
    }
    ```
  - `backend/src/main/resources/db/migration/V1__init_schema.sql`：
    - `flight` 表只有 `base_price DECIMAL`（单一价格）、`total_seats`、`remaining_seats`（总余座，不分舱位）
    - `flight_seat` 表虽有 `cabin_class VARCHAR(30) DEFAULT 'ECONOMY'` 字段，但生成时只填 ECONOMY
  - `backend/src/main/java/com/skybooker/admin/controller/AdminController.java:59` —— admin 只有 `POST /flights/{id}/generate-seats`，**无舱位库存设置接口**
  - 实测：AI 推荐 CA1564 显示"余 180 座"（`flight.remaining_seats` 总数，不分舱位）
- **建议修复**（推荐方案 B，规范且可扩展）：

  **方案 A（轻量，不推荐）**：`flight` 表加 `business_price` / `first_price` + `business_seats` / `first_seats` 字段，`generateSeats` 按比例生成 3 舱位。改动小，但 schema 冗余、扩展性差（加第 4 种舱位又要改表）。

  **方案 B（规范，推荐）**：新建 `flight_cabin` 表，一航班多舱位行：
  ```sql
  -- V10__add_flight_cabin.sql
  CREATE TABLE flight_cabin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flight_id BIGINT NOT NULL,
    cabin_class VARCHAR(30) NOT NULL,        -- ECONOMY / BUSINESS / FIRST
    price DECIMAL(10,2) NOT NULL,
    total_seats INT NOT NULL,
    remaining_seats INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_flight_cabin (flight_id, cabin_class),
    CONSTRAINT fk_flight_cabin_flight FOREIGN KEY (flight_id) REFERENCES flight(id),
    CHECK (cabin_class IN ('ECONOMY','BUSINESS','FIRST'))
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  ```
  配套后端改动：
  1. `FlightCabin` entity + `FlightCabinMapper` + XML（CRUD + 按 flight_id 批量查）
  2. admin 接口：
     - `PUT /api/admin/flights/{id}/cabins` —— 设置各舱位价格 + 库存（批量 upsert，校验舱位枚举）
     - `generateSeats` 改为按 `flight_cabin` 配置逐舱位生成座位（读各舱 `total_seats` 分配，按舱位分区排座）
  3. `FlightVO` 增加 `cabins: List<FlightCabinVO>`（各舱位 price / totalSeats / remainingSeats），供前端 booking 页展示
  4. 下单/候补校验 `seat.cabin_class` 与请求舱位一致；余票扣减改为按舱位（`flight_cabin.remaining_seats`）
- **验证**：
  - admin 创建航班后能设置 3 舱位（价格 + 座位数），生成座位按舱位分区（如 1-3 排头等、4-8 排公务、9-30 排经济）
  - 用户端 booking 页能选公务/头等舱并下单，价格按舱位
  - 订单的 `seat.cabin_class` 反映实际舱位
  - 候补按舱位兑现（`waitlist.cabin_class` 字段已存在，当前因只有 ECONOMY 实际无法测多舱位候补）
- **影响面**：
  - **schema 变更（红线，落地前需评审）**：新增 `flight_cabin` 表 + Flyway V10 迁移
  - 涉及模块：admin、flight、order、waitlist
  - 前端需配合：booking 页多舱位选择 UI、admin flights 页加舱位配置（见 [2026-06-30-issues-frontend.md](2026-06-30-issues-frontend.md)）
- **文档影响**：`docs/06_DATABASE_DESIGN.md`（新表）、`docs/07_API_DESIGN.md`（舱位接口）、`docs/10_BACKEND_DESIGN.md`、README 核心功能描述
- **排期**：中大工作量，建议单独 `feature/multi-cabin` 分支，先评审 schema 再实施

---

## 排期建议

| Issue | 优先级 | 工作量 | 备注 |
|---|---|---|---|
| B-CABIN-1 | P1 | 大（schema + service + 多模块） | 涉及 DB 迁移，需评审后开工 |

> 注：用户反馈的 P1（AI 不可用）已验证为 `.env.local` 配置问题，非后端 bug，已闭环。
