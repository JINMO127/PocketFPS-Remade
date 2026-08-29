package com.jinmo.pocketfps;

import com.jinmo.pocketfps.gpu.FramePredictor;
import com.jinmo.pocketfps.lod.EntityLODManager;
import com.jinmo.pocketfps.mixin.RedstoneHelper;
import com.jinmo.pocketfps.mixin.ChunkUpdateThrottlerMixin;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger("PocketFPS-Scheduler");
    private static int tickCounter = 0;
    private static OptimizationLevel currentLevel = OptimizationLevel.OFF;

    private static int originalViewDistance = -1;
    private static boolean viewDistanceChanged = false;

    public enum OptimizationLevel {
        OFF, LIGHT, MEDIUM, HEAVY
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) {
                restoreAll();
                return;
            }

            tickCounter++;
            if (tickCounter % 10 != 0) return;

            if (tickCounter % 100 == 0) {
                EntityLODManager.tickCleanup();
            }

            float fps = PerformanceTuner.getSmoothedFps();
            boolean lowPower = PerformanceTuner.isLowPowerMode();

            if (!lowPower) {
                if (currentLevel != OptimizationLevel.OFF) {
                    restoreAll();
                }
                return;
            }

            ConfigManager.Config config = ConfigManager.get();
            OptimizationLevel targetLevel;
            if (fps < config.heavyFpsThreshold) {
                targetLevel = OptimizationLevel.HEAVY;
            } else if (fps < config.mediumFpsThreshold) {
                targetLevel = OptimizationLevel.MEDIUM;
            } else if (fps < config.lightFpsThreshold) {
                targetLevel = OptimizationLevel.LIGHT;
            } else {
                targetLevel = OptimizationLevel.OFF;
            }

            if (targetLevel == currentLevel) return;
            currentLevel = targetLevel;

            applyLevel(targetLevel, fps);
        });
    }

    private static void applyLevel(OptimizationLevel level, float fps) {
        MinecraftClient client = MinecraftClient.getInstance();
        ConfigManager.Config config = ConfigManager.get();

        if (!viewDistanceChanged) {
            originalViewDistance = client.options.viewDistance;
            viewDistanceChanged = true;
        }

        switch (level) {
            case LIGHT:
                client.options.viewDistance = Math.min(originalViewDistance, 8);
                EntityLODManager.setFreezeDistance(config.freezeDistanceLight);
                if (PocketFPSCommand.isRedstoneEnabled()) {
                    RedstoneHelper.setRedstoneLimit(config.redstoneDistanceLight);
                }
                LOGGER.info("🟢 轻度优化 (FPS: {})", fps);
                break;

            case MEDIUM:
                client.options.viewDistance = Math.min(originalViewDistance, 4);
                EntityLODManager.setFreezeDistance(config.freezeDistanceMedium);
                if (PocketFPSCommand.isRedstoneEnabled()) {
                    RedstoneHelper.setRedstoneLimit(config.redstoneDistanceMedium);
                }
                if (PocketFPSCommand.isThrottlerEnabled()) {
                    ChunkUpdateThrottlerMixin.setThrottleRate(config.chunkThrottleMedium);
                }
                LOGGER.info("🟡 中度优化 (FPS: {})", fps);
                break;

            case HEAVY:
                client.options.viewDistance = Math.min(originalViewDistance, 2);
                EntityLODManager.setFreezeDistance(config.freezeDistanceHeavy);
                if (PocketFPSCommand.isRedstoneEnabled()) {
                    RedstoneHelper.setRedstoneLimit(config.redstoneDistanceHeavy);
                }
                if (PocketFPSCommand.isThrottlerEnabled()) {
                    ChunkUpdateThrottlerMixin.setThrottleRate(config.chunkThrottleHeavy);
                }
                if (PocketFPSCommand.isPredictorEnabled()) {
                    FramePredictor.enable(true);
                }
                LOGGER.info("🔴 重度优化 (FPS: {}) - 帧预测已激活", fps);
                break;

            default:
                restoreAll();
                break;
        }
    }

    public static void restoreAll() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (viewDistanceChanged && originalViewDistance != -1) {
            client.options.viewDistance = originalViewDistance;
            viewDistanceChanged = false;
        }

        EntityLODManager.clearCache();
        RedstoneHelper.setRedstoneLimit(-1);
        ChunkUpdateThrottlerMixin.setThrottleRate(1);
        FramePredictor.disable();
        currentLevel = OptimizationLevel.OFF;
        LOGGER.info("✅ 所有优化已恢复");
    }

    public static OptimizationLevel getCurrentLevel() {
        return currentLevel;
    }
}
