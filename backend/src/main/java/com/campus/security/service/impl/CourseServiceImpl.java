package com.campus.security.service.impl;

import com.campus.security.common.result.Result;
import com.campus.security.entity.Course;
import com.campus.security.mapper.CourseMapper;
import com.campus.security.mapper.UserPointsMapper;
import com.campus.security.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);
    private static final Path COVER_DIR = Paths.get(System.getProperty("user.dir"), "uploads", "covers");
    private static final String COVER_PREFIX = "/uploads/covers/";

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private UserPointsMapper userPointsMapper;

    @Override
    public List<Course> getCourseList(Long userId) {
        List<Course> courses = courseMapper.findAllWithProgress(userId);
        for (Course course : courses) {
            ensureLocalCover(course);
        }
        return courses;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> publishCourse(Course course) {
        if (course.getTitle() == null || course.getTitle().isEmpty()) {
            return Result.error(400, "课程标题不能为空");
        }
        String originalCover = course.getCoverUrl();
        String localCover = cacheCoverIfRemote(originalCover, course.getCategory());
        if (StringUtils.hasText(localCover)) {
            course.setCoverUrl(localCover);
        }
        courseMapper.insert(course);
        return Result.success("课程发布成功");
    }

    @Override
    public Result<Course> getCourseDetail(Long id, Long userId) {
        Course course = courseMapper.findById(id);
        if (course == null) {
            return Result.error(404, "课程不存在");
        }
        ensureLocalCover(course);
        java.util.Map<String, Object> progress = courseMapper.getProgress(userId, id);
        if (progress != null) {
            course.setIsLearned(true);
            course.setProgressStatus((String) progress.get("status"));
            if (progress.get("last_answers") != null) {
                course.setLastAnswers(progress.get("last_answers").toString());
            }
        } else {
            course.setIsLearned(false);
        }
        return Result.success(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> finishCourse(Long userId, Long courseId, Boolean passed, String lastAnswers) {
        Course course = courseMapper.findById(courseId);
        if (course == null) {
            return Result.error(404, "课程不存在");
        }

        // 检查是否已经存在学习记录且已获得过积分
        java.util.Map<String, Object> progress = courseMapper.getProgress(userId, courseId);
        String existingStatus = progress != null ? (String) progress.get("status") : null;
        if ("COMPLETED".equals(existingStatus)) {
            // 如果之前已经通过了，现在又传过来了最新的答案（可能用户再次复习并提交了新的全对答案）
            // 我们依然更新 last_answers，但不给积分
            courseMapper.insertProgress(userId, courseId, "COMPLETED", lastAnswers);
            return Result.success("已经学过并完成该课程，无需重复获取积分");
        }

        if (passed) {
            // 测验通过：更新状态为 COMPLETED，并加积分
            courseMapper.insertProgress(userId, courseId, "COMPLETED", lastAnswers);
            userPointsMapper.increasePoints(userId, course.getRewardPoints());
            return Result.success("学习完成，获得 " + course.getRewardPoints() + " 积分！");
        } else {
            // 测验失败：只更新状态为 FAILED，不加积分
            courseMapper.insertProgress(userId, courseId, "FAILED", lastAnswers);
            return Result.success("测验未通过，已标记为学习结束（无积分奖励）。");
        }
    }

    private void ensureLocalCover(Course course) {
        if (course == null) {
            return;
        }
        String current = course.getCoverUrl();
        if (StringUtils.hasText(current) && current.startsWith("/uploads/")) {
            return;
        }
        String localCover = cacheCoverIfRemote(current, course.getCategory());
        if (!StringUtils.hasText(localCover) || localCover.equals(current)) {
            return;
        }
        course.setCoverUrl(localCover);
        if (course.getId() != null) {
            courseMapper.updateCover(course.getId(), localCover);
        }
    }

    private String cacheCoverIfRemote(String coverUrl, String category) {
        if (!StringUtils.hasText(coverUrl)) {
            return generatePlaceholderCover(category);
        }
        String lower = coverUrl.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return coverUrl;
        }
        try {
            Files.createDirectories(COVER_DIR);
            String extension = resolveExtension(coverUrl);
            String filename = "cover_" + System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;
            Path target = COVER_DIR.resolve(filename);

            HttpURLConnection connection = (HttpURLConnection) new URL(coverUrl).openConnection();
            connection.setRequestProperty("User-Agent", "CampusSecurityBot/1.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return COVER_PREFIX + filename;
        } catch (Exception ex) {
            log.warn("下载封面图失败：{} ({})", coverUrl, ex.getMessage());
            return generatePlaceholderCover(category);
        }
    }

    private String resolveExtension(String coverUrl) {
        String candidate = coverUrl;
        int q = candidate.indexOf('?');
        if (q >= 0) {
            candidate = candidate.substring(0, q);
        }
        int dot = candidate.lastIndexOf('.');
        if (dot >= 0 && dot > candidate.lastIndexOf('/')) {
            String ext = candidate.substring(dot).toLowerCase();
            if (ext.length() >= 2 && ext.length() <= 5) {
                return ext;
            }
        }
        return ".jpg";
    }

    private String generatePlaceholderCover(String category) {
        try {
            Files.createDirectories(COVER_DIR);
            String filename = "cover_placeholder_" + System.currentTimeMillis() + "_" + UUID.randomUUID() + ".png";
            Path target = COVER_DIR.resolve(filename);
            int width = 640;
            int height = 360;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            Color[] palette = pickPalette(category);
            GradientPaint paint = new GradientPaint(0, 0, palette[0], width, height, palette[1]);
            g2d.setPaint(paint);
            g2d.fillRect(0, 0, width, height);
            g2d.dispose();
            ImageIO.write(image, "png", target.toFile());
            return COVER_PREFIX + filename;
        } catch (IOException ex) {
            log.error("生成课程占位图失败: {}", ex.getMessage());
            return "";
        }
    }

    private Color[] pickPalette(String category) {
        Color[][] palettes = new Color[][] {
                {new Color(14, 116, 144), new Color(2, 132, 199)},
                {new Color(190, 24, 93), new Color(219, 39, 119)},
                {new Color(22, 163, 74), new Color(34, 197, 94)},
                {new Color(234, 179, 8), new Color(250, 204, 21)},
                {new Color(79, 70, 229), new Color(124, 58, 237)},
                {new Color(38, 38, 38), new Color(82, 82, 91)}
        };
        int idx = 0;
        if (StringUtils.hasText(category)) {
            idx = Math.abs(category.hashCode()) % palettes.length;
        }
        return palettes[idx];
    }
}
