package com.campus.security.service;

import com.campus.security.CampusSecurityApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 幻觉降低对比实验 (A/B Test)
 * 验证：通用大模型在没有知识库时，对校园特定规范会产生幻觉（瞎编）；
 * 接入 RAG 知识库后，回答能准确包含特定标准答案。
 */
@SpringBootTest(classes = CampusSecurityApplication.class)
class RagHallucinationAbaTest {

    @Value("${llm.api-url}")
    private String apiUrl;

    @Value("${llm.api-key}")
    private String apiKey;

    @Value("${llm.model}")
    private String model;

    @Autowired
    private RagService ragService;

    @Autowired
    private ContextWindowManager contextWindowManager;

    private final RestTemplate restTemplate = new RestTemplate();

    // 精选 12 道强依赖校园特定知识库的客观题
    private static final String[][] CASES = new String[][]{
            // {question, expected_answer_keyword, 题目特点}
            {"校园消防安全基础 里宿舍十不准有哪些？", "私接电源", "通用模型可能只知道普通安全，不知本校特定十不准"},
            {"实验室安全规范与操作指南 泄漏事故要切断火源电源吗？", "切断火源和电源", "强特定动作"},
            {"校园防盗与防骗实战指南 常见的推销诈骗套路？", "推销诈骗", "通用大模型可能会泛泛而谈"},
            {"大学生心理健康与危机干预 期末焦虑表现？", "就业焦虑", "特定的知识点关联"},
            {"校园消防安全基础 被困火场用湿衣物堵门缝对吗？", "湿衣物", "具体自救动作"},
            {"实验室安全规范与操作指南 化学废液怎么收集？", "废液桶", "特定处理规范"},
            {"校园防盗与防骗实战指南 借用手机诈骗套路？", "借用手机", "具体诈骗案例"},
            {"大学生心理健康与危机干预 频繁谈论死亡是危机吗？", "频繁谈论死亡", "特定的心理健康危机信号"},
            {"校园防盗与防骗实战指南 贵重物品要锁入柜中吗？", "储物柜", "防盗特定建议"},
            {"理科实验室准入安全专项考核 题目有哪些？", "实验室", "强依赖数据库的题库内容"},
            {"校园消防安全基础 灭火器正确使用方法是什么？", "灭火器", "具体的消防设施使用"},
            {"大学生心理健康与安全必修测评 考什么？", "心理健康", "强依赖数据库的题库内容"}
    };

    @Test
    void runHallucinationAbaTest() {
        System.out.println("\n================ RAG 幻觉降低对比实验 (A/B Test) 开始 ================");
        System.out.println("对比：纯大模型 (Without RAG) vs 增强大模型 (With RAG)\n");

        int pureLlmHit = 0;
        int ragLlmHit = 0;
        int total = CASES.length;

        for (int i = 0; i < total; i++) {
            String question = CASES[i][0];
            String expectedKeyword = CASES[i][1];
            String reason = CASES[i][2];

            System.out.printf("Q%d: %s\n", (i + 1), question);
            System.out.printf("期望包含的校园特定关键词: [%s] (%s)\n", expectedKeyword, reason);

            // 1. 无 RAG 测试 (Pure LLM)
            List<Map<String, Object>> messagesWithoutRag = contextWindowManager.buildMessages(
                    new ArrayList<>(),
                    "你是一个校园安全助手，请简短回答问题。",
                    "" // 空 Context
            );
            Map<String, Object> userMsg1 = new HashMap<>();
            userMsg1.put("role", "user");
            userMsg1.put("content", question);
            messagesWithoutRag.add(userMsg1);
            
            String pureAnswer = "";
            try {
                pureAnswer = callLlm(messagesWithoutRag);
            } catch (Exception e) {
                pureAnswer = "【LLM 调用失败】" + e.getMessage();
            }
            boolean pureHit = pureAnswer.contains(expectedKeyword);
            if (pureHit) pureLlmHit++;

            // 2. 有 RAG 测试 (With RAG)
            String context = ragService.retrieveContext(question);
            List<Map<String, Object>> messagesWithRag = contextWindowManager.buildMessages(
                    new ArrayList<>(),
                    "你是一个校园安全助手，请根据提供的知识库简短回答问题。",
                    context
            );
            Map<String, Object> userMsg2 = new HashMap<>();
            userMsg2.put("role", "user");
            userMsg2.put("content", question);
            messagesWithRag.add(userMsg2);

            String ragAnswer = "";
            try {
                ragAnswer = callLlm(messagesWithRag);
            } catch (Exception e) {
                ragAnswer = "【LLM 调用失败】" + e.getMessage();
            }
            boolean ragHit = ragAnswer.contains(expectedKeyword);
            if (ragHit) ragLlmHit++;

            // 输出对比结果
            System.out.println("  [Without RAG] " + (pureHit ? "✅ 包含关键词" : "❌ 产生幻觉/未覆盖") );
            System.out.println("      回答截取: " + pureAnswer.replace("\n", " ").substring(0, Math.min(60, pureAnswer.length())) + "...");
            System.out.println("  [With RAG]    " + (ragHit ? "✅ 包含关键词" : "❌ 回答错误") );
            System.out.println("      回答截取: " + ragAnswer.replace("\n", " ").substring(0, Math.min(60, ragAnswer.length())) + "...");
            System.out.println("--------------------------------------------------");
        }

        System.out.println("\n==================== A/B Test 结果总览 ====================");
        System.out.printf("总样本数            : %d\n", total);
        System.out.printf("纯大模型准确率      : %d/%d (%.1f%%)\n", pureLlmHit, total, pureLlmHit * 100.0 / total);
        System.out.printf("RAG增强大模型准确率 : %d/%d (%.1f%%)\n", ragLlmHit, total, ragLlmHit * 100.0 / total);
        
        System.out.println("\n================ 准确率对比表格 ================");
        System.out.println("| 模型配置          | 答对题数 | 总题数 | 准确率   |");
        System.out.println("|-------------------|----------|--------|----------|");
        System.out.printf("| 纯大模型 (No RAG) | %-8d | %-6d | %-6.1f%% |\n", pureLlmHit, total, pureLlmHit * 100.0 / total);
        System.out.printf("| 增强模型 (With RAG)| %-8d | %-6d | %-6.1f%% |\n", ragLlmHit, total, ragLlmHit * 100.0 / total);
        System.out.println("===========================================================\n");

        // 验证：接入 RAG 后准确率必须显著高于无 RAG
        org.junit.jupiter.api.Assertions.assertTrue(ragLlmHit > pureLlmHit,
                "RAG 并没有显著提升回答准确率，测试失败！");
    }

    /**
     * 简单的同步 HTTP 调用 LLM（不使用流式）
     */
    private String callLlm(List<Map<String, Object>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.1); // 使用低温度减少随机性
        body.put("max_tokens", 500);
        body.put("stream", false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);
        
        if (response != null && response.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return String.valueOf(message.getOrDefault("content", ""));
            }
        }
        return "";
    }
}
