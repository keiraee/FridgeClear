# FridgeClear 图标规范

来源：[Remix Icon](https://github.com/Remix-Design/RemixIcon)（MIT 风格免费许可）

## 必须遵守

1. **只用线性（`-line`）图标**，不要混用 `-fill` 填充版。
2. **统一语义命名**：`home`、`recipe`、`pantry`… 与 `registry.ts` 中的 `IconName` 对齐。
3. **颜色用 CSS**：图标跟随 `currentColor`，页面通过文字颜色控制。
4. **页面禁止直接贴 emoji 当功能图标**；导航、按钮、空状态统一用 `FcIcon`。

## 接入方式

- 依赖：`unplugin-icons` + `@iconify-json/ri`（Remix Icon 数据集）
- 映射：`web/src/assets/icons/registry.ts`
- 组件：`<FcIcon name="pantry" :size="18" />`

新增图标时：

1. 在 [remixicon.com](https://remixicon.com) 搜索，选 `-line` 版本
2. 在 `registry.ts` 的 `ICON_NAMES` 和 `ICON_COMPONENTS` 中注册
3. 页面通过 `FcIcon` 使用，不要直接 import Remix 组件

## 使用示例

```vue
<FcIcon name="pantry" :size="18" />
<button type="button"><FcIcon name="plus" :size="16" /> 添加食材</button>
```
