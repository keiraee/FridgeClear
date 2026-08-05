# FridgeClear 冰箱清理助手

FridgeClear 是一个 AI Native 的食材消耗与备餐规划助手，核心目标是帮助用户管理冰箱库存、识别即将过期的食材，并根据现有库存推荐可制作的菜谱。

## 当前能力

- 库存食材新增、编辑、状态管理和删除
- 新增或编辑库存时自动匹配标准食材和食材别名
- 基于 HowToCook 菜谱库的 Markdown 导入
- 菜谱搜索、分类查询和详情查询
- 根据当前库存匹配可制作菜谱，并返回缺少食材和匹配率
- Flyway 自动创建和升级 MySQL 数据库表结构

## 技术栈

- 后端：Spring Boot 3、Java 17、Spring Data JPA
- 数据库：MySQL 8、Flyway
- API 文档：Knife4j / OpenAPI 3
- 前端：Vue 3 + Vite（位于 `web/`）

## 本地运行

1. 配置 `.env` 中的 MySQL 连接信息。
2. 如需重新导入菜谱，将 HowToCook 克隆到 `data/source/HowToCook`。
3. 启动后端：

   ```bash
   mvn spring-boot:run
   ```

4. 打开 API 文档：<http://localhost:8080/doc.html>

HowToCook 源码目录和 `.env` 不提交到 Git，数据库表会由 Flyway 自动创建和升级。

## 开发计划

当前阶段与任务清单见 **[docs/ROADMAP.md](docs/ROADMAP.md)**。  
现阶段集中 **界面优化**，完成后再进入推荐算法、收藏、异步备餐等功能。
