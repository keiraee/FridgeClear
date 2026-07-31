# RecipeImporter 导入规则设计

## 1. 目标

`RecipeImporter` 负责把本地 HowToCook Markdown 文件导入 FridgeClear 数据库。

```text
data/source/HowToCook/dishes/**/*.md
        ↓
Markdown 解析
        ↓
中间对象 ParsedRecipe
        ↓
字段校验和标准化
        ↓
recipe_source_document
recipe
ingredient
recipe_ingredient
recipe_step
recipe_media
```

第一版导入器只负责可靠地保存原始内容和可确定解析的字段，不负责让 AI 猜测缺失信息。

## 2. 输入范围

### 扫描目录

```text
data/source/HowToCook/dishes/
```

### 排除目录和文件

```text
dishes/template/
隐藏目录
README.md
非 Markdown 文件
```

### 分类映射

| 目录 | 数据库分类 |
|---|---|
| `aquatic` | `AQUATIC` |
| `breakfast` | `BREAKFAST` |
| `condiment` | `CONDIMENT` |
| `dessert` | `DESSERT` |
| `drink` | `DRINK` |
| `meat_dish` | `MEAT_DISH` |
| `semi-finished` | `SEMI_FINISHED` |
| `soup` | `SOUP` |
| `staple` | `STAPLE` |
| `vegetable_dish` | `VEGETABLE_DISH` |

未知目录不能直接丢弃，应记录为 `UNKNOWN` 并生成警告。

## 3. 导入流程

```text
1. 读取当前 Git commit
2. 扫描 Markdown 文件
3. 计算文件 hash
4. 解析为 ParsedRecipe
5. 校验 ParsedRecipe
6. 保存原始文档
7. 保存菜谱元数据
8. 保存食材和菜谱关系
9. 保存步骤和图片
10. 输出导入统计报告
```

每个文件独立处理。单个菜谱失败不能导致全部菜谱回滚。

## 4. 中间对象 ParsedRecipe

导入器先生成中间对象，不直接在 Markdown 解析过程中写数据库。

```text
ParsedRecipe
├── sourcePath
├── sourceCommit
├── fileHash
├── rawMarkdown
├── name
├── category
├── description
├── difficultyText
├── difficultyLevel
├── calories
├── requiredItems[]
├── quantityItems[]
├── steps[]
├── media[]
└── additionalContent
```

这样可以先打印解析结果，确认数据正确后再写库。

## 5. Markdown 解析规则

### 5.1 菜名

读取第一个一级标题：

```markdown
# 西红柿炒鸡蛋的做法
```

处理规则：

1. 去除开头的 `#` 和空格。
2. 如果以 `的做法` 结尾，去除该后缀。
3. 保存完整原始标题到 `source_title`。
4. 处理后的名称保存到 `recipe.name`。
5. 如果没有一级标题，标记为 `FAILED`。

### 5.2 菜谱描述

读取一级标题后、第一 个二级标题前的普通文本段落。

不把图片、HTML 注释和空行保存为描述。

### 5.3 难度

查找以下格式：

```text
预估烹饪难度：简单
预估烹饪难度：★★★
预估烹饪难度：中等
```

保存规则：

```text
difficulty_text = 原始文本
difficulty_level = 映射后的 1-5
```

映射建议：

| 原始值 | level |
|---|---:|
| 简单、★ | 1 |
| 较简单、★★ | 2 |
| 中等、★★★ | 3 |
| 较难、★★★★ | 4 |
| 困难、★★★★★ | 5 |

无法识别时保留原文，`difficulty_level = null`。

### 5.4 卡路里

查找：

```text
预估卡路里：815 大卡
```

只解析明确的数字。范围、缺失或非数字内容保留原文但不写入数值字段。

### 5.5 必备原料和工具

读取 `## 必备原料和工具` 到下一个 `##` 之间的 Markdown 列表。

每一行生成一个原始条目：

```text
raw_name
sort_order
source_section = REQUIRED
```

