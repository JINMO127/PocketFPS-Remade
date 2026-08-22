package com.jinmo.pocketfps.lod.mixin;

/**
 * Minimal stub for ChunkUpdateThrottler to satisfy compile-time references.
 * Intended to be a lightweight placeholder — replace with full mixin logic if needed.
 */
public class ChunkUpdateThrottlerMixin {
    private static int throttleRate = 1;

    public static void setThrottleRate(int rate) {
        throttleRate = rate;
    }

    public static int getThrottleRate() {
        return throttleRate;
    }
}
