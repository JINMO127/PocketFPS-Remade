package com.jinmo.pocketfps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("pocketfps.json");
    private static Config INSTANCE = load();
    
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
    
    public static Config get() {
        return INSTANCE;
    }
    
    public static void reload() {
        INSTANCE = load();
    }
}