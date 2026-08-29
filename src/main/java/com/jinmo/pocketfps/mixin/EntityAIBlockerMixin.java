package com.jinmo.pocketfps.mixin;

import com.jinmo.pocketfps.PerformanceTuner;
import com.jinmo.pocketfps.PocketFPSCommand;
import com.jinmo.pocketfps.lod.EntityLODManager;
import com.jinmo.pocketfps.lod.PathMerger;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public class EntityAIBlockerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;

        // ✅ 每 5 tick 调用一次路径合并
        if (mob.age % 5 == 0) {
            PathMerger.mergePathIfNeeded(mob);
        }

        // 检查实体冻结是否启用
        if (!PocketFPSCommand.isEntityFreezeEnabled()) {
            if (EntityLODManager.isFrozen(mob)) {
                EntityLODManager.unfreeze(mob);
            }
            return;
        }

        if (!PerformanceTuner.isLowPowerMode()) {
            if (EntityLODManager.isFrozen(mob)) {
                EntityLODManager.unfreeze(mob);
            }
            return;
        }

        if (!mob.world.isClient) return;
        if (!mob.isAlive() || mob.removed) {
            if (EntityLODManager.isFrozen(mob)) {
                EntityLODManager.unfreeze(mob);
            }
            return;
        }

        if (EntityLODManager.shouldFreeze(mob)) {
            EntityLODManager.freeze(mob);
            ci.cancel();
        } else {
            if (EntityLODManager.isFrozen(mob)) {
                EntityLODManager.unfreeze(mob);
            }
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void onTickMovement(CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;
        if (EntityLODManager.isFrozen(mob)) {
            ci.cancel();
        }
    }
}