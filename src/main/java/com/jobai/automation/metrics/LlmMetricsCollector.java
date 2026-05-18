package com.jobai.automation.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LLM调用指标收集器
 * 用于统计各模型的Token消耗和调用次数
 */
@Component
public class LlmMetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(LlmMetricsCollector.class);

    // 各模型调用次数
    private final Map<String, AtomicLong> modelCallCount = new ConcurrentHashMap<>();

    // 各模型输入Token消耗
    private final Map<String, AtomicLong> modelInputTokens = new ConcurrentHashMap<>();

    // 各模型输出Token消耗
    private final Map<String, AtomicLong> modelOutputTokens = new ConcurrentHashMap<>();

    // 总调用次数
    private final AtomicLong totalCalls = new AtomicLong(0);

    // 总输入Token
    private final AtomicLong totalInputTokens = new AtomicLong(0);

    // 总输出Token
    private final AtomicLong totalOutputTokens = new AtomicLong(0);

    /**
     * 记录LLM调用
     * @param model 模型名称
     * @param inputTokens 输入Token数
     * @param outputTokens 输出Token数
     */
    public void recordCall(String model, long inputTokens, long outputTokens) {
        // 更新模型统计
        modelCallCount.computeIfAbsent(model, k -> new AtomicLong(0)).incrementAndGet();
        modelInputTokens.computeIfAbsent(model, k -> new AtomicLong(0)).addAndGet(inputTokens);
        modelOutputTokens.computeIfAbsent(model, k -> new AtomicLong(0)).addAndGet(outputTokens);

        // 更新总计
        totalCalls.incrementAndGet();
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);

        log.debug("记录LLM调用 - 模型: {}, 输入Token: {}, 输出Token: {}", model, inputTokens, outputTokens);
    }

    /**
     * 获取所有模型调用次数
     */
    public Map<String, Long> getModelCallCount() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        modelCallCount.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    /**
     * 获取所有模型输入Token消耗
     */
    public Map<String, Long> getModelInputTokens() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        modelInputTokens.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    /**
     * 获取所有模型输出Token消耗
     */
    public Map<String, Long> getModelOutputTokens() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        modelOutputTokens.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    /**
     * 获取总调用次数
     */
    public long getTotalCalls() {
        return totalCalls.get();
    }

    /**
     * 获取总输入Token
     */
    public long getTotalInputTokens() {
        return totalInputTokens.get();
    }

    /**
     * 获取总输出Token
     */
    public long getTotalOutputTokens() {
        return totalOutputTokens.get();
    }

    /**
     * 获取总成本估算（按千 Token 计价，单位：人民币）
     * 参考阿里云 DashScope 价格（以 Qwen-Plus 为例）：
     * 输入：¥0.004/千 Token，输出：¥0.012/千 Token
     */
    public double getEstimatedCost() {
        double inputCost = totalInputTokens.get() / 1000.0 * 0.004;
        double outputCost = totalOutputTokens.get() / 1000.0 * 0.012;
        return inputCost + outputCost;
    }

    /**
     * 重置所有统计数据
     */
    public void reset() {
        modelCallCount.clear();
        modelInputTokens.clear();
        modelOutputTokens.clear();
        totalCalls.set(0);
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        log.info("LLM指标已重置");
    }
}
