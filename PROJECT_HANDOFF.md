# FridgeClear 项目交接与发展文档

> 面向后续开发者、其他 AI IDE 和项目评审者的单一入口文档。接手项目时请先阅读本文，再查看 `.md/README.md`（本地生成的代码索引）。

## 1. 项目定位

**FridgeClear** 的中文定位是：**清空冰箱——AI 食材消耗与备餐规划助手**。

它不是普通菜谱搜索工具，而是从用户“当前拥有的食材”出发，结合保质期、饮食偏好、烹饪时长和厨房设备，生成可执行的菜谱推荐、备餐计划和购物清单。

核心闭环：

```text
录入库存 → 识别临期与归一化食材 → AI 结合菜谱库推荐
→ 生成多日备餐计划 → 计算已拥有/缺少食材
→ 生成购物清单 → 记录购买和烹饪结果
```

产品体验参考 **SideChef**，但差异化重点是：

- 优先消耗现有库存，减少食材浪费；
- AI 综合库存、临期时间和用户条件做决策；
- 用户可以配置多个 AI Provider 和模型；
- 菜谱来自 HowToCook Markdown，并保留原文、步骤和图片关系；
- 计划、购物清单和烹饪状态都持久化。

## 2. 初心与长期目标

本项目用于实践 AI Native 全栈产品开发。初心不是给普通页面简单接一个聊天接口，而是让 AI 参与真实的产品决策链路：

1. 用户告诉系统家里有什么、什么时候过期、想吃什么、能用什么厨具；
2. AI 综合真实库存和菜谱知识做出有理由的推荐；
3. 推荐结果落成多日计划和购物清单；
4. 用户完成烹饪后回写状态，形成持续使用的数据闭环。

长期目标是把“能生成计划”发展为“真正减少食材浪费的个人厨房助手”，同时保持模型和协议可替换，支持用户使用自己的 API Key 和模型。

## 3. 当前完成情况

### 后端

- Spring Boot 3、Java 17、MySQL、Flyway；
- 注册、登录、JWT、当前用户查询；
- 当前用户库存 CRUD 和库存状态；
- HowToCook Markdown 菜谱导入；
- 菜谱原文、元数据、食材关系、步骤和媒体持久化；
- 菜谱搜索、分类、详情和图片接口；
- 食材归一化、别名合并和库存绑定；
- 基于当前库存的菜谱推荐；
- AI Provider 增删改、激活、连接测试、模型发现；
- 全局 AI 配置（system_ai_config）与管理端接口（仅 ADMIN）；
- AI 生成多日备餐计划（全局配置优先，回退用户自配 Provider）；
- 计划历史、详情、归档；
- 计划项状态、购物清单及购买状态；
- Knife4j/OpenAPI 文档；
- 用户数据隔离：库存、Provider、推荐和计划均使用当前 JWT 用户。

### 前端

- Vue 3 + TypeScript + Vite；
- Pinia 登录状态和 JWT 持久化；
- 登录、注册、退出；
- PC/mobile 基础响应式布局；
- SideChef 风格的产品主框架；
- 首页真实菜谱和库存统计；
- 菜谱列表、搜索、分类和图片；
- 我的冰箱真实新增、查询、删除、标记已用完；
- 备餐计划真实 AI 生成、耗时阶段提示、历史加载、归档；
- 管理端全局 AI 配置页（仅 ADMIN 可见入口和路由）；
- 计划项和购物项状态同步后端。

已验证过的真实链路：登录拿 Token → 当前用户接口 → 库存 CRUD → 菜谱导入/搜索/图片 → 食材归一化 → AI Provider 测试 → AI 生成计划 → 刷新后读取计划 → 更新烹饪/购物状态 → 计划归档。

## 4. 技术栈与目录

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3、Spring MVC、Spring Data JPA |
| 数据库 | MySQL 8，当前为阿里云 RDS |
| 迁移 | Flyway |
| API 文档 | OpenAPI 3、Knife4j |
| 鉴权 | JWT、BCrypt |
| AI 密钥 | AES-GCM 加密存储 |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Axios |
| 数据源 | HowToCook Markdown 菜谱库 |

