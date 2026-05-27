package com.jobai.automation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * MCP客户端配置类
 * 当前配置：
 * - MCP客户端已启用
 * - 使用SSE连接本地MCP服务器（http://localhost:8081）
 * - MCP服务器通过独立启动类运行
 */
@Configuration
public class McpConfig {

    private static final Logger log = LoggerFactory.getLogger(McpConfig.class);

    public McpConfig() {
        log.info("MCP 客户端配置已加载 - 连接到本地MCP服务器");
    }
}