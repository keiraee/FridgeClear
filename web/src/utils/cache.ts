/** 判断缓存是否在 TTL 内 */
export function isCacheFresh(fetchedAt: number | null, ttlMs: number): boolean {
  return fetchedAt !== null && Date.now() - fetchedAt < ttlMs
}
