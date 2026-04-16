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
import java.util.HashSet;
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

        List<String> chunks = new ArrayList<>();
        int length = content.length();
        for (int start = 0; start < length; start += chunkSize) {
            int end = Math.min(length, start + chunkSize);
            chunks.add(content.substring(start, end));
        }
        return chunks;
    }

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
        return tokenize(normalize(text));
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

        PriorityQueue<ChunkScore> heap = new PriorityQueue<>(Comparator.comparingInt(ChunkScore::score));
        for (RagChunk chunk : ragChunks) {
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

        StringBuilder builder = new StringBuilder();
        for (ChunkScore entry : sorted) {
            appendChunk(builder, entry.chunk());
            if (builder.length() >= ragMaxChars) {
                break;
            }
        }

        if (builder.length() > ragMaxChars) {
            return builder.substring(0, ragMaxChars);
        }

        return builder.toString().trim();
    }
}
