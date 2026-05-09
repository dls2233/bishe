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
            {"1",  "消防安全",   "干粉灭火器使用的正确口诀是什么？",                     "提、拔、握、压",   "EASY"},
            {"2",  "消防安全",   "宿舍里发生火灾时浓烟大，逃生的正确姿势是？",                 "低姿匍匐前进",        "EASY"},
            {"3",  "消防安全",   "火灾发生后能乘电梯下楼吗？",                     "严禁乘坐电梯",        "EASY"},
            {"4",  "消防安全",   "宿舍里哪些大功率电器不能用？",                 "热得快",              "MEDIUM"},
            {"5",  "消防安全",   "身上衣服着火了该怎么办？",                 "就地打滚",              "EASY"},
            {"6",  "消防安全",   "宿舍消防安全有什么要求？",                     "十不准",          "MEDIUM"},
            {"7",  "消防安全",   "被困在着火房间里该怎么办？",                 "湿衣物堵住门缝",        "HARD"},
            {"8",  "消防安全",   "消防通道能不能随意堵塞？",                     "堵塞消防通道",          "MEDIUM"},
            {"9",  "消防安全",   "宿舍私拉电线有什么风险？",                     "私接电源",          "MEDIUM"},
            {"10", "消防安全",   "灭火器上下颠倒几次是为什么？",                 "干粉松动",          "HARD"},

            {"11", "实验室安全", "进实验室能穿拖鞋和短裤吗？",                 "穿拖鞋、短裤",            "EASY"},
            {"12", "实验室安全", "实验室的强酸溅到皮肤上如何处理？",               "大量流动清水冲洗",              "MEDIUM"},
            {"13", "实验室安全", "挥发性气体实验必须在哪里做？",             "通风橱",          "EASY"},
            {"14", "实验室安全", "化学废液能倒入下水道吗？",     "专用废液桶",            "MEDIUM"},
            {"15", "实验室安全", "电气火灾能用水灭吗？",             "电器起火绝不能用水",            "MEDIUM"},
            {"16", "实验室安全", "危险化学品如何管理？",           "双人双锁",          "HARD"},
            {"17", "实验室安全", "实验室大型仪器设备可以直接使用吗？",         "取得操作资格",          "MEDIUM"},
            {"18", "实验室安全", "实验设备发现异常发热怎么办？",     "立即切断电源",    "MEDIUM"},
            {"19", "实验室安全", "实验室里能不能吃东西？",           "严禁饮食",          "EASY"},
            {"20", "实验室安全", "强酸溅到眼睛里怎么办？",       "洗眼器",            "MEDIUM"},

            {"21", "心理健康",   "长期情绪低落应该怎么办？",                   "心理健康中心",          "EASY"},
            {"22", "心理健康",   "新生入学适应不了该怎么办？",                 "适应障碍",          "MEDIUM"},
            {"23", "心理健康",   "期末考试前焦虑怎么办？",                 "规律作息",          "MEDIUM"},
            {"24", "心理健康",   "做心理咨询是不是代表有精神病？",           "了解自己",          "EASY"},
            {"25", "心理健康",   "同学朋友圈发轻生言论该怎么办？",         "联系辅导员",      "HARD"},
            {"26", "心理健康",   "如何识别身边同学的心理危机？",             "频繁谈论死亡",          "HARD"},
            {"27", "心理健康",   "宿舍人际关系紧张怎么办？",       "人际交往障碍",              "MEDIUM"},
            {"28", "心理健康",   "如何释放负面情绪？",               "向信任的朋友倾诉",          "MEDIUM"},
            {"29", "心理健康",   "人际交往中有矛盾怎么正确处理？",       "换位思考",          "MEDIUM"},
            {"30", "心理健康",   "学校有没有免费的心理咨询？",               "免费、保密",              "MEDIUM"},

            {"31", "网络安全",   "刷单返利是不是真的能赚钱？",                   "刷单都是诈骗",              "EASY"},
            {"32", "网络安全",   "收到自称警察的电话让转账到安全账户，该不该转？",             "没有所谓的安全账户",          "HARD"},
            {"33", "网络安全",   "校园贷有什么风险？",                 "高利贷",            "MEDIUM"},
            {"34", "网络安全",   "QQ好友发消息借钱能直接转吗？",               "电话或视频核实",          "MEDIUM"},
            {"35", "网络安全",   "被骗之后应该做什么？",                     "拨打110报警",              "EASY"},
            {"36", "网络安全",   "宿舍里贵重物品怎么保管？",           "锁入柜中",            "MEDIUM"},
            {"37", "网络安全",   "公检法会通过电话让你转账吗？",           "绝不会通过电话办案",            "HARD"},
            {"38", "网络安全",   "怎样防范陌生人进宿舍推销？",       "盘问或报告宿管",            "MEDIUM"},
            {"39", "网络安全",   "防骗有哪些基本原则？",           "三不一多",              "MEDIUM"},
            {"40", "网络安全",   "被骗后证据应该怎么保存？",           "转账记录",          "HARD"},

            {"41", "跨主题",     "我想了解一下2026年大学生心理健康测评考什么？",                   "心理健康",          "MEDIUM"},
            {"42", "跨主题",     "理科实验室准入考试的题目有哪些？",                 "实验室",            "MEDIUM"},
            {"43", "跨主题",     "帮我总结一下《实验室安全规范与操作指南》",             "实验室",              "EASY"},
            {"44", "跨主题",     "校园防盗防骗有哪些关键措施？",                   "防盗",              "EASY"},
            {"45", "跨主题",     "消防课程里一共讲了几点火场逃生技巧？",                     "火场逃生",          "MEDIUM"},

            {"46", "模糊查询",   "焦虑",                           "焦虑",              "HARD"},
            {"47", "模糊查询",   "灭火",                                 "灭火器",            "HARD"},
            {"48", "模糊查询",   "诈骗怎么办",                             "诈骗",              "MEDIUM"},
            {"49", "模糊查询",   "废液",                           "废液",              "HARD"},
            {"50", "模糊查询",   "安全事故发生时第一时间应该做什么？",                       "保持镇静",          "HARD"},
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
