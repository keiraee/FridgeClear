# FridgeClear REST API 契约

## 1. 基础约定

### Base URL

```text
/api/v1
```

### Content-Type

```http
Content-Type: application/json
```

### 时间和日期

```text
日期：yyyy-MM-dd
时间：yyyy-MM-dd'T'HH:mm:ssXXX
```

### 当前用户

MVP 暂不实现登录。后端暂时使用固定演示用户 `user_id = 1`，接口中不暴露 `user_id`。

后续接入 JWT 时，不修改业务 API，只替换用户来源。

## 2. 统一响应格式

成功响应：

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "requestId": "req_01J..."
}
```

错误响应：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "请求参数不合法",
  "data": null,
  "fieldErrors": [
    {
      "field": "expireDate",
      "message": "过期日期不能早于购买日期"
    }
  ],
  "requestId": "req_01J..."
}
```

### 常用错误码

| HTTP 状态 | code | 说明 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 参数校验失败 |
| 401 | `UNAUTHORIZED` | 未登录，后续启用 |
| 404 | `RESOURCE_NOT_FOUND` | 资源不存在 |
| 409 | `RESOURCE_CONFLICT` | 数据冲突 |
| 422 | `AI_PLAN_INVALID` | AI 返回结果无法通过校验 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |
| 502 | `AI_PROVIDER_ERROR` | AI 服务调用失败 |

## 3. 食材库存 API

### 3.1 获取库存列表

