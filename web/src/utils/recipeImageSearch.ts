/**
 * 无图菜谱的第三方图片搜索链接。
 * 通过固定「搜索引擎前缀 + 菜名 + 查询后缀」保证每次搜索词格式一致。
 *
 * 可选环境变量 VITE_RECIPE_IMAGE_SEARCH_PREFIX 覆盖前缀，例如：
 * - 必应图片（默认）: https://www.bing.com/images/search?q=
 * - 百度图片: https://image.baidu.com/search/index?tn=baiduimage&word=
 */
export const RECIPE_IMAGE_SEARCH_QUERY_SUFFIX = ' 菜谱 成品'

export const DEFAULT_RECIPE_IMAGE_SEARCH_PREFIX = 'https://www.bing.com/images/search?q='

function imageSearchPrefix(): string {
  const configured = import.meta.env.VITE_RECIPE_IMAGE_SEARCH_PREFIX?.trim()
  return configured || DEFAULT_RECIPE_IMAGE_SEARCH_PREFIX
}

export function buildRecipeImageSearchUrl(recipeName: string): string {
  const name = recipeName.trim()
  if (!name) return imageSearchPrefix()
  const query = `${name}${RECIPE_IMAGE_SEARCH_QUERY_SUFFIX}`
  return `${imageSearchPrefix()}${encodeURIComponent(query)}`
}
