package com.jinmo.pocketfps;

import java.util.HashMap;
import java.util.Map;

public class PathMerger {
    private static final Map<Integer, Object> targetMap = new HashMap<>();
    private static final Map<Integer, Object> mobToTarget = new HashMap<>();
    private static final Map<Integer, Long> lastUpdateTick = new HashMap<>();
    private static final Map<Integer, Object> cachedOffset = new HashMap<>();

    public static void clearAllCache() {
        targetMap.clear();
        mobToTarget.clear();
        lastUpdateTick.clear();
        cachedOffset.clear();
        try {
            PerformanceTuner.LOGGER.info("PathMerger 缓存已清空");
        } catch (Throwable t) {
        }
    }
}
