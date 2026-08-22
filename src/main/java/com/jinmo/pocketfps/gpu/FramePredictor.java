package com.jinmo.pocketfps.gpu;

import com.jinmo.pocketfps.PerformanceTuner;

public class FramePredictor {
    private static boolean enabled = false;

    public static void enable() {
        enabled = true;
        PerformanceTuner.LOGGER.info("FramePredictor: enabled");
    }

    public static void disable() {
        enabled = false;
        PerformanceTuner.LOGGER.info("FramePredictor: disabled");
    }

    public static boolean isEnabled() { return enabled; }
}
