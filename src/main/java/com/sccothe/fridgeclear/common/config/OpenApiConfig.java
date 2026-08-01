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
                        .title("FridgeClear API")
                        .version("1.0.0")
                        .description("FridgeClear 智能冰箱管理后端接口"));
    }
}
