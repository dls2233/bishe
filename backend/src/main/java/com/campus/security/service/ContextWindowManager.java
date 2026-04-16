package com.campus.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralizes all context budgeting logic for the chat endpoint so we can
 * independently control system指令、RAG文本与历史消息的token占用。
 */
@Service
public class ContextWindowManager {

    private static final Logger log = LoggerFactory.getLogger(ContextWindowManager.class);
    private static final int MESSAGE_OVERHEAD_TOKENS = 4;

    private final int totalBudget;
    private final int systemBudget;
    private final int ragBudget;

    public ContextWindowManager(@Value("${llm.context.total-tokens:6000}") int totalBudget,
                                @Value("${llm.context.system-tokens:1200}") int systemBudget,
                                @Value("${llm.context.rag-tokens:1600}") int ragBudget) {
        this.totalBudget = totalBudget;
        this.systemBudget = systemBudget;
        this.ragBudget = ragBudget;
    }

    public List<Map<String, Object>> buildMessages(List<Map<String, Object>> history,
                                                   String systemPrompt,
                                                   String ragContext) {
        List<Map<String, Object>> finalMessages = new ArrayList<>();

        String trimmedSystem = trimToBudget(systemPrompt, systemBudget);
        if (StringUtils.hasText(trimmedSystem)) {
            finalMessages.add(message("system", trimmedSystem));
            if (StringUtils.hasText(systemPrompt) && trimmedSystem.length() < systemPrompt.length()) {
                log.debug("ContextWindow: system prompt trimmed from {} to {} chars", systemPrompt.length(), trimmedSystem.length());
            }
        }

        if (StringUtils.hasText(ragContext)) {
            String trimmedRag = trimToBudget(ragContext, ragBudget);
            if (StringUtils.hasText(trimmedRag)) {
                finalMessages.add(message("system", "【知识库参考】\n" + trimmedRag));
                if (trimmedRag.length() < ragContext.length()) {
                    log.debug("ContextWindow: rag context trimmed from {} to {} chars", ragContext.length(), trimmedRag.length());
                }
            }
        }

        int consumed = estimateTokensForMessages(finalMessages);
        int remaining = Math.max(0, totalBudget - consumed);

        finalMessages.addAll(collectHistory(history, remaining));
        return finalMessages;
    }

    private List<Map<String, Object>> collectHistory(List<Map<String, Object>> history, int remainingBudget) {
        if (CollectionUtils.isEmpty(history) || remainingBudget <= 0) {
            return List.of();
        }

        List<Map<String, Object>> retained = new ArrayList<>();
        List<Map<String, Object>> dropped = new ArrayList<>();
        int used = 0;

        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> original = history.get(i);
            String role = String.valueOf(original.getOrDefault("role", "user"));
            String content = String.valueOf(original.getOrDefault("content", ""));
            int msgTokens = estimateTokens(content) + MESSAGE_OVERHEAD_TOKENS;

            if (used + msgTokens > remainingBudget) {
                if (retained.isEmpty()) {
                    String truncated = truncateToTokens(content, remainingBudget - MESSAGE_OVERHEAD_TOKENS);
                    if (StringUtils.hasText(truncated)) {
                        retained.add(0, message(role, truncated));
                        log.debug("ContextWindow: truncated single {} message from {} chars to {} chars", role, content.length(), truncated.length());
                    }
                    used = remainingBudget;
                    continue;
                }
                dropped.add(0, message(role, content));
                continue;
            }

            retained.add(0, message(role, content));
            used += msgTokens;
        }

        if (!dropped.isEmpty() && used < remainingBudget) {
            String summary = summarizeHistory(dropped);
            int summaryBudget = remainingBudget - used - MESSAGE_OVERHEAD_TOKENS;
            String trimmedSummary = truncateToTokens(summary, summaryBudget);
            if (StringUtils.hasText(trimmedSummary)) {
                retained.add(0, message("system", trimmedSummary));
                log.debug("ContextWindow: summarized {} dropped messages into {} chars", dropped.size(), trimmedSummary.length());
            }
        }

        return retained;
    }

    private int estimateTokensForMessages(List<Map<String, Object>> messages) {
        int total = 0;
        for (Map<String, Object> message : messages) {
            total += estimateTokens(String.valueOf(message.getOrDefault("content", ""))) + MESSAGE_OVERHEAD_TOKENS;
        }
        return total;
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    private String trimToBudget(String text, int tokenBudget) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return truncateToTokens(text, tokenBudget);
    }

    private String truncateToTokens(String text, int allowedTokens) {
        if (!StringUtils.hasText(text) || allowedTokens <= 0) {
            return "";
        }
        int approxChars = (int) Math.floor(allowedTokens * 1.8);
        if (approxChars <= 0) {
            return "";
        }
        if (text.length() <= approxChars) {
            return text;
        }
        int endIdx = Math.max(0, approxChars - 3);
        return text.substring(0, endIdx) + "...";
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 1;
        }
        int length = text.codePointCount(0, text.length());
        return Math.max(1, (int) Math.ceil(length / 1.8));
    }

    private String summarizeHistory(List<Map<String, Object>> dropped) {
        StringBuilder builder = new StringBuilder("【对话摘要】");
        int limit = Math.min(dropped.size(), 4);
        for (int i = 0; i < limit; i++) {
            Map<String, Object> msg = dropped.get(i);
            String role = String.valueOf(msg.getOrDefault("role", "user"));
            String content = String.valueOf(msg.getOrDefault("content", ""));
            builder.append("\n- ").append(role).append(": ")
                    .append(shorten(content, 160));
        }
        if (dropped.size() > limit) {
            builder.append("\n- …");
        }
        return builder.toString();
    }

    private String shorten(String content, int maxChars) {
        if (!StringUtils.hasText(content) || content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, Math.max(0, maxChars - 3)) + "...";
    }
}
