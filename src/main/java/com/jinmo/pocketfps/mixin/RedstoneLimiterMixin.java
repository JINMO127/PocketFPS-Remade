package com.jinmo.pocketfps.mixin;

import com.jinmo.pocketfps.PerformanceTuner;
import com.jinmo.pocketfps.PocketFPSCommand;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
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

    /**
     * 注入 World.updateNeighborsAlways 方法
     * 该方法在方块更新时被调用，用于通知相邻方块
     * 通过限制距离远的方块更新来降低红石性能消耗
     * 
     * ⚠️ 当前阶段：暂时禁用实现，等待后续版本完整功能
     */
    @Inject(method = "updateNeighborsAlways", at = @At("HEAD"), cancellable = true)
    private void onUpdateNeighborsAlways(BlockPos pos, Block sourceBlock, CallbackInfo ci) {
        // 功能暂时禁用，避免 Mixin 报错
        // TODO: 后续版本恢复红石限制功能
    }

    @Unique
    public static void setMaxDistance(int distance) {
        maxDistance = distance;
    }

    @Unique
    public static void setRedstoneLimit(int distance) {
        maxDistance = distance;
    }
}
