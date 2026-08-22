package com.jinmo.pocketfps.lod.mixin;

import com.jinmo.pocketfps.PerformanceTuner;
import com.jinmo.pocketfps.PocketFPSCommand;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public class RedstoneLimiterMixin {
    
    @Unique
    private static int maxDistance = -1; // -1 = 无限制
    
    @Inject(method = "updateNeighborsAlways", at = @At("HEAD"), cancellable = true)
    private void onUpdateNeighbors(BlockPos pos, BlockState state, CallbackInfo ci) {
        World world = (World) (Object) this;
        if (!world.isClient) return;
        if (maxDistance <= 0) return;
        if (!PerformanceTuner.isLowPowerMode()) return;
        // ✅ 新增：检查红石限制是否启用
        if (!PocketFPSCommand.isRedstoneEnabled()) return;
        
        net.minecraft.entity.player.PlayerEntity player = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), maxDistance, false);
        if (player == null) {
            ci.cancel();
        }
    }
    
    public static void setMaxDistance(int distance) {
        maxDistance = distance;
    }
}