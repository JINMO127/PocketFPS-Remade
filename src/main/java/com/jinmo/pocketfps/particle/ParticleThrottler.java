package com.jinmo.pocketfps.particle;

import net.minecraft.util.math.Vec3d;

public class ParticleThrottler {
    private static boolean enabled = false;
    private static int particleCounter = 0;
    private static int maxParticlesPerFrame = 100;
    private static int maxParticleDistance = 32;

    public static void setEnabled(boolean enable) {
        enabled = enable;
        if (!enable) {
            particleCounter = 0;
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

    public static void resetCounter() {
        particleCounter = 0;
    }

    public static boolean shouldThrottleParticle(Vec3d particlePos, Vec3d playerPos) {
        if (!enabled) return false;

        if (particlePos.distanceTo(playerPos) > maxParticleDistance) {
            return true;
        }

        particleCounter++;
        return particleCounter > maxParticlesPerFrame;
    }
}