```http
GET /api/v1/pantry-items
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `status` | string | 否 | `AVAILABLE`、`USED_UP`、`EXPIRED`、`DISCARDED` |
| `keyword` | string | 否 | 食材名称搜索 |
| `expiringWithinDays` | integer | 否 | N 天内过期 |
| `page` | integer | 否 | 默认 0 |
| `size` | integer | 否 | 默认 20，最大 100 |

响应 `data`：

```json
{
  "items": [
    {
      "id": 1,
      "rawName": "西红柿",
      "canonicalName": "西红柿",
      "quantity": 3,
      "unit": "个",
      "purchaseDate": "2026-08-01",
      "expireDate": "2026-08-04",
      "status": "AVAILABLE",
      "isExpiringSoon": true,
      "note": null
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

### 3.2 新增库存食材

```http
POST /api/v1/pantry-items
```

请求：

```json
{
  "rawName": "西红柿",
  "quantity": 3,
  "unit": "个",
  "purchaseDate": "2026-08-01",
  "expireDate": "2026-08-04",
  "note": "较熟，优先使用"
}
```

规则：

- `rawName` 必填
- `quantity` 可为空
- `unit` 可为空
- `expireDate` 可为空
- 如果能匹配 `ingredient`，保存 `ingredientId`
- 无法匹配时允许创建，但返回匹配提示

### 3.3 修改库存食材

```http
PUT /api/v1/pantry-items/{id}
```

请求字段与新增接口相同。

### 3.4 修改库存状态

```http
PATCH /api/v1/pantry-items/{id}/status
```

请求：

```json
{
  "status": "USED_UP"
}
```

### 3.5 删除库存食材

```http
DELETE /api/v1/pantry-items/{id}
```

成功返回 HTTP `204 No Content`，不返回业务数据。

## 4. 菜谱 API

### 4.1 菜谱列表

```http
GET /api/v1/recipes
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `keyword` | string | 否 | 菜名或食材关键词 |
| `category` | string | 否 | 菜谱分类 |
| `difficultyLevel` | integer | 否 | 最大难度 1-5 |
| `maxCookingMinutes` | integer | 否 | 第一版可暂不启用 |
| `ingredientIds` | string | 否 | 逗号分隔 |
| `page` | integer | 否 | 默认 0 |
| `size` | integer | 否 | 默认 20 |

响应 `data`：

```json
{
  "items": [
    {
      "id": 101,
      "name": "西红柿炒鸡蛋",
      "category": "VEGETABLE_DISH",
      "description": "简单易做的家常菜",
      "difficultyText": "简单",
      "difficultyLevel": 1,
      "calories": 420,
      "coverImageUrl": null,
      "ingredientCount": 6
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

### 4.2 菜谱详情

```http
GET /api/v1/recipes/{id}
```

响应 `data`：

```json
{
  "id": 101,
  "name": "西红柿炒鸡蛋",
  "category": "VEGETABLE_DISH",
  "description": "简单易做的家常菜",
  "difficultyText": "简单",
  "difficultyLevel": 1,
  "calories": 420,
  "ingredients": [
    {
      "id": 1,
      "name": "西红柿",
      "role": "MAIN",
      "isOptional": false,
      "rawQuantity": "2 个",
      "quantityMin": 2,
      "quantityMax": 2,
      "unit": "个",
      "quantityParseStatus": "PARSED"
    }
  ],
  "steps": [
    {
      "stepNo": 1,
      "content": "西红柿洗净切块，备用。"
    }
  ],
  "media": [],
  "source": {
    "repository": "https://github.com/Anduin2017/HowToCook.git",
    "path": "dishes/vegetable_dish/西红柿炒鸡蛋.md",
    "commit": "c05758f"
  }
}
```

### 4.3 库存推荐菜谱

```http
GET /api/v1/recommendations/recipes
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `limit` | int | 否 | 返回数量，默认 20，范围 1-50 |
| `filter` | string | 否 | `ready_now`（缺料 ≤ 2，首页默认；兼容 `cook_tonight`）/ `high_match`（≥ 60%）/ `all`（可尝试，≥ 40%） |

排序规则：匹配率降序 → 临期食材命中数降序 → 缺料数升序 → 菜名升序。

响应 `data.recipes[]` 字段补充：

| 字段 | 说明 |
|---|---|
| `expiringMatchedIngredients` | 命中且来自临期库存的食材名 |
| `expiringMatchedCount` | 临期命中数量 |
| `readyToCook` | 是否满足「现在能做」（缺料 ≤ 2） |

## 5. AI 备餐计划 API

### 5.1 生成备餐计划

```http
POST /api/v1/meal-plans/generate
```

请求：

```json
{
  "days": 3,
  "peopleCount": 2,
  "maxCookingMinutes": 30,
  "mealTypes": ["DINNER"],
  "dietaryPreference": "家常菜",
  "dislikedIngredients": ["香菜"],
  "availableAppliances": ["炒锅", "电饭煲"],
  "usePantryItemIds": [1, 2, 3]
}
```

参数规则：

| 字段 | 规则 |
|---|---|
| `days` | 1-7 |
| `peopleCount` | 1-10 |
| `maxCookingMinutes` | 10-180 |
| `mealTypes` | 至少一个 |
| `dietaryPreference` | 可选 |
| `dislikedIngredients` | 可选 |
| `availableAppliances` | 可选 |
| `usePantryItemIds` | 为空时默认使用全部可用库存 |

响应 `data`：

```json
{
  "mealPlanId": 2001,
  "summary": "优先消耗西红柿和鸡蛋，3 天预计需要额外采购 4 种食材。",
  "expiringIngredients": [
    {
      "pantryItemId": 1,
      "name": "西红柿",
      "expireDate": "2026-08-04",
      "reason": "临近过期，安排在第一天使用"
    }
  ],
  "items": [
    {
      "id": 3001,
      "planDate": "2026-08-02",
      "mealType": "DINNER",
      "recipe": {
        "id": 101,
        "name": "西红柿炒鸡蛋"
      },
      "servings": 2,
      "usedIngredients": ["西红柿", "鸡蛋"],
      "missingIngredients": ["食用油"],
      "reason": "库存匹配度高，烹饪时间较短",
      "status": "PLANNED"
    }
  ],
  "shoppingList": [
    {
      "name": "食用油",
      "quantity": 30,
      "unit": "ml",
      "reason": "计划中 2 道菜需要"
    }
  ]
}
```

### 5.2 查询备餐计划列表

```http
GET /api/v1/meal-plans
```

查询参数：

```text
startDate
endDate
status
page
size
```

### 5.3 查询备餐计划详情

```http
GET /api/v1/meal-plans/{id}
```

### 5.4 修改计划项状态

```http
PATCH /api/v1/meal-plans/{mealPlanId}/items/{itemId}/status
```

请求：

```json
{
  "status": "COOKED"
}
```

### 5.5 删除或归档计划

```http
DELETE /api/v1/meal-plans/{id}
```

MVP 中建议做软删除，实际更新为 `ARCHIVED`。

## 6. 采购清单 API

### 6.1 获取采购清单

```http
GET /api/v1/meal-plans/{mealPlanId}/shopping-list
```

### 6.2 修改采购状态

```http
PATCH /api/v1/shopping-list-items/{id}/status
```

请求：

```json
{
  "status": "PURCHASED"
}
```

## 7. 不作为前端 API 的功能

以下功能第一版使用命令行或内部服务完成，不暴露给 Vue：

```text
HowToCook 全量导入
菜谱重新解析
数据质量检查
菜谱隐藏和恢复
AI 向量化
```

原因是这些属于后台管理和数据工程，不属于普通用户操作。

## 8. 前端页面和 API 对应关系

| 页面 | 主要 API |
|---|---|
| Dashboard | `GET /pantry-items`、`GET /meal-plans` |
| Pantry | `GET/POST/PUT/PATCH/DELETE /pantry-items` |
| RecipeList | `GET /recipes` |
| RecipeDetail | `GET /recipes/{id}` |
| MealPlan | `POST /meal-plans/generate`、`GET /meal-plans/{id}` |
| ShoppingList | `GET /meal-plans/{id}/shopping-list`、`PATCH /shopping-list-items/{id}/status` |

## 9. API 实现顺序

1. `GET/POST/PUT/PATCH/DELETE /pantry-items`
2. `GET /recipes`
3. `GET /recipes/{id}`
4. `POST /meal-plans/generate`
5. `GET /meal-plans/{id}`
6. 采购清单接口
7. Swagger UI 和参数注解
