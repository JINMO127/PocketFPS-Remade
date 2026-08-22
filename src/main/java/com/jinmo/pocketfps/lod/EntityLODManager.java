package com.jinmo.pocketfps.lod;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EntityLODManager {
    private static final Map<UUID, Long> frozenMobs = new ConcurrentHashMap<>();
    private static int freezeDistance = 32;

    public static void setFreezeDistance(int distance) {
        freezeDistance = distance;
    }

    public static boolean shouldFreeze(MobEntity mob) {
        if (mob == null || mob.world == null || !mob.isAlive()) return false;
        if (mob.removed) return false;

        PlayerEntity player = mob.world.getClosestPlayer(mob, freezeDistance);
        if (player == null) return true;

        double distSq = player.squaredDistanceTo(mob);
        double thresholdSq = freezeDistance * freezeDistance;
        return distSq > thresholdSq;
    }

    public static void freeze(MobEntity mob) {
        if (mob == null || mob.removed) return;
        UUID uuid = mob.getUuid();
        frozenMobs.put(uuid, System.currentTimeMillis());
        mob.setAiDisabled(true);
        mob.setInvisible(true);
    }

    public static void unfreeze(MobEntity mob) {
        if (mob == null) return;
        UUID uuid = mob.getUuid();
        frozenMobs.remove(uuid);
        mob.setAiDisabled(false);
        mob.setInvisible(false);
        PathMerger.cleanupTargetMaps(mob);
    }

    public static boolean isFrozen(MobEntity mob) {
        return mob != null && frozenMobs.containsKey(mob.getUuid());
    }

    public static void clearCache() {
        frozenMobs.clear();
    }

    public static void tickCleanup() {
        long now = System.currentTimeMillis();
        frozenMobs.entrySet().removeIf(entry -> now - entry.getValue() > 30000);
    }
    
    // 新增：用于命令显示
    public static int getFrozenCount() {
        return frozenMobs.size();
    }
}