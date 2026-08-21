package com.jinmo.pocketfps;

import net.fabricmc.api.ModInitializer;

public class PocketFPS implements ModInitializer {
    @Override
    public void onInitialize() {
        PerformanceTuner.LOGGER.info("⚡ PocketFPS 初始化中...");
        
        PerformanceTuner.register();
        PerformanceScheduler.register();
        HUDIndicator.register();
        WorldUnloadListener.register();
        
        PerformanceTuner.LOGGER.info("✅ PocketFPS 初始化完成!");
        PerformanceTuner.LOGGER.info("📐 配置文件位置: config/pocketfps.json");
    }
}