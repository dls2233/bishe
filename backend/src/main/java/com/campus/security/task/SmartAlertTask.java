package com.campus.security.task;

import com.campus.security.common.utils.SseSessionManager;
import com.campus.security.entity.Alert;
import com.campus.security.entity.ExamRecord;
import com.campus.security.entity.News;
import com.campus.security.mapper.AlertMapper;
import com.campus.security.mapper.ExamRecordMapper;
import com.campus.security.mapper.NewsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 智能预警任务调度器
 * 实现多种自动预警机制：
 * 1. 安全资讯关键词预警
 * 2. 学习行为异常预警
 * 3. 时间敏感预警（节假日、考试季）
 */
@Component
public class SmartAlertTask {

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private AlertMapper alertMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // 记录已处理过的新闻ID，避免重复预警
    private Set<Long> processedNewsIds = new HashSet<>();

    // 记录已预警过的用户ID+日期组合，避免重复骚扰
    private Set<String> warnedUserDates = new HashSet<>();

    // 危险关键词列表
    private static final Map<String, String> DANGER_KEYWORDS = new HashMap<>();
    static {
        DANGER_KEYWORDS.put("诈骗", "WARNING");
        DANGER_KEYWORDS.put("诈骗", "WARNING");
        DANGER_KEYWORDS.put("火灾", "DANGER");
        DANGER_KEYWORDS.put("地震", "DANGER");
        DANGER_KEYWORDS.put("台风", "DANGER");
        DANGER_KEYWORDS.put("暴雨", "WARNING");
        DANGER_KEYWORDS.put("交通事故", "WARNING");
        DANGER_KEYWORDS.put("中毒", "DANGER");
        DANGER_KEYWORDS.put("爆炸", "DANGER");
        DANGER_KEYWORDS.put("传销", "WARNING");
        DANGER_KEYWORDS.put("校园贷", "WARNING");
        DANGER_KEYWORDS.put("溺水", "DANGER");
        DANGER_KEYWORDS.put("踩踏", "DANGER");
    }

    // 季节性安全关键词
    private static final Map<Month, String[]> SEASONAL_TOPICS = new HashMap<>();
    static {
        SEASONAL_TOPICS.put(Month.JANUARY, new String[]{"冬季防火", "用电安全"});
        SEASONAL_TOPICS.put(Month.FEBRUARY, new String[]{"春节防盗", "烟花爆竹安全"});
        SEASONAL_TOPICS.put(Month.MARCH, new String[]{"春季传染病预防", "开学季安全"});
        SEASONAL_TOPICS.put(Month.APRIL, new String[]{"踏青安全", "防火"});
        SEASONAL_TOPICS.put(Month.MAY, new String[]{"五一假期安全", "防溺水"});
        SEASONAL_TOPICS.put(Month.JUNE, new String[]{"高考中考安全", "防中暑"});
        SEASONAL_TOPICS.put(Month.JULY, new String[]{"暑期安全", "防溺水"});
        SEASONAL_TOPICS.put(Month.AUGUST, new String[]{"开学季准备", "交通安全"});
        SEASONAL_TOPICS.put(Month.SEPTEMBER, new String[]{"新生入学安全", "军训安全"});
        SEASONAL_TOPICS.put(Month.OCTOBER, new String[]{"国庆假期安全", "秋季防火"});
        SEASONAL_TOPICS.put(Month.NOVEMBER, new String[]{"秋冬季防火", "用电安全"});
        SEASONAL_TOPICS.put(Month.DECEMBER, new String[]{"年终防盗", "冬季安全"});
    }

    /**
     * 任务1：新闻安全预警（每30分钟检查一次）
     * 扫描最新新闻，检测危险关键词自动推送预警
     */
    @Scheduled(fixedRate = 1800000, initialDelay = 10000)
    public void checkNewsForAlerts() {
        System.out.println("[智能预警] 开始扫描新闻内容...");
        try {
            // 获取最近24小时内的新闻
            List<News> recentNews = newsMapper.findRecentNews(24);
            int alertCount = 0;

            for (News news : recentNews) {
                if (processedNewsIds.contains(news.getId())) {
                    continue;
                }

                String content = (news.getTitle() + " " + news.getContent()).toLowerCase();
                for (Map.Entry<String, String> entry : DANGER_KEYWORDS.entrySet()) {
                    String keyword = entry.getKey().toLowerCase();
                    if (content.contains(keyword)) {
                        // 发现危险关键词，触发预警
                        publishKeywordAlert(news, entry.getKey(), entry.getValue());
                        processedNewsIds.add(news.getId());
                        alertCount++;
                        break;
                    }
                }
            }

            if (alertCount > 0) {
                System.out.println("[智能预警] 检测到 " + alertCount + " 条安全预警，已推送!");
            }
        } catch (Exception e) {
            System.err.println("[智能预警] 新闻扫描失败: " + e.getMessage());
        }
    }

