package com.jobai.automation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    private static final Logger log = LoggerFactory.getLogger(McpConfig.class);

    public McpConfig() {
        log.info("MCP 配置已加载 - Jina MCP SSE 连接已启用");
    }
}