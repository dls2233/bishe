package com.campus.security.controller;

import com.campus.security.service.ContextWindowManager;
import com.campus.security.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Value("${llm.api-key:sk-placeholder}")
    private String apiKey;

    @Value("${llm.api-url:https://api.siliconflow.cn/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${llm.model:Qwen/Qwen2.5-7B-Instruct}")
    private String model;

    @Autowired
    private RagService ragService;

    @Autowired
    private ContextWindowManager contextWindowManager;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void chat(@RequestBody Map<String, Object> requestBody, jakarta.servlet.http.HttpServletResponse httpServletResponse) {
        try {
            // 校验是否配置了有效的 API Key
            if (apiKey == null || apiKey.contains("placeholder") || apiKey.contains("please_replace")) {
                httpServletResponse.setStatus(500);
                return;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String baseSystemPrompt = "你是重游安全教育平台的智能助手。你可以和老师、学生进行日常的基础对话、聊天问候，并协助他们解决日常学习生活中的简单问题。当被问及校园防诈骗、网络安全、消防安全、实验室安全、心理健康等安全相关问题时，请务必专业、耐心、详实地解答，体现你作为安全助手的核心价值。";
            
            String actionPrompt = "\n\n【特殊指令-自动化发布】\n" +
                    "如果用户（通常是教师）明确要求你帮忙发布课程或在线测评，请你务必生成一篇极度详实的内容（必须超过 1500 字，必须包含背景介绍、详细的核心概念讲解、丰富的真实案例分析、具体的操作流程规范等长篇幅内容）以及至少 5 道题目的小测验。\n" +
                    "在发布课程前，你必须向用户确认课程主题/分类（如：网络安全、消防安全等）以及课程标题。如果用户没有提供这些信息，请先追问，**绝对不要**复用旧上下文中的主题或标题。\n" +
                    "但是，**绝对不要**在正常的回复文本中打印任何课程大纲、题目列表、详细内容或课程目标等任何实质性内容。\n" +
                    "你的回复**只能**包含一句话，请一字不差地输出：\n" +
                    "『好的，正在调用MCP工具为您自动生成并发布课程，请稍候...』\n" +
                    "在输出完这句话后，直接在末尾输出触发发布的 JSON 代码块。\n" +
                    "发布课程的代码块格式如下（请将生成的至少 1500 字的超长正文填入 content 字段。务必保证 JSON 格式完全合法且不被截断。对于封面图 coverUrl，请使用真实、绝对可用、不加防盗链的网络图片 URL。如果是食品安全，请使用：https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=800&q=80；如果是网络安全，请使用：https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=800&q=80；如果是消防安全，请使用：https://images.unsplash.com/photo-1473655584838-8924b45d2903?auto=format&fit=crop&w=800&q=80；如果是实验室安全，请使用：https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&w=800&q=80）：\n" +
                    "```action\n" +
                    "{\n" +
                    "  \"action\": \"publish_course\",\n" +
                    "  \"payload\": {\n" +
                    "    \"title\": \"课程标题\",\n" +
                    "    \"category\": \"安全分类\",\n" +
                    "    \"content\": \"这里填入你生成的极度详细的课程正文内容（必须超过 1500 字，请尽可能多地展开知识点细节。注意：请对文本内部的引号进行转义，避免破坏 JSON 结构）\",\n" +
                    "    \"coverUrl\": \"填入上述指定的真实图片链接\",\n" +
                    "    \"quizList\": [\n" +
                    "      {\"question\": \"题干\", \"options\": [\"选项内容1\", \"选项内容2\", \"选项内容3\", \"选项内容4\"], \"answer\": 0}\n" +
                    "    ],\n" +
                    "    \"rewardPoints\": 10\n" +
                    "  }\n" +
                    "}\n" +
                    "```\n" +
                    "注意：在填写 options 数组时，**绝对不要**在选项内容前加上 'A. '、'B. '、'C. '、'D. ' 的前缀，只需要填写真正的文本内容即可（例如不要写 'A. 苹果'，只写 '苹果'）。\n" +
                    "发布测评的代码块格式如下：\n" +
                    "```action\n" +
                    "{\n" +
                    "  \"action\": \"publish_exam\",\n" +
                    "  \"payload\": {\n" +
                    "    \"exam\": {\"title\": \"测评标题\", \"description\": \"测评简介\", \"timeLimit\": 30, \"totalScore\": 100, \"passScore\": 60, \"isMandatory\": true},\n" +
                    "    \"questions\": [\n" +
                    "      {\"content\": \"题干\", \"type\": \"SINGLE_CHOICE\", \"optionsList\": [\"选项内容1\", \"选项内容2\", \"选项内容3\", \"选项内容4\"], \"answer\": \"0\", \"score\": 10}\n" +
                    "    ]\n" +
                    "  }\n" +
                    "}\n" +
                    "```\n" +
                    "同样注意：在填写 optionsList 数组时，**绝对不要**带 'A. ' 等字母前缀。\n" +
                    "必须严格使用 ```action 和 ``` 包裹。普通闲聊绝不要输出此代码块。请务必保证 JSON 括号和反引号成对闭合！";
            
            baseSystemPrompt += actionPrompt;

            List<Map<String, Object>> history = new ArrayList<>();
            String lastUserQuery = "";

            if (requestBody.containsKey("messages") && requestBody.get("messages") instanceof List<?> rawHistory) {
                history.addAll((List<Map<String, Object>>) rawHistory);
                if (!history.isEmpty()) {
                    lastUserQuery = (String) history.get(history.size() - 1).getOrDefault("content", "");
                }
            } else {
                lastUserQuery = (String) requestBody.getOrDefault("message", "");
                if (lastUserQuery != null) {
                    Map<String, Object> userMessageObj = new HashMap<>();
                    userMessageObj.put("role", "user");
                    userMessageObj.put("content", lastUserQuery);
                    history.add(userMessageObj);
                }
            }

            String ragContext = "";
            if (lastUserQuery != null && !lastUserQuery.trim().isEmpty()) {
                ragContext = ragService.retrieveContext(lastUserQuery);
            }

            List<Map<String, Object>> messages = contextWindowManager.buildMessages(history, baseSystemPrompt, ragContext);
            if (messages.isEmpty()) {
                httpServletResponse.setStatus(400);
                return;
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("temperature", 0.6);
            body.put("max_tokens", 4096); // 增加 max_tokens 以防止长 JSON 被截断
            body.put("stream", true); // 开启流式输出

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.execute(apiUrl, org.springframework.http.HttpMethod.POST,
                    requestCallback -> {
                        requestCallback.getHeaders().addAll(entity.getHeaders());
                        try {
                            new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter().write(entity.getBody(), MediaType.APPLICATION_JSON, requestCallback);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    responseExtractor -> {
                        httpServletResponse.setStatus(responseExtractor.getStatusCode().value());
                        httpServletResponse.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
                        httpServletResponse.setCharacterEncoding("UTF-8");
                        httpServletResponse.setHeader("Cache-Control", "no-cache");
                        httpServletResponse.setHeader("Connection", "keep-alive");
                        
                        try (java.io.InputStream is = responseExtractor.getBody();
                             java.io.OutputStream os = httpServletResponse.getOutputStream()) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                                os.flush();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            httpServletResponse.setStatus(500);
        }
    }

    /**
     * 非流式同步调用 LLM（供前端重试 Action 使用）
     */
    @PostMapping(value = "/completions-sync", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> chatSync(@RequestBody Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", requestBody.getOrDefault("model", model));
        body.put("messages", requestBody.get("messages"));
        body.put("temperature", requestBody.getOrDefault("temperature", 0.1));
        body.put("max_tokens", requestBody.getOrDefault("max_tokens", 2000));
        body.put("stream", false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.postForObject(apiUrl, entity, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }
}
