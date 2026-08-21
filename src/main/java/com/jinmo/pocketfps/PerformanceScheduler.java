package com.jinmo.pocketfps;

import com.jinmo.pocketfps.gpu.FramePredictor;
import com.jinmo.pocketfps.lod.EntityLODManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class PerformanceScheduler {
    private static int tickCounter = 0;
    private static OptimizationLevel currentLevel = OptimizationLevel.OFF;
    
    public enum OptimizationLevel {
        OFF,      // 原版
        LIGHT,    // 轻度
        MEDIUM,   // 中度
        HEAVY     // 重度
    }
    
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) {
                restoreAll();
                return;
            }
            
            tickCounter++;
            if (tickCounter % 10 != 0) return; // 每10tick检查一次
            
            float fps = PerformanceTuner.getSmoothedFps();
            boolean lowPower = PerformanceTuner.isLowPowerMode();
            
            if (!lowPower) {
                if (currentLevel != OptimizationLevel.OFF) {
                    restoreAll();
                }
                return;
            }
            
            // ✅ 四级降级策略
            OptimizationLevel targetLevel;
            if (fps < 10) {
                targetLevel = OptimizationLevel.HEAVY;
            } else if (fps < 18) {
                targetLevel = OptimizationLevel.HEAVY;
            } else if (fps < 25) {
                targetLevel = OptimizationLevel.MEDIUM;
            } else if (fps < 40) {
                targetLevel = OptimizationLevel.LIGHT;
            } else {
                targetLevel = OptimizationLevel.OFF;
            }
            
            // 避免频繁切换
            if (targetLevel == currentLevel) return;
            currentLevel = targetLevel;
            
            // 执行降级
            applyLevel(targetLevel, fps);
        });
    }
    
    private static void applyLevel(OptimizationLevel level, float fps) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        switch (level) {
            case LIGHT:
                // 轻度：降低视距 + 限制红石
                client.options.viewDistance = Math.min(client.options.viewDistance, 8);
                EntityLODManager.setFreezeDistance(48);
                RedstoneLimiterMixin.setMaxDistance(64);
                LOGGER.info("🟢 轻度优化 (FPS: {:.1f})", fps);
                break;
                
            case MEDIUM:
                // 中度：实体冻结 + 区块节流
                client.options.viewDistance = Math.min(client.options.viewDistance, 4);
                EntityLODManager.setFreezeDistance(32);
                RedstoneLimiterMixin.setMaxDistance(32);
                ChunkUpdateThrottlerMixin.setThrottleRate(3);
                LOGGER.info("🟡 中度优化 (FPS: {:.1f})", fps);
                break;
                
            case HEAVY:
                // 重度：帧预测 + 激进冻结
                client.options.viewDistance = Math.min(client.options.viewDistance, 2);
                EntityLODManager.setFreezeDistance(16);
                RedstoneLimiterMixin.setMaxDistance(16);
                ChunkUpdateThrottlerMixin.setThrottleRate(5);
                FramePredictor.enable(true);
                LOGGER.info("🔴 重度优化 (FPS: {:.1f}) - 帧预测已激活", fps);
                break;
                
            default:
                restoreAll();
                break;
        }
    }
    
    private static void restoreAll() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            // 只恢复被改过的值
            if (client.options.viewDistance < 12) {
                client.options.viewDistance = 12;
            }
        }
        EntityLODManager.clearCache();
        RedstoneLimiterMixin.setMaxDistance(-1); // 无限制
        ChunkUpdateThrottlerMixin.setThrottleRate(1);
        FramePredictor.disable();
        currentLevel = OptimizationLevel.OFF;
        LOGGER.info("✅ 所有优化已恢复");
    }
    
    public static OptimizationLevel getCurrentLevel() {
        return currentLevel;
    }
}