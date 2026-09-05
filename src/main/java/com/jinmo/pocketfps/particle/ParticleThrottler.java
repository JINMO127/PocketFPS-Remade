package com.jinmo.pocketfps.particle;

import net.minecraft.util.math.Vec3d;

public class ParticleThrottler {
    private static boolean enabled = false;
    private static int currentFrameParticles = 0;  // ✅ 修复：改为每帧计数
    private static int maxParticlesPerFrame = 100;
    private static int maxParticleDistance = 32;

    public static void setEnabled(boolean enable) {
        enabled = enable;
        if (!enable) {
            currentFrameParticles = 0;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setMaxDistance(int distance) {
        maxParticleDistance = distance;
    }

    public static void setMaxParticlesPerFrame(int count) {
        maxParticlesPerFrame = count;
    }

    /**
     * ✅ 修复：在每帧开始时调用此方法重置计数器
     * 之前的逻辑会导致计数器持续增长，最终所有粒子都被阻止
     */
    public static void resetCounter() {
        currentFrameParticles = 0;
    }

    public static boolean shouldThrottleParticle(Vec3d particlePos, Vec3d playerPos) {
        if (!enabled) return false;

        // 超过距离限制，不渲染
        if (particlePos.distanceTo(playerPos) > maxParticleDistance) {
            return true;
        }

        // ✅ 修复：每次调用时增加计数，不会导致永久阻止
        currentFrameParticles++;
        return currentFrameParticles > maxParticlesPerFrame;
    }
}
