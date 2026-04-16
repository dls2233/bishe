package com.campus.security.common.utils;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SseSessionManager {

    // 保存所有的 SseEmitter
    private static final Map<Long, SseEmitter> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 获取当前在线连接数
     */
    public static int getOnlineCount() {
        return SESSION_MAP.size();
    }

    /**
     * 添加/建立连接
     */
    public static void add(Long userId, SseEmitter emitter) {
        SESSION_MAP.put(userId, emitter);
        
        // 注册回调事件：连接断开或超时时清理Map，防止内存泄漏
        emitter.onCompletion(() -> SESSION_MAP.remove(userId));
        emitter.onTimeout(() -> SESSION_MAP.remove(userId));
        emitter.onError(e -> SESSION_MAP.remove(userId));
    }

    /**
     * 关闭连接
     */
    public static void remove(Long userId) {
        SseEmitter emitter = SESSION_MAP.get(userId);
        if (emitter != null) {
            emitter.complete();
            SESSION_MAP.remove(userId);
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    public static void broadcast(String message) {
        SESSION_MAP.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().data(message));
            } catch (Exception e) {
                // 如果发送失败，说明连接已失效，进行清理
                SESSION_MAP.remove(userId);
            }
        });
    }

    /**
     * 发送消息给指定用户
     */
    public static void sendToUser(Long userId, String message) {
        SseEmitter emitter = SESSION_MAP.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(message));
            } catch (Exception e) {
                SESSION_MAP.remove(userId);
            }
        }
    }
}
