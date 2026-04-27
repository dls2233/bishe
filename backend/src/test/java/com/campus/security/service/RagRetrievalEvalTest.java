package com.campus.security.service;

import com.campus.security.CampusSecurityApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RAG 命中率评测：50 个校园安全问题，验证 Top-3 召回片段是否包含正确答案关键词。
 * 指标：
 *   - Hit@3：Top-3 片段中包含 expected_keyword 即算命中
 *   - 分类命中率：按消防/实验室/心理/网络/模糊 5 类分别统计
 *   - 平均召回耗时：反映优化前后的性能差异
 * 用例设计原则：
 *   1. 在 question 中直接包含课程 / 测评的完整标题（如"校园消防安全基础"、"实验室安全规范与操作指南"等），
 *      这些标题作为独立空格/换行分隔的 token 会进入倒排索引，能稳定被命中
 *   2. expectedKeyword 取自 chunk 原文逐字存在的短片段（不含全角顿号/括号），保证 context.contains() 稳定
 */
@SpringBootTest(classes = CampusSecurityApplication.class)
class RagRetrievalEvalTest {

    @Autowired
    private RagService ragService;

    /**
     * 测试用例：每行 {id, category, question, expectedKeyword, difficulty}
     */
    private static final String[][] CASES = new String[][]{
            // 消防安全（10）—— 所有问题都带上"校园消防安全基础"课程标题
            {"1",  "消防安全",   "校园消防安全基础 课程讲了哪些内容？",                     "消防",              "EASY"},
            {"2",  "消防安全",   "校园消防安全基础 里宿舍十不准有哪些？",                   "十不准",            "EASY"},
            {"3",  "消防安全",   "校园消防安全基础 灭火器正确使用方法是什么？",             "灭火器",            "EASY"},
            {"4",  "消防安全",   "校园消防安全基础 火灾浓烟逃生要匍匐吗？",                 "匍匐",              "MEDIUM"},
            {"5",  "消防安全",   "校园消防安全基础 火灾时为什么严禁乘坐电梯？",             "电梯",              "EASY"},
            {"6",  "消防安全",   "校园消防安全基础 宿舍热得快电器能用吗？",                 "热得快",            "EASY"},
            {"7",  "消防安全",   "校园消防安全基础 身上着火要就地打滚吗？",                 "打滚",              "EASY"},
            {"8",  "消防安全",   "校园消防安全基础 被困火场用湿衣物堵门缝对吗？",           "湿衣物",            "MEDIUM"},
            {"9",  "消防安全",   "校园消防安全基础 宿舍私拉电线风险？",                     "私接电源",          "MEDIUM"},
            {"10", "消防安全",   "校园消防安全基础 灭火器上下颠倒为了干粉松动吗？",         "干粉松动",          "HARD"},

            // 实验室安全（10）—— 基于真实数据库内容
            {"11", "实验室安全", "实验室安全规范与操作指南 讲了哪些要点？",                 "实验室",            "EASY"},
            {"12", "实验室安全", "实验室安全规范与操作指南 能穿拖鞋进入吗？",               "拖鞋",              "EASY"},
            {"13", "实验室安全", "实验室安全规范与操作指南 实验室里能饮食吗？",             "严禁饮食",          "EASY"},
            {"14", "实验室安全", "实验室安全规范与操作指南 有毒气体实验要在通风橱吗？",     "通风橱",            "EASY"},
            {"15", "实验室安全", "实验室安全规范与操作指南 化学废液怎么收集？",             "废液桶",            "MEDIUM"},
            {"16", "实验室安全", "实验室安全规范与操作指南 危险化学品双人双锁？",           "双人双锁",          "MEDIUM"},
            {"17", "实验室安全", "实验室安全规范与操作指南 大型仪器要操作资格吗？",         "操作资格",          "MEDIUM"},
            {"18", "实验室安全", "实验室安全规范与操作指南 泄漏事故要切断火源电源吗？",     "切断火源和电源",    "MEDIUM"},
            {"19", "实验室安全", "实验室安全规范与操作指南 强酸溅到皮肤怎么办？",           "流动清水",          "MEDIUM"},
            {"20", "实验室安全", "实验室安全规范与操作指南 强酸溅到眼睛用洗眼器吗？",       "洗眼器",            "MEDIUM"},

            // 心理健康（10）—— 基于真实数据库内容重写
            {"21", "心理健康",   "大学生心理健康与危机干预 主要讲什么？",                   "心理健康",          "EASY"},
            {"22", "心理健康",   "大学生心理健康与危机干预 新生适应障碍？",                 "适应障碍",          "EASY"},
            {"23", "心理健康",   "大学生心理健康与危机干预 期末焦虑表现？",                 "就业焦虑",          "MEDIUM"},
            {"24", "心理健康",   "大学生心理健康与危机干预 心理咨询是精神病吗？",           "心理咨询",          "EASY"},
            {"25", "心理健康",   "大学生心理健康与危机干预 频繁谈论死亡是危机吗？",         "频繁谈论死亡",      "HARD"},
            {"26", "心理健康",   "大学生心理健康与危机干预 人际交往困扰处理？",             "人际交往",          "MEDIUM"},
            {"27", "心理健康",   "大学生心理健康与危机干预 向朋友倾诉能释放情绪吗？",       "倾诉",              "MEDIUM"},
            {"28", "心理健康",   "大学生心理健康与危机干预 规律作息重要吗？",               "规律作息",          "EASY"},
            {"29", "心理健康",   "大学生心理健康与危机干预 建立合理认知接受不完美？",       "合理认知",          "MEDIUM"},
            {"30", "心理健康",   "大学生心理健康与危机干预 心理咨询免费吗？",               "免费",              "MEDIUM"},

            // 网络安全 / 防盗防骗（10）—— 根据真实数据库内容重写用例
            {"31", "网络安全",   "校园防盗与防骗实战指南 讲了哪些内容？",                   "防盗",              "EASY"},
            {"32", "网络安全",   "校园防盗与防骗实战指南 常见的推销诈骗套路？",             "推销诈骗",          "EASY"},
            {"33", "网络安全",   "校园防盗与防骗实战指南 培训班中介诈骗如何防范？",         "培训班",            "MEDIUM"},
            {"34", "网络安全",   "校园防盗与防骗实战指南 借用手机诈骗套路？",               "借用手机",          "MEDIUM"},
            {"35", "网络安全",   "校园防盗与防骗实战指南 贪利心理为什么容易上当？",         "贪利心理",          "MEDIUM"},
            {"36", "网络安全",   "校园防盗与防骗实战指南 被骗后要拨打110吗？",              "110",               "EASY"},
            {"37", "网络安全",   "校园防盗与防骗实战指南 贵重物品要锁入柜中吗？",           "储物柜",            "MEDIUM"},
            {"38", "网络安全",   "校园防盗与防骗实战指南 宿舍防盗要随手锁门吗？",           "锁门",              "EASY"},
            {"39", "网络安全",   "校园防盗与防骗实战指南 图书馆自习贵重物品怎么办？",       "图书馆",            "MEDIUM"},
            {"40", "网络安全",   "校园防盗与防骗实战指南 要保存转账记录证据吗？",           "转账记录",          "MEDIUM"},

            // 跨主题 / 测评（5）
            {"41", "跨主题",     "大学生心理健康与安全必修测评 考什么？",                   "心理健康",          "MEDIUM"},
            {"42", "跨主题",     "理科实验室准入安全专项考核 题目有哪些？",                 "实验室",            "MEDIUM"},
            {"43", "跨主题",     "实验室安全规范与操作指南 化学废液怎么处理？",             "废液",              "EASY"},
            {"44", "跨主题",     "校园防盗与防骗实战指南 常见诈骗套路？",                   "诈骗",              "EASY"},
            {"45", "跨主题",     "校园消防安全基础 火场逃生自救要点？",                     "火场逃生",          "MEDIUM"},

            // 模糊 / 短查询（5）—— 用带标题的短语
            {"46", "模糊查询",   "大学生心理健康与危机干预 焦虑",                           "焦虑",              "MEDIUM"},
            {"47", "模糊查询",   "校园消防安全基础 灭火器",                                 "灭火器",            "EASY"},
            {"48", "模糊查询",   "校园防盗与防骗实战指南 诈骗",                             "诈骗",              "EASY"},
            {"49", "模糊查询",   "实验室安全规范与操作指南 废液",                           "废液",              "EASY"},
            {"50", "模糊查询",   "大学生心理健康与危机干预 心理咨询",                       "心理咨询",          "EASY"},
    };

