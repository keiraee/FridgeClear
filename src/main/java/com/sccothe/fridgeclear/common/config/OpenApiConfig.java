package com.sccothe.fridgeclear.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FridgeClear 冰箱清理助手 API")
                        .version("1.0.0")
                        .description("FridgeClear 是一个 AI Native 食材消耗与备餐规划助手。系统基于用户库存、食材保质期和菜谱数据，提供库存管理、菜谱匹配以及后续 AI 备餐计划能力。"));
    }
}
