package com.jinmo.pocketfps.gpu;

/**
 * Minimal FramePredictor stub to allow compilation. The real implementation may
 * use GPU/offthread prediction logic — keep this as a placeholder for now.
 */
public class FramePredictor {
    private static boolean enabled = false;

    public static void enable(boolean on) {
        enabled = on;
        try {
            com.jinmo.pocketfps.PerformanceTuner.LOGGER.info("FramePredictor: enabled=" + on);
        } catch (Throwable t) {
            // ignore if logger isn't available at compile/runtime in some contexts
        }
    }

    public static void disable() {
        enabled = false;
        try {
            com.jinmo.pocketfps.PerformanceTuner.LOGGER.info("FramePredictor: disabled");
        } catch (Throwable t) {
        }
    }

    public static boolean isEnabled() { return enabled; }
}
