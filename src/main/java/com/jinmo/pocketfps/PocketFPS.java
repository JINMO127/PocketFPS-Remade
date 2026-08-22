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
        PocketFPSCommand.register();  // 注册命令
        
        // 从配置文件恢复开关状态
        ConfigManager.Config config = ConfigManager.get();
        PocketFPSCommand.setPredictorEnabled(config.predictorEnabled, false);
        PocketFPSCommand.setThrottlerEnabled(config.throttlerEnabled, false);
        PocketFPSCommand.setRedstoneEnabled(config.redstoneEnabled, false);
        PocketFPSCommand.setEntityFreezeEnabled(config.entityFreezeEnabled, false);
        
        PerformanceTuner.LOGGER.info("✅ PocketFPS 初始化完成!");
        PerformanceTuner.LOGGER.info("📐 配置文件位置: config/pocketfps.json");
        PerformanceTuner.LOGGER.info("💡 使用 /pocketfps 命令控制 Mod");
    }
}