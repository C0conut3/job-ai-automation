package com.jobai.automation.metrics;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM指标端点
 * 提供LLM调用统计数据接口
 */
@RestController
@RequestMapping("/api/metrics")
public class LlmMetricsEndpoint {

    private final LlmMetricsCollector metricsCollector;

    public LlmMetricsEndpoint(LlmMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    /**
     * 获取所有LLM指标
     */
    @GetMapping("/llm")
    public Map<String, Object> getLlmMetrics() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalCalls", metricsCollector.getTotalCalls());
        result.put("totalInputTokens", metricsCollector.getTotalInputTokens());
        result.put("totalOutputTokens", metricsCollector.getTotalOutputTokens());
        result.put("estimatedCost", String.format("%.4f", metricsCollector.getEstimatedCost()));
        result.put("modelCallCount", metricsCollector.getModelCallCount());
        result.put("modelInputTokens", metricsCollector.getModelInputTokens());
        result.put("modelOutputTokens", metricsCollector.getModelOutputTokens());
        return result;
    }

    /**
     * 获取各模型调用统计
     */
    @GetMapping("/llm/models")
    public Map<String, Object> getModelMetrics() {
        Map<String, Object> result = new HashMap<>();
        result.put("callCount", metricsCollector.getModelCallCount());
        result.put("inputTokens", metricsCollector.getModelInputTokens());
        result.put("outputTokens", metricsCollector.getModelOutputTokens());
        return result;
    }

    /**
     * 获取汇总统计
     */
    @GetMapping("/llm/summary")
    public Map<String, Object> getSummary() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalCalls", metricsCollector.getTotalCalls());
        result.put("totalInputTokens", metricsCollector.getTotalInputTokens());
        result.put("totalOutputTokens", metricsCollector.getTotalOutputTokens());
        result.put("estimatedCost", String.format("%.4f", metricsCollector.getEstimatedCost()));
        return result;
    }

    /**
     * 重置统计数据
     */
    @PostMapping("/llm/reset")
    public Map<String, Object> resetMetrics() {
        metricsCollector.reset();
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "指标已重置");
        return result;
    }
}
