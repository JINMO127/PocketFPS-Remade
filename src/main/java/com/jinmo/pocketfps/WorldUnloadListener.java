package com.jinmo.pocketfps;

import com.jinmo.pocketfps.gpu.FramePredictor;
import com.jinmo.pocketfps.lod.EntityLODManager;
import com.jinmo.pocketfps.lod.PathMerger;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class WorldUnloadListener {
    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            clearAllCaches();
            restorePerformanceSettings();
        });
    }
    
    private static void clearAllCaches() {
        EntityLODManager.clearCache();
        PathMerger.clearAllCache();
        FramePredictor.disable();
    }
    
    private static void restorePerformanceSettings() {
        PerformanceScheduler.restoreAll();
        PerformanceTuner.setLowPowerMode(false);
        PerformanceTuner.LOGGER.info("🔄 已恢复所有性能设置");
    }
}
