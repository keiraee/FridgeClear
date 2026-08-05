package com.sccothe.fridgeclear.common.api;

/**
 * AI 服务未配置、未启用或当前协议不支持时抛出，映射为 503 + AI_SERVICE_UNAVAILABLE。
 */
public class AiServiceUnavailableException extends RuntimeException {
    public AiServiceUnavailableException(String message) {
        super(message);
    }
}
