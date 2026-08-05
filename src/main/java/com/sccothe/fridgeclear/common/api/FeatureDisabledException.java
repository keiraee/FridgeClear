package com.sccothe.fridgeclear.common.api;

/**
 * 功能未开放时抛出（如 BYOK 关闭），映射为 403 + FEATURE_DISABLED。
 */
public class FeatureDisabledException extends RuntimeException {
    public FeatureDisabledException(String message) {
        super(message);
    }
}
