package com.lskun.mcp.config;

import com.lskun.mcp.service.R2ServiceClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfiguration {

    @Bean
    public ToolCallbackProvider r2Tools(R2ServiceClient r2ServiceClient) {
        return MethodToolCallbackProvider.builder().toolObjects(r2ServiceClient).build();
    }
}