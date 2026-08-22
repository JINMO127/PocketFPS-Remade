package com.jinmo.pocketfps;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import java.util.logging.Logger;

public class PerformanceTuner {
    public static final Logger LOGGER = Logger.getLogger("PocketFPS");
    
    private static float smoothedFps = 60.0f;
    private static long lastFrameTime = 0;
    private static boolean isLowPowerMode = false;
    private static boolean forcedByPlayer = false;
    
    public static void register() {
        // ✅ 用 RenderTick 计算真实帧率（与 Tick 解耦）
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            updateFps();
        });
    }
    
    private static void updateFps() {
        long now = System.nanoTime();
        if (lastFrameTime == 0) {
            lastFrameTime = now;
            return;
        }
        float delta = (now - lastFrameTime) / 1_000_000_000.0f;
        lastFrameTime = now;
        if (delta > 0.001f && delta < 1.0f) {
            float currentFps = 1.0f / delta;
            smoothedFps = smoothedFps * 0.9f + currentFps * 0.1f;
        }
    }
    
    public static float getSmoothedFps() {
        return smoothedFps;
    }
    
    public static boolean isLowPowerMode() {
        return isLowPowerMode;
    }
    
    public static void setLowPowerMode(boolean active) {
        isLowPowerMode = active;
        if (active) {
            LOGGER.info(String.format("⚡ 低功耗模式已激活 (FPS: %.1f)", smoothedFps));
        } else {
            LOGGER.info(String.format("✅ 低功耗模式已关闭 (FPS: %.1f)", smoothedFps));
        }
    }
    
    public static void forceLowPower(boolean enable) {
        forcedByPlayer = enable;
        if (enable) {
            setLowPowerMode(true);
        }
    }
    
    public static boolean isForcedByPlayer() {
        return forcedByPlayer;
    }
}
