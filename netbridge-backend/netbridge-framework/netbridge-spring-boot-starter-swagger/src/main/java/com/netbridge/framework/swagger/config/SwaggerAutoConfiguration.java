package com.netbridge.framework.swagger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class SwaggerAutoConfiguration {

    @Bean
    public OpenAPI netBridgeOpenApi() {
        return new OpenAPI().info(new Info().title("NetBridge API").version("v1.0.0"));
    }
}
