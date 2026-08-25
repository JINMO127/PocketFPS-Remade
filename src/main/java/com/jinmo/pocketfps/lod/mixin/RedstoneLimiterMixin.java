package com.jinmo.pocketfps.lod.mixin;

import com.jinmo.pocketfps.PerformanceTuner;
import com.jinmo.pocketfps.PocketFPSCommand;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)  // Mojang 映射
public class RedstoneLimiterMixin {

    @Unique
    private static int maxDistance = -1;

    @Inject(method = "updateNeighborsAlways", at = @At("HEAD"), cancellable = true)
    private void onUpdateNeighbors(BlockPos pos, Block sourceBlock, CallbackInfo ci) {
        // 方法体为空，功能关闭
    }

    public static class Api {
        public static void setRedstoneLimit(int distance) {
            // 什么都不做
        }
    }
}