```text
FridgeClear/
├── src/main/java/com/sccothe/fridgeclear/
│   ├── auth/          注册登录、JWT、当前用户
│   ├── pantry/        冰箱库存
│   ├── recipe/        菜谱、导入、媒体、推荐、食材归一化
│   ├── mealplan/      备餐计划、购物清单
│   ├── ai/            Provider、模型发现、AI 调用
│   └── common/        统一响应、异常、OpenAPI、安全配置
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/  Flyway V1-V9
├── src/test/          后端测试
├── web/src/
│   ├── api/           Axios API 封装
│   ├── components/    通用组件
│   ├── stores/        Pinia 状态
│   ├── views/         页面
│   └── router/        路由和鉴权守卫
├── data/source/HowToCook/  本地数据源，已忽略
├── docs/              设计和辅助文档
├── .md/               本地自动代码索引，已忽略
└── PROJECT_HANDOFF.md 本文档
```

## 5. 数据库与 HowToCook

当前 Flyway 迁移：

- V1 基础结构；
- V2 菜谱、食材、步骤、原始文档和媒体；
- V3/V4 文本和来源标识调整；
- V5 食材别名；
- V6/V7 备餐计划结构；
- V8 AI Provider；
- V9 用户账号。

应用启动由 Flyway 自动迁移，JPA 使用 `ddl-auto: validate` 校验结构。已经执行过的 migration 不要修改，数据库变更必须新增 migration。

HowToCook 是导入源，不是 FridgeClear 业务代码，目录已被根目录 `.gitignore` 忽略。新机器执行：

```bash
mkdir -p data/source
git clone --depth 1 https://github.com/Anduin2017/HowToCook.git data/source/HowToCook
git -C data/source/HowToCook lfs install
git -C data/source/HowToCook lfs pull
```

图片没有 LFS 拉取完整时，菜谱仍可导入，但媒体接口可能 404。导入接口为：

```text
POST /api/v1/admin/imports/howtocook
```

当前导入接口是同步任务，数据量大时等待 1–3 分钟；重复执行是幂等的，会更新或跳过，不应产生重复菜谱。未来应改为异步任务 + 进度接口。

## 6. 环境配置与启动

复制 `.env.example` 为本地 `.env`，不要提交 `.env`：

```bash
cp .env.example .env
openssl rand -base64 32  # 用于 AI_PROVIDER_ENCRYPTION_KEY
openssl rand -base64 32  # 用于 AUTH_JWT_SECRET
```

主要变量：

```dotenv
DB_HOST=数据库地址
DB_PORT=3306
DB_NAME=fridge_clear
DB_USERNAME=fridge_clear
DB_PASSWORD=数据库密码
DB_USE_SSL=true
AI_PROVIDER_ENCRYPTION_KEY=Base64密钥
AUTH_JWT_SECRET=Base64密钥
AUTH_JWT_EXPIRATION_SECONDS=7200
AI_PLATFORM_PROVIDER_NAME=全局AI服务名称（可选）
AI_PLATFORM_PROTOCOL=OPENAI_CHAT
AI_PLATFORM_BASE_URL=全局AI服务地址（可选）
AI_PLATFORM_MODEL_NAME=全局默认模型（可选）
AI_PLATFORM_API_KEY=全局平台API Key（可选）
ADMIN_BOOTSTRAP_EMAIL=首个管理员邮箱（可选）
ADMIN_BOOTSTRAP_PASSWORD=首个管理员密码（可选）
```

`AI_PLATFORM_*` 是**种子配置**：仅当 `system_ai_config` 表为空时首次启动写入一次，之后由管理端页面（`/admin/ai-config`）维护，不再读取环境变量。`ADMIN_BOOTSTRAP_*` 每次启动都会检查：邮箱用户已存在则提升为 ADMIN，不存在且有密码则创建。

启动后端：

```bash
./mvnw -q -DskipTests compile
./mvnw spring-boot:run
```

或在 IDE 启动 `FridgeClearApplication`。地址：

- API：`http://localhost:8080`
- Knife4j：`http://localhost:8080/doc.html`
- OpenAPI：`http://localhost:8080/v3/api-docs`

启动前端：

```bash
cd web
npm install
npm run dev
```

默认地址 `http://localhost:5173`，Vite 将 `/api` 代理到 `http://localhost:8080`。

