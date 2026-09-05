package com.jinmo.pocketfps.mixin;

import com.jinmo.pocketfps.PerformanceTuner;
import com.jinmo.pocketfps.PocketFPSCommand;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class ChunkUpdateThrottlerMixin {
    
    @Unique
    private static int throttleRate = 1;
    
    /**
     * ✅ 修复：使用官方 Yarn 1.16.5 中真实存在的方法 markBlockForRenderUpdate
     * 而不是不存在的 scheduleChunkRender
     * 
     * 该方法在方块更新时被调用，用于标记需要重新渲染的方块
     */
    @Inject(method = "markBlockForRenderUpdate", at = @At("HEAD"), cancellable = true)
    private void onMarkBlockForRenderUpdate(BlockPos pos, CallbackInfo ci) {
        if (!PerformanceTuner.isLowPowerMode() || throttleRate <= 1) return;
        // ✅ 检查区块更新节流是否启用
        if (!PocketFPSCommand.isThrottlerEnabled()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        
        BlockPos playerPos = client.player.getBlockPos();
        int dx = Math.abs(pos.getX() - playerPos.getX());
        int dy = Math.abs(pos.getY() - playerPos.getY());
        int dz = Math.abs(pos.getZ() - playerPos.getZ());
        int distance = Math.max(Math.max(dx, dy), dz);
        
        int rate;
        if (distance < 32) {
            rate = 1;
        } else if (distance < 64) {
            rate = Math.min(throttleRate, 3);
        } else {
            rate = throttleRate;
        }
        
        if (rate <= 1) return;
        
        long gameTime = client.world.getTime();
        long seed = Math.abs(pos.getX() * 7L + pos.getY() * 11L + pos.getZ() * 13L);
        if ((gameTime + seed) % rate != 0) {
            ci.cancel();
        }
    }
    
    public static void setThrottleRate(int rate) {
        throttleRate = Math.max(1, rate);
    }
}
