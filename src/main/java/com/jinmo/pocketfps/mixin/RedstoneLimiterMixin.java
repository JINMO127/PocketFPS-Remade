package com.jinmo.pocketfps.mixin;

import com.jinmo.pocketfps.PerformanceTuner;
import com.jinmo.pocketfps.PocketFPSCommand;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
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
    private static int maxDistance = -1;

    @Inject(method = "updateNeighborsAlways", at = @At("HEAD"), cancellable = true)
    private void onUpdateNeighbors(BlockPos pos, Block sourceBlock, CallbackInfo ci) {
        // 功能暂时禁用，避免 Mixin 报错
        // TODO: 后续版本恢复红石限制功能
    }

    @Unique
    private static void setMaxDistance(int distance) {
        maxDistance = distance;
    }

    @Unique
    private static void setRedstoneLimit(int distance) {
        // 功能暂时禁用，避免 Mixin 报错
        // TODO: 后续版本恢复红石限制功能
    }
}