安全要求：

- 不要提交数据库密码、AI Key、JWT 和完整 Token；
- 更换 `AI_PROVIDER_ENCRYPTION_KEY` 会导致历史 Provider Key 无法解密；
- 更换 `AUTH_JWT_SECRET` 会使已有 Token 全部失效；
- 生产环境应改用密钥管理服务。

## 7. API 目录

| 模块 | 前缀 | 说明 |
| --- | --- | --- |
| 认证 | `/api/v1/auth` | 注册、登录、当前用户 |
| 库存 | `/api/v1/pantry-items` | 当前用户库存 CRUD 和状态 |
| 菜谱 | `/api/v1/recipes` | 搜索、详情、图片 |
| 推荐 | `/api/v1/recommendations` | 根据库存推荐 |
| 计划 | `/api/v1/meal-plans` | AI 生成、历史、详情、归档 |
| 购物 | `/api/v1/shopping-list-items` | 购物清单状态 |
| Provider | `/api/v1/ai/providers` | 用户自配 Provider：配置、激活、测试、模型发现 |
| 全局 AI | `/api/v1/admin/ai/config` | 全局 AI 配置、模型列表、连接测试（仅 ADMIN） |
| 导入 | `/api/v1/admin/imports` | HowToCook 导入（仅 ADMIN） |
| 归一化 | `/api/v1/admin/ingredients` | 别名和库存绑定（仅 ADMIN） |

`/api/v1/admin/**` 全部要求 `ROLE_ADMIN`（`SecurityConfig` 中 `hasRole("ADMIN")`）。

所有受保护接口必须使用当前用户，不能重新引入固定的 `DEMO_USER_ID` 或 `userId=1`。

## 8. AI Provider 现状

消费级定位：**普通用户不需要配置 API Key**。生成备餐计划时 `AiChatGateway` 优先使用全局配置（`system_ai_config` 单行表，管理端维护），全局未配置/未启用时回退到用户自配 Provider（为未来 BYOK 预留）。用户端没有 AI 设置页。

API Key 加密保存（AES-GCM），接口不返回明文。协议公共逻辑（模型发现、Base URL 归一化、异常脱敏）已抽到 `AiProtocolSupport`，用户自配 Provider 与全局配置共用。

产品保留用户保存多个 Provider 并选择一个激活配置的能力；API Key 加密保存，接口不返回明文。

模型发现层已考虑：

- `OPENAI_CHAT`
- `ANTHROPIC_MESSAGES`
- `GEMINI_NATIVE`

但当前备餐实际聊天网关主要实现 `OPENAI_CHAT`。因此“可以管理多种协议”不等于“每种协议都已经可以生成计划”。后续应使用协议适配器，不应把所有服务强行伪装成 OpenAI：

```text
AiChatGateway
 └── AiProtocolAdapter
      ├── OpenAiChatAdapter
      ├── AnthropicMessagesAdapter
      ├── GeminiNativeAdapter
      └── 其他协议
```

新增协议必须同时补充模型发现、连接测试、聊天调用、超时和错误转换。

## 9. 已知问题与技术债

### 高优先级

- 用户自配 Provider 的前端管理页尚未实现，主要依靠 Knife4j（全局配置页 `/admin/ai-config` 已完成，按需再补用户 Provider 管理）；
- 备餐生成是同步请求，模型慢时等待 1–3 分钟，尚无真正异步进度；
- 需要继续检查页面加载、点击反馈、空状态、错误重试和移动端适配。

### 中优先级

- 收藏按钮仍是占位行为；
- 首页搜索按钮和部分导航交互仍需接入真实功能；
- 菜谱详情页和步骤浏览仍需完善；
- 首页“可做菜谱”还应接入真实推荐接口；
- Spring Data `PageImpl` 直接序列化有稳定性警告，应改分页 DTO；
- Hibernate SQL/bind 日志过于详细，应改为可选调试 profile；
- `web/src/mock/` 旧数据基本不再使用，确认无引用后可清理；
- 图片依赖本地 HowToCook，部署时应考虑对象存储或静态资源打包。

### 低优先级

