package com.campus.security.controller;

import com.campus.security.common.result.Result;
import com.campus.security.common.utils.JwtUtils;
import com.campus.security.common.utils.SseSessionManager;
import com.campus.security.entity.Alert;
import com.campus.security.mapper.AlertMapper;
import com.campus.security.task.SmartAlertTask;
import com.campus.security.task.RealTimeHotspotAlert;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alert")
public class AlertController {

    @Autowired
    private AlertMapper alertMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SmartAlertTask smartAlertTask;

    @Autowired
    private RealTimeHotspotAlert realTimeHotspotAlert;

    /**
     * 发布预警（仅用于演示，实际项目中需要做管理员权限校验）
     */
    @PostMapping("/publish")
    public Result<String> publish(@RequestBody Alert alert, @RequestParam(required = false) Long targetUserId) {
        // 1. 将预警消息存入数据库
        alertMapper.insert(alert);

        // 2. 通过 SSE 推送给客户端
        try {
            String jsonMessage = objectMapper.writeValueAsString(alert);
            if (targetUserId != null) {
                // 如果指定了用户ID，则只推给该用户
                SseSessionManager.sendToUser(targetUserId, jsonMessage);
            } else {
                // 否则广播给所有人
                SseSessionManager.broadcast(jsonMessage);
            }
        } catch (Exception e) {
            return Result.error(500, "预警广播失败");
        }

        return Result.success("预警发布成功并已推送到在线客户端");
    }

    /**
     * 模拟给当前用户单发测试预警
     */
    @PostMapping("/test-single")
    public Result<String> testSingleAlert(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);

        Alert alert = new Alert();
        alert.setTitle("个人专属测试预警");
        alert.setContent("这是一条只发给你（用户ID: " + userId + "）的测试预警信息，其他用户收不到。");
        alert.setLevel("WARNING");

        // 不入库，直接推送测试
        try {
            String jsonMessage = objectMapper.writeValueAsString(alert);
            SseSessionManager.sendToUser(userId, jsonMessage);
        } catch (Exception e) {
            return Result.error(500, "推送失败");
        }

        return Result.success("推送成功");
    }

    /**
     * 获取预警历史列表
     */
    @GetMapping("/list")
    public Result<List<Alert>> list() {
        return Result.success(alertMapper.findAll());
    }

    /**
     * 【测试用】手动触发一次季节性预警
     */
    @PostMapping("/test-seasonal")
    public Result<String> testSeasonalAlert() {
        smartAlertTask.publishSeasonalAlert();
        return Result.success("季节性预警测试触发成功");
    }

    /**
     * 【测试用】模拟一条带危险关键词的新闻预警
     */
    @PostMapping("/test-keyword")
    public Result<String> testKeywordAlert(@RequestBody Map<String, String> params) {
        String keyword = params.getOrDefault("keyword", "诈骗");
        String level = params.getOrDefault("level", "WARNING");
        
        Alert alert = new Alert();
        alert.setTitle("🚨 安全资讯预警: " + keyword);
        alert.setContent("注意！发现与'" + keyword + "'相关的安全资讯，请务必注意安全！\n这是一条测试预警消息。");
        alert.setLevel(level);
        alert.setStatus("ACTIVE");
        
        alertMapper.insert(alert);
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(alert);
            SseSessionManager.broadcast(jsonMessage);
        } catch (Exception e) {
            return Result.error(500, "推送失败");
        }
        
        return Result.success("关键词预警测试触发成功");
    }

    /**
     * 获取所有热点事件列表
     */
    @GetMapping("/hotspots")
    public Result<List<RealTimeHotspotAlert.HotspotEvent>> getHotspots() {
        return Result.success(realTimeHotspotAlert.getAllHotspots());
    }

    /**
     * 手动触发指定热点预警
     */
    @PostMapping("/trigger-hotspot/{index}")
    public Result<String> triggerHotspot(@PathVariable int index) {
        realTimeHotspotAlert.triggerTestAlert(index);
        return Result.success("热点事件预警已触发");
    }
}