第一版不强行区分食材和厨具：

```text
role = UNKNOWN
```

后续通过工具词表或人工确认更新为：

```text
MAIN / SEASONING / TOOL
```

### 5.6 计算和用量

读取 `## 计算` 到下一个 `##` 之间的文本和列表。

每一行先保存完整的：

```text
raw_quantity
source_section = CALCULATION
```

解析器只尝试识别明确格式：

```text
10g
10-15ml
100 克
2 个
```

对于下列内容不做强制换算：

```text
一根大葱
适量
按个人口味
一小把
按比例调整
```

此时：

```text
quantity_parse_status = UNPARSED
```

### 5.7 食材关联

`requiredItems` 和 `quantityItems` 分开解析，之后通过以下顺序尝试关联：

```text
1. 完全匹配标准名称
2. 匹配已有别名
3. 去除“（可选）”后匹配
4. 去除“（别称：xxx）”后匹配
5. 无法匹配则创建 UNKNOWN 原始条目
```

不使用模糊匹配自动合并食材，例如不能自动把“鸡胸肉”和“鸡肉”视为同一种食材。

### 5.8 操作步骤

读取 `## 操作` 到下一个 `##` 之间的有序列表。

```markdown
1. 土豆去皮、切块
2. 锅中加入食用油
```

保存：

```text
step_no
content
```

第一版不拆分：

- 温度
- 火候
- 时间
- 动作
- 判断标准

### 5.9 图片

解析 Markdown 图片：

```markdown
![西红柿炒鸡蛋](./成品.jpg)
```

保存：

```text
media_type = IMAGE
source_path = 菜谱文件所在目录 + 相对路径
alt_text = 图片描述
sort_order = 图片出现顺序
```

第一版不复制图片到 MinIO，也不调用视觉模型。

### 5.10 附加内容

完整保存 `## 附加内容` 到文件结尾的 Markdown 文本，后续用于菜谱详情和 RAG 文档。

## 6. 导入状态

### SUCCESS

满足：

- 有一级标题
- 有菜名
- 至少有一个原料或工具条目
- 至少有一个操作步骤

### PARTIAL

满足基础导入，但存在以下问题：

- 没有计算用量
- 部分用量无法解析
- 没有图片
- 食材没有匹配到标准名称
- 缺少难度或卡路里

### FAILED

无法生成有效菜谱：

- 没有一级标题
- 文件无法读取
- Markdown 编码异常
- 没有任何可识别的正文内容

## 7. 幂等和增量导入

使用以下字段判断文件是否发生变化：

```text
source_repository
source_commit
source_path
file_hash
```

规则：

```text
相同 source_path + 相同 file_hash：跳过
相同 source_path + hash 变化：更新
新 source_path：新增
旧 source_path 不再存在：标记 HIDDEN，不直接删除
```

导入必须支持重复执行，不允许每次导入都产生重复菜谱和重复食材关系。

## 8. 导入报告

每次导入完成后输出：

```text
扫描文件数
成功数
部分成功数
失败数
新增菜谱数
更新菜谱数
跳过数
未解析用量数
未匹配食材数
缺失章节数
```

失败和警告至少包含：

```text
source_path
line_number（如果可获得）
error_code
message
```

## 9. 第一版不做的事情

- 不让 AI 参与基础 Markdown 解析
- 不自动把相似名称合并成同一食材
- 不自动推断精确营养成分
- 不把 `适量` 转换成数字
- 不把步骤拆成复杂的动作图
- 不处理用户上传图片
- 不在导入阶段生成向量

向量化和 AI 解析放到基础数据成功导入之后。

## 10. 下一步

下一步才是实现导入器代码：

1. 增加 Markdown 解析依赖。
2. 实现 `ParsedRecipe` 中间对象。
3. 实现文件扫描器。
4. 实现章节解析器。
5. 实现数据库持久化。
6. 针对 3 个样例菜谱编写测试。
7. 执行全量导入并生成报告。
