package com.campus.security.service;

import com.campus.security.entity.Course;
import com.campus.security.entity.Exam;
import com.campus.security.entity.Question;
import com.campus.security.mapper.CourseMapper;
import com.campus.security.mapper.ExamMapper;
import com.campus.security.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RagService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Value("${llm.context.rag-top-k:3}")
    private int ragTopK;

    @Value("${llm.context.rag-chunk-chars:400}")
    private int ragChunkChars;

    @Value("${llm.context.rag-max-chars:1800}")
    private int ragMaxChars;

    private volatile List<RagChunk> ragChunks = List.of();
    // 倒排索引：关键词 -> 包含该关键词的 chunk 索引集合（方案一性能优化）
    private volatile Map<String, List<Integer>> invertedIndex = Map.of();
    private List<Course> allCourses = List.of();
    private List<Exam> allExams = List.of();
    private Map<Long, List<Question>> examQuestionCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshKnowledgeBase();
    }

    public synchronized void refreshKnowledgeBase() {
        List<Course> latestCourses = courseMapper.findAllWithProgress(0L);
        List<Exam> latestExams = examMapper.findAllWithProgress(0L);

        if (latestCourses == null) {
            latestCourses = Collections.emptyList();
        }
        if (latestExams == null) {
            latestExams = Collections.emptyList();
        }

        Map<Long, List<Question>> latestQuestionCache = new ConcurrentHashMap<>();
        for (Exam exam : latestExams) {
            List<Question> questions = questionMapper.findByExamId(exam.getId());
            latestQuestionCache.put(exam.getId(), questions == null ? List.of() : questions);
        }

        this.allCourses = latestCourses;
        this.allExams = latestExams;
        this.examQuestionCache = latestQuestionCache;
        rebuildChunks();
    }

    // 去除标题中的无用词汇，提取核心关键词
    private String extractCoreKeyword(String title) {
        if (title == null) return "";
        return title.replaceAll("【.*?】", "").replaceAll("^\\d{4}", "").replaceAll("必修测评", "").replaceAll("综合测评", "").trim();
    }

    private void rebuildChunks() {
        List<RagChunk> chunks = new ArrayList<>();

        for (Course course : allCourses) {
            chunks.addAll(chunkCourse(course));
        }

        for (Exam exam : allExams) {
            List<Question> questions = examQuestionCache.getOrDefault(exam.getId(), List.of());
            chunks.addAll(chunkExam(exam, questions));
        }

        this.ragChunks = chunks;
        // 方案一：构建倒排索引，将 O(N) 全量扫描降为 O(候选数)，大幅提升大规模检索性能
        this.invertedIndex = buildInvertedIndex(chunks);
    }

    /**
     * 构建倒排索引：将每个 chunk 的关键词映射到它的索引位置。
     * 查询阶段可根据 query 分词快速定位候选 chunk 子集，避免全量遍历。
     */
    private Map<String, List<Integer>> buildInvertedIndex(List<RagChunk> chunks) {
        Map<String, List<Integer>> index = new HashMap<>();
        for (int i = 0; i < chunks.size(); i++) {
            for (String keyword : chunks.get(i).keywords()) {
                if (!StringUtils.hasText(keyword)) {
                    continue;
                }
                index.computeIfAbsent(keyword, k -> new ArrayList<>()).add(i);
            }
        }
        return index;
    }

    private List<RagChunk> chunkCourse(Course course) {
        if (course == null || !StringUtils.hasText(course.getContent())) {
            return List.of();
        }

        List<RagChunk> chunks = new ArrayList<>();
        List<String> segments = splitIntoChunks(course.getContent(), ragChunkChars);
        String cleanTitle = extractCoreKeyword(course.getTitle());

        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i).trim();
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            String header = "【相关课程资料】\n课程名称：" + course.getTitle();
            if (segments.size() > 1) {
                header += "（片段" + (i + 1) + "/" + segments.size() + "）";
            }
            String text = header + "\n内容：\n" + segment;
            chunks.add(new RagChunk(
                    ChunkType.COURSE,
                    course.getId(),
                    cleanTitle,
                    text,
                    normalize(text),
                    extractKeywords(cleanTitle + " " + segment)
            ));
        }

        return chunks;
    }

    private List<RagChunk> chunkExam(Exam exam, List<Question> questions) {
        if (exam == null || questions == null || questions.isEmpty()) {
            return List.of();
        }

        List<RagChunk> chunks = new ArrayList<>();
        String cleanTitle = extractCoreKeyword(exam.getTitle());

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            StringBuilder textBuilder = new StringBuilder();
            textBuilder.append("【相关测评资料】\n")
                    .append("测评名称：").append(exam.getTitle()).append("\n")
                    .append("题目：").append(q.getContent()).append("\n");
            if (StringUtils.hasText(q.getOptions())) {
                textBuilder.append("选项：").append(q.getOptions()).append("\n");
            }
            if (StringUtils.hasText(q.getAnswer())) {
                textBuilder.append("答案：").append(q.getAnswer()).append("\n");
            }
            String text = textBuilder.toString();
            chunks.add(new RagChunk(
                    ChunkType.EXAM,
                    exam.getId(),
                    cleanTitle,
                    text,
                    normalize(text),
                    extractKeywords(cleanTitle + " " + q.getContent())
            ));
        }

        return chunks;
    }

    private void appendChunk(StringBuilder builder, RagChunk chunk) {
        if (builder.length() > 0) {
            builder.append("\n");
        }
        builder.append(chunk.text()).append("\n");
    }

    private List<String> splitIntoChunks(String content, int chunkSize) {
        if (!StringUtils.hasText(content) || chunkSize <= 0) {
            return List.of();
        }

        // 优化：按段落（换行）优先切分，避免固定长度切分把"切断电源"、"刷单返利"等短语拦腰斩断
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = content.split("\\n");
        StringBuilder current = new StringBuilder();
        for (String para : paragraphs) {
            if (current.length() + para.length() + 1 > chunkSize && current.length() > 0) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            if (para.length() > chunkSize) {
                // 段落本身过长，保存已累积内容后再按长度硬切
                if (current.length() > 0) {
                    chunks.add(current.toString());
                    current.setLength(0);
                }
                for (int start = 0; start < para.length(); start += chunkSize) {
                    int end = Math.min(para.length(), start + chunkSize);
                    chunks.add(para.substring(start, end));
                }
            } else {
                if (current.length() > 0) current.append("\n");
                current.append(para);
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    /** 领域专有短语白名单：这些词不会被默认分词切开，必须显式作为 keyword 索引 */
    private static final String[] DOMAIN_PHRASES = {
            "切断电源", "刷单返利", "安全账户", "高利贷", "锁入柜中", "电话办案",
            "三不一多", "转账记录", "学业焦虑", "人际交往障碍", "换位思考",
            "频繁谈论死亡", "适应障碍", "规律作息", "双人双锁", "操作资格",
            "通风橱", "洗眼器", "流动清水", "废液桶", "干粉松动", "私接电源",
            "湿衣物", "低姿匍匐", "就地打滚", "严禁乘坐电梯", "十不准", "热得快",
            "火场逃生", "心理咨询", "心理健康", "灭火器", "废液", "诈骗", "焦虑", "防盗"
    };

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase();
    }

    private List<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String[] tokens = text.split("[^\\p{L}\\p{N}]+");
        Set<String> result = new HashSet<>();
        for (String token : tokens) {
            if (token.length() > 1) {
                result.add(token);
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> extractKeywords(String text) {
        List<String> base = tokenize(normalize(text));
        // 补充：领域专有短语白名单，若正文中包含则显式纳入关键词索引
        if (StringUtils.hasText(text)) {
            Set<String> enriched = new LinkedHashSet<>(base);
            String lower = normalize(text);
            for (String phrase : DOMAIN_PHRASES) {
                if (lower.contains(phrase.toLowerCase())) {
                    enriched.add(phrase.toLowerCase());
                }
            }
            return new ArrayList<>(enriched);
        }
        return base;
    }

    private int scoreChunk(String normalizedQuery, List<String> queryTerms, RagChunk chunk) {
        int score = 0;
        for (String keyword : chunk.keywords()) {
            if (normalizedQuery.contains(keyword)) {
                score += 3;
            }
        }

        for (String term : queryTerms) {
            if (chunk.normalizedText().contains(term)) {
                score += 2;
            }
        }

        return score;
    }

    private String fallbackKeywordScan(String normalizedQuery) {
        StringBuilder builder = new StringBuilder();

        for (Course course : allCourses) {
            if (builder.length() >= ragMaxChars) {
                break;
            }
            String keyword = normalize(extractCoreKeyword(course.getTitle()));
            if (keyword.length() > 1 && normalizedQuery.contains(keyword)) {
                builder.append("【相关课程资料】\n")
                        .append("课程名称：").append(course.getTitle()).append("\n")
                        .append("课程内容：").append(shorten(course.getContent(), ragChunkChars)).append("\n\n");
            }
        }

        for (Exam exam : allExams) {
            if (builder.length() >= ragMaxChars) {
                break;
            }
            String keyword = normalize(extractCoreKeyword(exam.getTitle()));
            if (keyword.length() > 1 && normalizedQuery.contains(keyword)) {
                builder.append("【相关在线测评资料】\n")
                        .append("测评名称：").append(exam.getTitle()).append("\n");
                List<Question> questions = examQuestionCache.getOrDefault(exam.getId(), List.of());
                for (int i = 0; i < Math.min(questions.size(), ragTopK); i++) {
                    Question q = questions.get(i);
                    builder.append("题目：").append(q.getContent()).append("\n");
                }
                builder.append("\n");
            }
        }

        String result = builder.toString();
        if (result.length() > ragMaxChars) {
            return result.substring(0, ragMaxChars);
        }
        return result.trim();
    }

    private String shorten(String text, int maxChars) {
        if (!StringUtils.hasText(text) || text.length() <= maxChars) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private record RagChunk(ChunkType type,
                            Long sourceId,
                            String title,
                            String text,
                            String normalizedText,
                            List<String> keywords) { }

    private record ChunkScore(int score, RagChunk chunk) { }

    private enum ChunkType {
        COURSE,
        EXAM
    }

    public String retrieveContext(String userQuery) {
        if (!StringUtils.hasText(userQuery)) {
            return "";
        }

        String normalizedQuery = normalize(userQuery);
        List<String> queryTerms = tokenize(normalizedQuery);

        // 方案一：通过倒排索引定位候选 chunk，将 O(N) 线性扫描优化为 O(候选数)
        Set<Integer> candidateIdxSet = lookupCandidates(normalizedQuery, queryTerms);

        PriorityQueue<ChunkScore> heap = new PriorityQueue<>(Comparator.comparingInt(ChunkScore::score));
        List<RagChunk> chunkSnapshot = ragChunks;
        for (Integer idx : candidateIdxSet) {
            if (idx == null || idx < 0 || idx >= chunkSnapshot.size()) {
                continue;
            }
            RagChunk chunk = chunkSnapshot.get(idx);
            int score = scoreChunk(normalizedQuery, queryTerms, chunk);
            if (score <= 0) {
                continue;
            }
            heap.offer(new ChunkScore(score, chunk));
            if (heap.size() > ragTopK) {
                heap.poll();
            }
        }

        if (heap.isEmpty()) {
            return fallbackKeywordScan(normalizedQuery);
        }

        List<ChunkScore> sorted = new ArrayList<>(heap);
        sorted.sort((a, b) -> Integer.compare(b.score(), a.score()));

        // 优化：同源 chunk 补齐。当 Top-K 命中某一 course/exam 时，补充同源的其他 chunk，
        // 避免长文档被 400 字硬切后某些关键词落在未被 Top-K 选中的片段中。
        List<RagChunk> finalChunks = expandSameSourceChunks(sorted, chunkSnapshot);

        StringBuilder builder = new StringBuilder();
        for (RagChunk chunk : finalChunks) {
            appendChunk(builder, chunk);
            if (builder.length() >= ragMaxChars) {
                break;
            }
        }

        if (builder.length() > ragMaxChars) {
            return builder.substring(0, ragMaxChars);
        }

        return builder.toString().trim();
    }

    /**
     * 同源扩展：对 Top-K 中每个 chunk 所属的 (type, sourceId)，补齐该源下的所有兄弟 chunk，
     * 保证长文档中的细节关键词不会因 400 字硬切 + Top-K 限制而被丢弃。
     */
    private List<RagChunk> expandSameSourceChunks(List<ChunkScore> sorted, List<RagChunk> allChunks) {
        // 1. 收集命中的 (type, sourceId) 来源集合
        Set<String> sourceKeys = new LinkedHashSet<>();
        for (ChunkScore cs : sorted) {
            sourceKeys.add(cs.chunk().type() + "#" + cs.chunk().sourceId());
        }
        // 2. 保留原 Top-K 顺序的主命中 chunk
        List<RagChunk> expanded = new ArrayList<>();
        Set<String> addedTexts = new HashSet<>();
        for (ChunkScore cs : sorted) {
            if (addedTexts.add(cs.chunk().text())) {
                expanded.add(cs.chunk());
            }
        }
        // 3. 补齐同源兄弟 chunk（用 text 做去重，避免 record equals 冲突）
        for (RagChunk c : allChunks) {
            String key = c.type() + "#" + c.sourceId();
            if (sourceKeys.contains(key) && addedTexts.add(c.text())) {
                expanded.add(c);
            }
        }
        return expanded;
    }

    /**
     * 利用倒排索引定位候选 chunk。
     * 优先通过 query 分词命中；若无命中则退化为遍历索引 key，保证检索不遗漏子串匹配。
     */
    private Set<Integer> lookupCandidates(String normalizedQuery, List<String> queryTerms) {
        Map<String, List<Integer>> indexSnapshot = invertedIndex;
        if (indexSnapshot.isEmpty()) {
            return Set.of();
        }

        Set<Integer> candidates = new LinkedHashSet<>();

        // 1) 快速路径：query 分词精确命中索引 key
        for (String term : queryTerms) {
            List<Integer> posting = indexSnapshot.get(term);
            if (posting != null) {
                candidates.addAll(posting);
            }
        }

        // 2) 慢速兜底：遍历索引 key，按"子串包含"策略召回（覆盖中文无分隔符的场景）
        //    即使走到这里，也只是 O(K) 的字符串包含检测，K 为关键词总数，而非 O(N·L)
        if (candidates.isEmpty()) {
            for (Map.Entry<String, List<Integer>> entry : indexSnapshot.entrySet()) {
                String key = entry.getKey();
                if (key.length() > 1 && normalizedQuery.contains(key)) {
                    candidates.addAll(entry.getValue());
                }
            }
        }

        return candidates;
    }
}
