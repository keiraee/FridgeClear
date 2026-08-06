# FridgeClear 冰箱清理助手

FridgeClear 是一个 AI Native 的食材消耗与备餐规划助手：管理冰箱库存、识别临期食材、根据库存推荐菜谱，并由 AI 生成多日备餐计划与采购清单。

## 当前能力

- 用户注册 / 登录（JWT），数据按用户隔离
- 冰箱库存 CRUD、临期标识、食材别名归一化
- HowToCook 菜谱导入、搜索、分类、详情与步骤图
- 首页库存推荐（临期加权、别名匹配、筛选：现在能做 / 高匹配 / 可尝试）
- 菜谱收藏与「我的收藏」列表
- AI 备餐计划：**异步提交 + 轮询进度**（`ai_plan_run` 记录状态）
- 备餐历史、计划项状态、采购清单
- 管理端全局 AI 配置（`/admin/ai-config`，仅 ADMIN）
- Flyway 自动迁移 MySQL 表结构

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17、Spring Boot 3、Spring Data JPA、Spring Security |
| 数据库 | MySQL 8、Flyway |
| API 文档 | OpenAPI 3、Knife4j |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Remix Icon（按需打包） |

## 本地运行

### 1. 环境配置

```bash
cp .env.example .env
# 填写 DB_*、AUTH_JWT_SECRET、AI_PROVIDER_ENCRYPTION_KEY
# 可选：AI_PLATFORM_* 首次启动写入全局 AI 配置；ADMIN_BOOTSTRAP_* 创建管理员
```

生成密钥：

```bash
openssl rand -base64 32   # AUTH_JWT_SECRET / AI_PROVIDER_ENCRYPTION_KEY
```

### 2. 菜谱数据（可选）

```bash
mkdir -p data/source
git clone --depth 1 https://github.com/Anduin2017/HowToCook.git data/source/HowToCook
```

导入接口：`POST /api/v1/admin/imports/howtocook`（需 ADMIN）

### 3. 启动

```bash
# 后端
./mvnw spring-boot:run

# 前端（另开终端）
cd web && npm install && npm run dev
```

| 服务 | 地址 |
|------|------|
| API | http://localhost:8080 |
| Knife4j | http://localhost:8080/doc.html |
| 前端 | http://localhost:5173 |

开发时可用 `--spring.profiles.active=dev` 打开 Hibernate SQL 调试日志。

### 4. 测试

集成测试使用 Testcontainers + MySQL（需本机 Docker）：

```bash
./mvnw test
```

无 Docker 时相关测试会自动跳过（`disabledWithoutDocker = true`）。

## 文档

- 开发路线图与阶段进度：[docs/ROADMAP.md](docs/ROADMAP.md)
- 项目交接与架构说明：[PROJECT_HANDOFF.md](PROJECT_HANDOFF.md)
- REST API 契约：[docs/api-contract.md](docs/api-contract.md)

## 仓库说明

- `.env`、`data/source/HowToCook/` 不提交 Git
- 数据库变更只新增 Flyway migration，勿改已执行的历史脚本
