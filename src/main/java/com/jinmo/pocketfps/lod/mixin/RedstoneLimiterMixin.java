package com.jinmo.pocketfps.lod.mixin;

import com.jinmo.pocketfps.PerformanceTuner;
import com.jinmo.pocketfps.PocketFPSCommand;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_1937.class)
public class RedstoneLimiterMixin {
    
    @Unique
    private static int maxDistance = -1;
    
    @Inject(method = "updateNeighborsAlways", at = @At("HEAD"), cancellable = true)
    private void onUpdateNeighbors(class_2338 pos, class_2680 state, CallbackInfo ci) {
        class_1937 world = (class_1937) (Object) this;
        if (!world.field_9236) return;
        if (maxDistance <= 0) return;
        if (!PerformanceTuner.isLowPowerMode()) return;
        if (!PocketFPSCommand.isRedstoneEnabled()) return;
        
        net.minecraft.class_1657 player = world.method_18459(pos.method_10263(), pos.method_10264(), pos.method_10260(), maxDistance, false);
        if (player == null) {
            ci.cancel();
        }
    }
    
    @Unique
    private static void setMaxDistance(int distance) {
        maxDistance = distance;
    }
    
    public static void setRedstoneLimit(int distance) {
        setMaxDistance(distance);
    }
}