- 合并购物清单重复食材并支持一键回写库存；
- 补充后端集成测试和前端关键流程测试；
- 对 AI 结果做 JSON Schema、菜谱存在性、禁忌和份量校验；
- 增加限流、重试、超时、审计、Token 和成本统计。

## 10. 后续路线图

> **详细计划见 [`docs/ROADMAP.md`](docs/ROADMAP.md)**（含勾选进度、当前阶段、完成标准）。  
> **当前集中：阶段 A 界面优化**，完成后再做推荐加强、收藏、异步备餐。

### 阶段一：稳定性

1. 统一异常响应；
2. 统一分页 DTO；
3. 降低默认 SQL 日志；
4. 为认证、库存、菜谱和计划补集成测试；
5. 清理无引用 mock 和重复前端逻辑。

### 阶段二：前端产品闭环

1. ✅ 全局 AI 配置页（`/admin/ai-config`，仅 ADMIN）；用户自配 Provider 管理页视 BYOK 需求再补；
2. ✅ 菜谱详情页：封面、食材、步骤、图片、来源、加入计划；
3. ✅ 首页接入真实推荐接口；
4. 🔄 **界面优化**（见 `docs/ROADMAP.md` 阶段 A）；
5. 实现收藏和个人菜谱筛选；
6. 完善历史计划、复制计划和归档确认；
7. 统一加载、错误、空状态和 PC/mobile 交互。

### 阶段三：AI Native 深化

1. 用适配器接入 Anthropic、Gemini、Grok 等协议；
2. Provider 级别配置模型能力、上下文、价格和超时；
3. 规则 + AI 混合进行食材别名和单位换算；
4. 展示推荐理由和优先消耗原因；
5. 增加失败重试、降级和结构化结果校验；
6. 记录耗时、Token、模型和失败原因。

### 阶段四：部署与开源

1. 图片迁移对象存储；
2. 增加 Docker Compose 和一键启动；
3. 增加生产配置、健康检查、日志和监控；
4. 增加 CI：编译、测试、前端构建和安全检查；
5. 完善 LICENSE、贡献指南、数据来源和版权说明；
6. 提供脱敏演示账号和种子数据。

### 阶段五：独立产品

支持家庭成员、过敏源、饮食目标、历史消耗预测、拍照/语音/条码录入，以及购物平台对接，形成“库存—推荐—采购—烹饪—反馈”闭环。

## 11. 交给其他 AI IDE 的规则

1. 先读本文和 `.md/README.md`，再修改代码；
2. 先执行 `git status`，不要覆盖已有修改；
3. 保持前后端分离，以 Knife4j/OpenAPI 为接口契约；
4. 不提交 `.env`、`data/source/HowToCook/`、数据库密码或 AI Key；
5. 不用固定用户 ID 绕过鉴权；
6. 数据库变更新增 Flyway migration，不改已执行的历史 migration；
7. 修改 API 时同步更新 DTO、前端类型、调用方和文档；
8. AI 输出必须有结构化校验、超时和错误提示；
9. 改动后至少执行后端编译和前端构建；
10. 一组可验证改动完成后再提交，提交信息用中文；
11. 发现文档和代码不一致时，以实际代码/测试为准并更新本文档。

## 12. 建议接手后的第一批任务

1. 修复 `IllegalStateException` 的统一业务错误响应；
2. 实现 AI Provider 前端管理页；
3. 实现菜谱详情页；
4. 首页推荐接入真实推荐接口；
5. 为备餐生成增加异步任务和进度接口；
6. 最后再做多协议适配器和对象存储迁移。

## 13. 交接检查清单

- [ ] 已复制并填写 `.env`
- [ ] 已准备 MySQL 和账号权限
- [ ] 已克隆 HowToCook 并执行 Git LFS
- [ ] 后端 `./mvnw -q -DskipTests compile` 通过
- [ ] 前端 `cd web && npm install && npm run build` 通过
- [ ] 已验证注册、登录、JWT 和当前用户
- [ ] 已验证库存用户隔离
- [ ] 已验证菜谱搜索和图片接口
- [ ] 已配置并激活 AI Provider
- [ ] 已生成并重新读取持久化备餐计划
- [ ] 已确认敏感文件未进入 Git
- [ ] 已更新本文档和中文提交说明