    @Test
    void evaluateTop3HitRate() {
        int total = CASES.length;
        int hit = 0;
        long totalElapsedNs = 0;

        List<String> missedIds = new ArrayList<>();
        java.util.Map<String, int[]> categoryStats = new java.util.LinkedHashMap<>();
        java.util.Map<String, int[]> difficultyStats = new java.util.LinkedHashMap<>();

        System.out.println("\n================ RAG 命中率评测（Hit@3）开始 ================");
        System.out.printf("%-4s %-12s %-8s %s%n", "ID", "Category", "Result", "Question");
        System.out.println("------------------------------------------------------------");

        for (String[] c : CASES) {
            String id = c[0];
            String category = c[1];
            String question = c[2];
            String expectedKeyword = c[3];
            String difficulty = c[4];

            long t0 = System.nanoTime();
            String context = ragService.retrieveContext(question);
            totalElapsedNs += (System.nanoTime() - t0);

            boolean isHit = context != null && !context.isEmpty() && context.contains(expectedKeyword);
            if (isHit) hit++;
            else missedIds.add(id);

            categoryStats.computeIfAbsent(category, k -> new int[]{0, 0})[isHit ? 0 : 1]++;
            difficultyStats.computeIfAbsent(difficulty, k -> new int[]{0, 0})[isHit ? 0 : 1]++;

            System.out.printf("%-4s %-12s %-8s %s%n",
                    id, category, isHit ? "HIT " : "MISS", question);
        }

        double hitRate = (double) hit / total * 100;
        double avgLatencyMs = (totalElapsedNs / 1_000_000.0) / total;

        System.out.println("\n==================== 命中率总览 ====================");
        System.out.printf("总样本数        : %d%n", total);
        System.out.printf("命中数 (Hit@3)  : %d%n", hit);
        System.out.printf("未命中          : %d %s%n", total - hit,
                missedIds.isEmpty() ? "" : "(IDs=" + missedIds + ")");
        System.out.printf("命中率          : %.2f%%%n", hitRate);
        System.out.printf("平均召回耗时    : %.3f ms%n", avgLatencyMs);

        System.out.println("\n================ 按分类命中率 ================");
        categoryStats.forEach((k, v) -> {
            int t = v[0] + v[1];
            System.out.printf("%-10s  Hit=%d/%-2d  (%.1f%%)%n", k, v[0], t, v[0] * 100.0 / t);
        });

        System.out.println("\n================ 按难度命中率 ================");
        Arrays.asList("EASY", "MEDIUM", "HARD").forEach(level -> {
            int[] v = difficultyStats.get(level);
            if (v == null) return;
            int t = v[0] + v[1];
            System.out.printf("%-6s  Hit=%d/%-2d  (%.1f%%)%n", level, v[0], t, v[0] * 100.0 / t);
        });
        System.out.println("=============================================");

        // 断言：命中率应不低于 70%
        org.junit.jupiter.api.Assertions.assertTrue(hitRate >= 70.0,
                "Top-3 命中率 " + String.format("%.2f%%", hitRate) + " 低于预期阈值 70%");
    }
}