    /**
     * 任务2：学习行为异常预警（每小时检查一次）
     * 检测连续测评不通过的学生，推送安全警示
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 20000)
    public void checkLearningBehavior() {
        System.out.println("[智能预警] 开始检查学习行为...");
        try {
            LocalDate today = LocalDate.now();
            String dateKey = today.format(DateTimeFormatter.ISO_DATE);

            // 获取最近3次测评记录
            List<ExamRecord> recentRecords = examRecordMapper.findRecentRecords(3);
            
            // 按用户分组统计连续失败次数
            Map<Long, List<ExamRecord>> userRecords = new HashMap<>();
            for (ExamRecord record : recentRecords) {
                userRecords.computeIfAbsent(record.getUserId(), k -> new ArrayList<>()).add(record);
            }

            int alertCount = 0;
            for (Map.Entry<Long, List<ExamRecord>> entry : userRecords.entrySet()) {
                Long userId = entry.getKey();
                List<ExamRecord> records = entry.getValue();

                // 检查是否连续3次不通过
                long failCount = records.stream()
                        .filter(r -> !r.getIsPass())
                        .count();
                
                String userDateKey = userId + "_" + dateKey;
                if (failCount >= 3 && !warnedUserDates.contains(userDateKey)) {
                    publishLearningAlert(userId, records.size());
                    warnedUserDates.add(userDateKey);
                    alertCount++;
                }
            }

            if (alertCount > 0) {
                System.out.println("[智能预警] 向 " + alertCount + " 名学生推送学习预警!");
            }
        } catch (Exception e) {
            System.err.println("[智能预警] 学习行为检查失败: " + e.getMessage());
        }
    }

    /**
     * 任务3：季节性/节假日预警（每天早上8点推送）
     */
    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Shanghai")
    public void publishSeasonalAlert() {
        System.out.println("[智能预警] 发布季节性安全提醒...");
        try {
            LocalDate today = LocalDate.now();
            Month month = today.getMonth();
            
            String[] topics = SEASONAL_TOPICS.getOrDefault(month, new String[]{"日常安全"});
            String topic = topics[new Random().nextInt(topics.length)];

            String title = "📅 " + today.getMonthValue() + "月安全提醒";
            String content = generateSeasonalContent(topic, today);

            Alert alert = new Alert();
            alert.setTitle(title);
            alert.setContent(content);
            alert.setLevel("INFO");
            alert.setStatus("ACTIVE");
            
            alertMapper.insert(alert);
            broadcastAlert(alert);
            System.out.println("[智能预警] 季节性提醒已发布: " + title);
        } catch (Exception e) {
            System.err.println("[智能预警] 季节性提醒失败: " + e.getMessage());
        }
    }

    /**
     * 发布新闻关键词预警
     */
    private void publishKeywordAlert(News news, String keyword, String level) {
        Alert alert = new Alert();
        alert.setTitle("🚨 安全资讯预警: " + keyword);
        alert.setContent("注意！发现与'" + keyword + "'相关的安全资讯，请务必注意安全。\n资讯标题: " + news.getTitle());
        alert.setLevel(level);
        alert.setStatus("ACTIVE");
        
        alertMapper.insert(alert);
        broadcastAlert(alert);
    }

    /**
     * 发布学习行为预警
     */
    private void publishLearningAlert(Long userId, int failCount) {
        Alert alert = new Alert();
        alert.setTitle("📚 学习安全提醒");
        alert.setContent("您最近连续" + failCount + "次测评未通过，建议加强安全知识学习，可回顾相关课程内容。");
        alert.setLevel("WARNING");
        alert.setStatus("ACTIVE");
        
        alertMapper.insert(alert);
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(alert);
            SseSessionManager.sendToUser(userId, jsonMessage);
        } catch (Exception e) {
            System.err.println("[智能预警] 个人预警推送失败: " + e.getMessage());
        }
    }

    /**
     * 生成季节性内容
     */
    private String generateSeasonalContent(String topic, LocalDate date) {
        String[] tips = {
            "定期排查身边安全隐患，确保校园生活安全",
            "遇到紧急情况保持冷静，及时向老师和保卫处报告",
            "学习安全知识，提高自我保护意识",
            "遵守学校安全规定，共同维护安全环境"
        };
        String randomTip = tips[new Random().nextInt(tips.length)];
        
        return "【" + topic + "】\n" + date.getMonthValue() + "月已至，请同学们关注相关安全事项。\n\n" +
               "安全小贴士：" + randomTip + "。";
    }

    /**
     * 广播预警消息
     */
    private void broadcastAlert(Alert alert) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(alert);
            SseSessionManager.broadcast(jsonMessage);
        } catch (Exception e) {
            System.err.println("[智能预警] 广播失败: " + e.getMessage());
        }
    }
}
