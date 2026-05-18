package com.jobai.automation.health;

import com.jobai.automation.config.AiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * LLM健康检查指示器
 * 检查DashScope API连接状态
 */
@Component
public class LlmHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(LlmHealthIndicator.class);

    private final AiConfig aiConfig;

    public LlmHealthIndicator(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    @Override
    public Health health() {
        try {
            // 检查DashScope API是否可达
            boolean apiReachable = checkDashScopeApi();
            
            if (apiReachable) {
                return Health.up()
                        .withDetail("status", "UP")
                        .withDetail("api-endpoint", "DashScope API")
                        .withDetail("default-model", aiConfig.getModel())
                        .withDetail("message", "LLM服务正常")
                        .build();
            } else {
                return Health.down()
                        .withDetail("status", "DOWN")
                        .withDetail("api-endpoint", "DashScope API")
                        .withDetail("message", "无法连接到LLM服务")
                        .build();
            }
        } catch (Exception e) {
            log.error("LLM健康检查失败: {}", e.getMessage());
            return Health.down(e)
                    .withDetail("status", "DOWN")
                    .withDetail("message", "健康检查异常: " + e.getMessage())
                    .build();
        }
    }

    private boolean checkDashScopeApi() {
        try {
            URL url = new URL("https://dashscope.aliyuncs.com/api/text/chat");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            // 只要能连接上（不一定是200，因为需要认证）就认为API可达
            return responseCode != -1;
        } catch (IOException e) {
            log.debug("DashScope API连接测试失败: {}", e.getMessage());
            return false;
        }
    }
}
