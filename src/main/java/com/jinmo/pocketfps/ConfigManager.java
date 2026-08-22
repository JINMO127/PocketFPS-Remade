package com.jinmo.pocketfps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("pocketfps.json");
    private static Config INSTANCE = load();
    private static long lastModified = 0;
    private static final ScheduledExecutorService watcher = Executors.newSingleThreadScheduledExecutor();
    
    static {
        try {
            if (CONFIG_PATH.toFile().exists()) {
                lastModified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
            }
        } catch (IOException ignored) {}
        
        watcher.scheduleAtFixedRate(() -> {
            try {
                if (CONFIG_PATH.toFile().exists()) {
                    long current = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
                    if (current > lastModified) {
                        lastModified = current;
                        reload();
                        PerformanceTuner.LOGGER.info("📄 配置文件已自动重载");
                    }
                }
            } catch (IOException ignored) {}
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    public static class Config {
        public boolean enableMod = true;
        public int lightFpsThreshold = 40;
        public int mediumFpsThreshold = 25;
        public int heavyFpsThreshold = 15;
        public int freezeDistanceLight = 48;
        public int freezeDistanceMedium = 32;
        public int freezeDistanceHeavy = 16;
        public int chunkThrottleMedium = 3;
        public int chunkThrottleHeavy = 5;
        public int redstoneDistanceLight = 64;
        public int redstoneDistanceMedium = 32;
        public int redstoneDistanceHeavy = 16;
        public boolean showHUDIndicator = true;
        public boolean enableFpsSmoothing = true;
        public float fpsSmoothingFactor = 0.9f;
        // 新增：功能开关
        public boolean predictorEnabled = true;
        public boolean throttlerEnabled = true;
        public boolean redstoneEnabled = true;
        public boolean entityFreezeEnabled = true;
    }
    
    private static Config load() {
        if (!CONFIG_PATH.toFile().exists()) {
            Config defaults = new Config();
            save(defaults);
            return defaults;
        }
        try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
            return GSON.fromJson(reader, Config.class);
        } catch (IOException e) {
            PerformanceTuner.LOGGER.error("无法加载配置文件", e);
            return new Config();
        }
    }
    
    private static void save(Config config) {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            PerformanceTuner.LOGGER.error("无法保存配置文件", e);
        }
    }
    
    // 新增：公开保存方法
    public static void saveConfig() {
        save(INSTANCE);
    }
    
    public static Config get() {
        return INSTANCE;
    }
    
    public static void reload() {
        INSTANCE = load();
        PerformanceTuner.setSmoothingEnabled(INSTANCE.enableFpsSmoothing);
        PerformanceTuner.setSmoothingFactor(INSTANCE.fpsSmoothingFactor);
        
        // 从配置文件恢复开关状态（不触发保存）
        PocketFPSCommand.setPredictorEnabled(INSTANCE.predictorEnabled, false);
        PocketFPSCommand.setThrottlerEnabled(INSTANCE.throttlerEnabled, false);
        PocketFPSCommand.setRedstoneEnabled(INSTANCE.redstoneEnabled, false);
        PocketFPSCommand.setEntityFreezeEnabled(INSTANCE.entityFreezeEnabled, false);
        
        PerformanceTuner.LOGGER.info("✅ 配置已重新加载");
    }
}