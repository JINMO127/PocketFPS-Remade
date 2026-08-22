package com.jinmo.pocketfps.lod;

import com.jinmo.pocketfps.PerformanceTuner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class PathMerger {
    private static final Map<UUID, List<MobEntity>> targetMap = new HashMap<>();
    private static final Map<UUID, UUID> mobToTarget = new HashMap<>();
    private static final Map<UUID, Long> lastUpdateTick = new HashMap<>();
    private static final Map<UUID, Vec3d> cachedOffset = new HashMap<>();
    private static int cleanupCounter = 0;

    public static void mergePathIfNeeded(MobEntity mob) {
        if (mob == null || mob.world == null || !mob.isAlive()) return;
        if (!PerformanceTuner.isLowPowerMode()) return;
        if (EntityLODManager.isFrozen(mob)) return;

        LivingEntity target = mob.getTarget();
        if (target == null) return;

        UUID targetUuid = target.getUuid();
        mobToTarget.put(mob.getUuid(), targetUuid);

        List<MobEntity> mobs = targetMap.computeIfAbsent(targetUuid, k -> new ArrayList<>());

        cleanupCounter++;
        if (cleanupCounter % 10 == 0) {
            mobs.removeIf(m -> m == null || !m.isAlive() || m.removed);
            if (mobs.isEmpty()) {
                targetMap.remove(targetUuid);
                mobs = targetMap.computeIfAbsent(targetUuid, k -> new ArrayList<>());
            }
        }

        // ✅ 只改这里：同种类型检查，不再限定僵尸
        if (!mobs.isEmpty()) {
            MobEntity first = mobs.get(0);
            if (first == null) return;
            if (first.getType() != mob.getType()) {
                return;
            }
        }
        mobs.add(mob);

        if (mobs.size() >= 3) {
            mobs.removeIf(m -> m == null || !m.isAlive() || m.removed);
            if (mobs.size() < 3) return;

            MobEntity leader = mobs.get(0);
            if (leader == null) return;
            if (mob == leader) return;

            long currentTick = mob.world.getTime();
            UUID mobUuid = mob.getUuid();

            if (lastUpdateTick.getOrDefault(mobUuid, 0L) + 20 < currentTick) {
                Vec3d offset = new Vec3d(
                        (mob.getRandom().nextDouble() - 0.5) * 2.0,
                        0,
                        (mob.getRandom().nextDouble() - 0.5) * 2.0
                );
                cachedOffset.put(mobUuid, offset);
                lastUpdateTick.put(mobUuid, currentTick);
            }

            Vec3d offset = cachedOffset.get(mobUuid);
            if (offset == null) offset = Vec3d.ZERO;

            if (mob.getNavigation() != null) {
                mob.getNavigation().startMovingTo(
                        leader.getX() + offset.x,
                        leader.getY(),
                        leader.getZ() + offset.z,
                        1.0
                );
            }
        }
    }

    public static void cleanupTargetMaps(MobEntity mob) {
        if (mob == null) return;
        UUID mobUuid = mob.getUuid();
        UUID targetUuid = mobToTarget.remove(mobUuid);

        if (targetUuid != null) {
            List<MobEntity> mobs = targetMap.get(targetUuid);
            if (mobs != null) {
                mobs.remove(mob);
                if (mobs.isEmpty()) {
                    targetMap.remove(targetUuid);
                }
            }
        }

        lastUpdateTick.remove(mobUuid);
        cachedOffset.remove(mobUuid);
    }

    public static void clearAllCache() {
        targetMap.clear();
        mobToTarget.clear();
        lastUpdateTick.clear();
        cachedOffset.clear();
    }
}