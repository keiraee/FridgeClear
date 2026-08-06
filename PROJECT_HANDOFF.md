# FridgeClear 项目交接与发展文档

> 面向后续开发者、其他 AI IDE 和项目评审者的单一入口文档。接手项目时请先阅读本文，再查看 `docs/ROADMAP.md` 与 `docs/api-contract.md`。

## 1. 项目定位

**FridgeClear** 的中文定位是：**清空冰箱——AI 食材消耗与备餐规划助手**。

核心闭环：

```text
录入库存 → 识别临期与归一化食材 → 推荐匹配菜谱 → 收藏
→ 异步 AI 生成备餐计划 → 采购清单 → 烹饪状态回写
```

## 2. 当前完成情况（2026-08）

### 后端

- Spring Boot 3、Java 17、MySQL、Flyway（当前 V1–V11）
- JWT 认证、用户隔离
- 库存 CRUD、食材别名、HowToCook 导入
- 菜谱查询、媒体、库存推荐（临期加权 + 别名 + 筛选）
- 菜谱收藏 API（`user_favorite_recipe`）
- AI 备餐：**POST 提交任务（202）+ GET 轮询**（`ai_plan_run`）
- 全局 AI 配置（`system_ai_config`）、管理端接口
- 统一分页 DTO（`PageResponse`）、默认关闭 Hibernate SQL 噪音日志

### 前端

- Vue 3 + Pinia + 响应式布局（PC / 移动端底部导航）
- Remix Icon 统一图标（`FcIcon` + `unplugin-icons` 按需打包）
- 首页推荐与筛选、菜谱详情、收藏与「我的收藏」
- 备餐页异步生成 + loading / 轮询 / 错误提示
- 换账号时备餐页状态自动清空（`keep-alive` 场景）
- 管理员手机端底部「管理」入口 → `/admin/ai-config`

### 阶段进度（详见 `docs/ROADMAP.md`）

| 阶段 | 主题 | 状态 |
|------|------|------|
| A | 界面优化 | ✅ |
| B | 推荐算法加强 | ✅ |
| C | 收藏功能 | ✅ |
| D | AI 备餐异步化 | ✅ |
| E | 工程化与文档 | 🔄 进行中 |

## 3. 技术栈与目录

```text
FridgeClear/
├── src/main/java/com/sccothe/fridgeclear/
│   ├── auth/          注册登录、JWT
│   ├── pantry/        冰箱库存
│   ├── recipe/        菜谱、导入、推荐
│   ├── favorite/      菜谱收藏
│   ├── mealplan/      备餐计划、异步生成
│   ├── ai/            Provider、全局配置、AI 网关
│   └── common/        响应、异常、分页、安全
├── src/main/resources/db/migration/   Flyway V1–V11
├── src/test/          Testcontainers 集成测试
├── web/src/           Vue 前端
├── docs/              ROADMAP、API 契约
└── PROJECT_HANDOFF.md 本文档
```

## 4. 数据库迁移摘要

| 版本 | 说明 |
|------|------|
| V1–V5 | 基础结构、菜谱、别名 |
| V6–V7 | 备餐计划、购物清单 |
| V8–V9 | AI Provider、用户账号 |
| V10 | 系统 AI 配置 |
| V11 | 用户菜谱收藏 |

## 5. 环境配置与启动

见根目录 [README.md](README.md) 与 `.env.example`。

要点：

- `AI_PLATFORM_*` 仅在 `system_ai_config` 为空时种子写入一次，之后由管理端维护
- `ADMIN_BOOTSTRAP_*` 提升或创建管理员
- 备餐生成前会校验 AI 是否可用；未配置时返回 503

## 6. API 目录

| 模块 | 前缀 |
|------|------|
| 认证 | `/api/v1/auth` |
| 库存 | `/api/v1/pantry-items` |
| 菜谱 | `/api/v1/recipes` |
| 推荐 | `/api/v1/recommendations` |
| 收藏 | `/api/v1/favorites` |
| 备餐 | `/api/v1/meal-plans`（含 `POST /generate`、`GET /generate/tasks/{id}`） |
| 购物 | `/api/v1/shopping-list-items` |
| 管理 | `/api/v1/admin/**` |

契约详情：[docs/api-contract.md](docs/api-contract.md)

## 7. 已知问题与技术债

### 中优先级

- 用户自配 AI Provider 的前端管理页未实现（BYOK 默认关闭）
- HowToCook 导入仍为同步长任务，宜改异步
- 图片依赖本地 HowToCook 目录，生产环境宜迁对象存储
- 多协议 AI 适配器（Anthropic / Gemini）仅部分实现

### 低优先级

- 合并购物清单重复项、一键回写库存
- 前端 E2E 测试
- CI/CD、Docker Compose 一键启动
- 限流、成本统计、审计日志

## 8. 交给其他 AI 的规则

1. 先读本文 + `docs/ROADMAP.md`，再改代码
2. 不提交 `.env`、密码、API Key
3. 不用固定 `user_id` 绕过鉴权
4. 数据库变更新增 Flyway migration
5. 改 API 时同步 `docs/api-contract.md` 与 `web/src/types`
6. 改动后执行 `./mvnw compile` 与 `cd web && npm run build`
7. 集成测试需 Docker：`./mvnw test`

## 9. 交接检查清单

- [ ] 已配置 `.env` 与 MySQL
- [ ] 后端编译通过
- [ ] 前端 `npm run build` 通过
- [ ] 已验证登录、库存、推荐、收藏
- [ ] 管理端已配置并启用全局 AI
- [ ] 已验证备餐异步生成与轮询
- [ ] `./mvnw test` 通过（需 Docker）
