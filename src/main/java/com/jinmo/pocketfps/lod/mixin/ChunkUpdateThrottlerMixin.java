package com.jinmo.pocketfps.lod.mixin;

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
    
    @Inject(method = "scheduleChunkRender", at = @At("HEAD"), cancellable = true)
    private void onScheduleChunkRender(int chunkX, int chunkZ, boolean important, CallbackInfo ci) {
        if (!PerformanceTuner.isLowPowerMode() || throttleRate <= 1) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        
        BlockPos playerPos = client.player.getBlockPos();
        int dx = Math.abs((chunkX << 4) - playerPos.getX());
        int dz = Math.abs((chunkZ << 4) - playerPos.getZ());
        int distance = Math.max(dx, dz);
        
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
        long hash = Math.abs(chunkX * 7L + chunkZ * 13L);
        if ((gameTime + hash) % rate != 0) {
            ci.cancel();
        }
    }
    
    public static void setThrottleRate(int rate) {
        throttleRate = Math.max(1, rate);
    }
}