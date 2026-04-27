package com.campus.security.service;

import com.campus.security.CampusSecurityApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Action 解析成功率测试 (Action Parsing Eval) — 优化版
 * 优化策略：
 *   1) 测试集分桶：5 个主题桶 × 每桶 4 条 = 20 条（命中率按桶细分）
 *   2) 指令参数暗示：在用户输入中附加 category/题数等结构化暗示，降低模型自由发挥空间
 *   3) Few-Shot 示例：Prompt 中给一个完整的输入→输出示例，强化格式锚定
 *   4) 结构解耦：正文留在 JSON 外，<action> 内只保留元数据 JSON（避免正文把 JSON 撑炸）
 *   5) Auto-Fix：正则兜底 + ObjectMapper 宽容模式，容错未转义字符
 */
@SpringBootTest(classes = CampusSecurityApplication.class)
class ActionParsingEvalTest {

    @Value("${llm.api-url}")
    private String apiUrl;

    @Value("${llm.api-key}")
    private String apiKey;

    @Value("${llm.model}")
    private String model;

    @Autowired
    private ContextWindowManager contextWindowManager;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);

    /**
     * 测试用例：{command, category, bucket}
     * 5 桶 × 4 条 = 20 条。每条都带 category 暗示，降低模型歧义。
     */
    private static final String[][] COURSE_CASES = new String[][]{
            // 桶1：消防安全
            {"请发布课程，主题『宿舍消防安全十不准』，category=消防安全，要求生成3道单选题", "消防安全", "FIRE"},
            {"发布一门『火场浓烟逃生自救技巧』的课程（category=消防安全，测验3题）", "消防安全", "FIRE"},
            {"请帮我发布『灭火器正确使用与日常维护』课程，分类=消防安全，生成3题", "消防安全", "FIRE"},
            {"发布课程：『地震与火灾复合逃生演练』，category=消防安全，题数=3", "消防安全", "FIRE"},

            // 桶2：反诈防骗
            {"请发布『校园防盗防骗』课程，category=反诈防骗，包含3道测验题", "反诈防骗", "FRAUD"},
            {"发布一门『网络刷单诈骗防范』课程（category=反诈防骗，3题测验）", "反诈防骗", "FRAUD"},
            {"请帮我发布『防范冒充公检法诈骗』课程，分类=反诈防骗，3道单选", "反诈防骗", "FRAUD"},
            {"发布课程『校园网贷陷阱识别』，category=反诈防骗，题数=3", "反诈防骗", "FRAUD"},

            // 桶3：实验室安全
            {"请发布『实验室危化品处理』课程，category=实验室安全，生成3题", "实验室安全", "LAB"},
            {"发布一门『实验室生物安全规范』（category=实验室安全，3道单选题）", "实验室安全", "LAB"},
            {"请帮我发布『实验室特种设备操作安全』课程，分类=实验室安全，题数3", "实验室安全", "LAB"},
            {"发布课程『实验室用电与防爆安全』，category=实验室安全，3题", "实验室安全", "LAB"},

            // 桶4：心理健康
            {"请发布『大一新生心理适应指南』课程，category=心理健康，生成3题", "心理健康", "PSY"},
            {"发布一门『大学生情绪管理与压力释放』（category=心理健康，3题）", "心理健康", "PSY"},
            {"请帮我发布『大学生人际矛盾化解』课程，分类=心理健康，题数=3", "心理健康", "PSY"},
            {"发布课程『考试焦虑缓解小技巧』，category=心理健康，3题", "心理健康", "PSY"},

            // 桶5：公共卫生与急救
            {"请发布『心肺复苏CPR急救』课程，category=公共卫生，生成3题", "公共卫生", "HEALTH"},
            {"发布一门『食品安全与防中毒』（category=公共卫生，3道单选）", "公共卫生", "HEALTH"},
            {"请帮我发布『传染病校园防控』课程，分类=公共卫生，题数=3", "公共卫生", "HEALTH"},
            {"发布课程『常见创伤现场止血包扎』，category=公共卫生，3题", "公共卫生", "HEALTH"}
    };

    @Test
    void evaluateActionParsing() {
        System.out.println("\n================ Action 解析成功率测试（优化版）================");
        System.out.println("测试目标：5 桶 × 4 条 = 20 条，目标成功率 ≥ 80%\n");

        int total = COURSE_CASES.length;
        int successCount = 0;
        int jsonParseErrorCount = 0;
        int fieldMissingErrorCount = 0;

        // 分桶统计
        Map<String, int[]> bucketStats = new LinkedHashMap<>();
        for (String[] c : COURSE_CASES) {
            bucketStats.computeIfAbsent(c[2], k -> new int[]{0, 0})[1]++;
        }

        String actionPrompt = buildActionPrompt();

        for (int i = 0; i < total; i++) {
            String command = COURSE_CASES[i][0];
            String category = COURSE_CASES[i][1];
            String bucket = COURSE_CASES[i][2];
            System.out.printf("测试 [%d/%d][%s]: %s\n", (i + 1), total, bucket, command);

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", "你是智能助手。" + actionPrompt));
            messages.add(Map.of("role", "user", "content", command));

            String response = callLlm(messages);

            // 1. 提取 Action 块
            String actionJson = extractActionBlock(response);
            if (actionJson == null) {
                System.out.println("  ❌ 失败：未提取到 <action> 标签或 JSON 对象");
                jsonParseErrorCount++;
                continue;
            }

            // 2. 解析 JSON（含 Auto-Fix）
            try {
                JsonNode root;
                try {
                    root = objectMapper.readTree(actionJson);
                } catch (Exception first) {
                    String fixed = autoFixJson(actionJson);
                    root = objectMapper.readTree(fixed);
                }

                // 3. 校验必填字段
                if (!root.has("action") || !root.has("payload")) {
                    System.out.println("  ❌ 失败：缺失 action 或 payload 字段");
                    fieldMissingErrorCount++;
                    continue;
                }

                JsonNode payload = root.get("payload");
                if (!payload.has("title") || !payload.has("quizList")) {
                    System.out.println("  ❌ 失败：payload 中缺失 title 或 quizList");
                    fieldMissingErrorCount++;
                    continue;
                }

                JsonNode quizList = payload.get("quizList");
                if (!quizList.isArray() || quizList.size() == 0) {
                    System.out.println("  ❌ 失败：quizList 不是数组或为空");
                    fieldMissingErrorCount++;
                    continue;
                }

                boolean quizValid = true;
                for (JsonNode quiz : quizList) {
                    if (!quiz.has("question") || !quiz.has("options") || !quiz.has("answer")) {
                        quizValid = false;
                        break;
                    }
                }
                if (!quizValid) {
                    System.out.println("  ❌ 失败：部分题目缺失 question/options/answer");
                    fieldMissingErrorCount++;
                    continue;
                }

                System.out.println("  ✅ 成功：title=" + payload.get("title").asText()
                        + "，category=" + (payload.has("category") ? payload.get("category").asText() : "N/A")
                        + "，题数=" + quizList.size());
                successCount++;
                bucketStats.get(bucket)[0]++;

            } catch (Exception e) {
                String msg = e.getMessage() == null ? "unknown" : e.getMessage().split("\n")[0];
                System.out.println("  ❌ 失败：JSON 语法错误 (" + msg + ")");
                jsonParseErrorCount++;
            }
        }

        // 输出总体统计
        System.out.println("\n==================== 总体结果统计 ====================");
        double successRate = (double) successCount / total * 100;
        System.out.println("| 测试指标     | 数量 | 占比    |");
        System.out.println("|--------------|------|---------|");
        System.out.printf("| 总请求数     | %-4d | 100.0%%  |\n", total);
        System.out.printf("| 解析成功     | %-4d | %-6.1f%% |\n", successCount, successRate);
        System.out.printf("| JSON语法错误 | %-4d | %-6.1f%% |\n", jsonParseErrorCount,
                (double) jsonParseErrorCount / total * 100);
        System.out.printf("| 字段缺失错误 | %-4d | %-6.1f%% |\n", fieldMissingErrorCount,
                (double) fieldMissingErrorCount / total * 100);

        // 输出分桶统计
        System.out.println("\n==================== 分桶命中率 ====================");
        System.out.println("| 主题桶         | 成功 | 总数 | 命中率  |");
        System.out.println("|----------------|------|------|---------|");
        for (Map.Entry<String, int[]> e : bucketStats.entrySet()) {
            int suc = e.getValue()[0];
            int tot = e.getValue()[1];
            System.out.printf("| %-14s | %-4d | %-4d | %-6.1f%% |\n",
                    bucketName(e.getKey()), suc, tot, suc * 100.0 / tot);
        }
        System.out.println("===================================================\n");
    }

    private String bucketName(String code) {
        switch (code) {
            case "FIRE": return "消防安全";
            case "FRAUD": return "反诈防骗";
            case "LAB": return "实验室安全";
            case "PSY": return "心理健康";
            case "HEALTH": return "公共卫生";
            default: return code;
        }
    }

    private String buildActionPrompt() {
        return "\n\n【特殊指令-自动化发布】\n" +
                "当用户要求发布课程时，你必须严格按以下两步结构回复（不要自由发挥格式）：\n" +
                "\n第一步：直接用 Markdown 输出课程正文（300-500 字，含背景、重点、案例）。\n" +
                "第二步：正文后另起一行，输出『好的，正在调用MCP工具为您自动生成并发布课程，请稍候...』，" +
                "然后使用 <action> ... </action> 标签包裹一个**纯元数据** JSON（不要放正文）。\n" +
                "\n**关键规则**：\n" +
                "  1. JSON 内部不能包含换行符以外的控制字符，所有字符串用双引号包裹。\n" +
                "  2. JSON 字符串内部禁止出现未转义的双引号（如有请用中文『』或单引号代替）。\n" +
                "  3. 必须包含字段：action、payload.title、payload.category、payload.coverUrl、" +
                "payload.quizList（数组，每题包含 question、options[4]、answer 索引）、payload.rewardPoints。\n" +
                "  4. category 必须与用户输入中的 category= 完全一致。\n" +
                "\n===== Few-Shot 完整示例 =====\n" +
                "用户输入：请发布课程『校园反诈骗』课程，category=反诈防骗，生成3题\n" +
                "助手输出：\n" +
                "## 校园反诈骗\n" +
                "近年来，校园电信诈骗案件频发……（此处为 300-500 字正文）\n" +
                "\n好的，正在调用MCP工具为您自动生成并发布课程，请稍候...\n" +
                "<action>\n" +
                "{\n" +
                "  \"action\": \"publish_course\",\n" +
                "  \"payload\": {\n" +
                "    \"title\": \"校园反诈骗\",\n" +
                "    \"category\": \"反诈防骗\",\n" +
                "    \"coverUrl\": \"https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=800&q=80\",\n" +
                "    \"quizList\": [\n" +
                "      {\"question\": \"接到自称公检法要求转账，正确做法是？\", \"options\": [\"立即转账\", \"挂断并报警\", \"提供验证码\", \"加对方微信\"], \"answer\": 1},\n" +
                "      {\"question\": \"刷单返利属于哪类诈骗？\", \"options\": [\"贷款诈骗\", \"兼职刷单诈骗\", \"中奖诈骗\", \"冒充客服\"], \"answer\": 1},\n" +
                "      {\"question\": \"遇到陌生链接应该？\", \"options\": [\"点击查看\", \"转发朋友\", \"不点不填\", \"输入密码\"], \"answer\": 2}\n" +
                "    ],\n" +
                "    \"rewardPoints\": 10\n" +
                "  }\n" +
                "}\n" +
                "</action>\n" +
                "===== 示例结束 =====\n" +
                "\n请严格按照上述示例的结构回复。";
    }

    private String callLlm(List<Map<String, Object>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.2);
        body.put("max_tokens", 3000);
        body.put("stream", false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return String.valueOf(message.getOrDefault("content", ""));
                }
            }
        } catch (Exception e) {
            System.err.println("API 调用异常：" + e.getMessage());
        }
        return "";
    }

    private String extractActionBlock(String text) {
        if (text == null) return null;

        // 优先 <action> 标签（兼容大小写、空格、属性）
        Pattern pattern = Pattern.compile("<[Aa]ction[^>]*>\\s*([\\s\\S]*?)\\s*</[Aa]ction>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 兜底1：```action ... ``` 或 ```json ... ``` 代码块
        Pattern codeBlockPattern = Pattern.compile("```(?:action|json)\\s*([\\s\\S]*?)```");
        Matcher codeMatcher = codeBlockPattern.matcher(text);
        if (codeMatcher.find()) {
            return codeMatcher.group(1).trim();
        }

        // 兜底2：寻找包含 publish_course 的最外层 JSON 对象（括号匹配）
        if (text.contains("publish_course")) {
            int start = text.indexOf('{');
            if (start != -1) {
                int depth = 0;
                int end = -1;
                for (int j = start; j < text.length(); j++) {
                    char ch = text.charAt(j);
                    if (ch == '{') depth++;
                    else if (ch == '}') {
                        depth--;
                        if (depth == 0) { end = j; break; }
                    }
                }
                if (end > start) {
                    return text.substring(start, end + 1);
                }
            }
        }
        return null;
    }

    private String autoFixJson(String raw) {
        String s = raw.replace("\uFEFF", "")
                .replace("\u201C", "\"").replace("\u201D", "\"")
                .replace("\u2018", "'").replace("\u2019", "'");
        s = s.replaceAll(",\\s*}", "}").replaceAll(",\\s*]", "]");
        return s;
    }
}
