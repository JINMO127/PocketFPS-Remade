package com.jinmo.pocketfps;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PerformanceTuner {
    public static final Logger LOGGER = LogManager.getLogger("PocketFPS");
    
    private static float smoothedFps = 60.0f;
    private static long lastFrameTime = 0;
    private static boolean isLowPowerMode = false;
    private static boolean forcedByPlayer = false;
    
    private static boolean smoothingEnabled = true;
    private static float smoothingFactor = 0.9f;
    
    public static void register() {
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
            if (smoothingEnabled) {
                smoothedFps = smoothedFps * smoothingFactor + currentFps * (1.0f - smoothingFactor);
            } else {
                smoothedFps = currentFps;
            }
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
            LOGGER.info("⚡ 性能模式已激活 (FPS: {})", smoothedFps);
        } else {
            LOGGER.info("✅ 性能模式已关闭 (FPS: {})", smoothedFps);
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
    
    public static void setSmoothingEnabled(boolean enabled) {
        smoothingEnabled = enabled;
    }
    
    public static void setSmoothingFactor(float factor) {
        smoothingFactor = Math.max(0.1f, Math.min(0.99f, factor));
    }
}
