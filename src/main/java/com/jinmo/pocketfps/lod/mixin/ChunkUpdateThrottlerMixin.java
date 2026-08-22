package com.jinmo.pocketfps.lod.mixin;

import com.jinmo.pocketfps.PerformanceTuner;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.render.ChunkRenderDispatcher")
public class ChunkUpdateThrottlerMixin {

    private static int throttleRate = 1; // 每 N tick 允许一次更新
    private static final int MAX_RATE = 20;

    public static void setThrottleRate(int rate) {
        throttleRate = Math.max(1, Math.min(MAX_RATE, rate));
    }

    public static int getThrottleRate() { return throttleRate; }

    // Note: 具体注入目标方法名要和 mappings/yarn 保持一致；这里使用示例方法名 scheduleChunkRender
    @Inject(method = "scheduleChunkRender", at = @At("HEAD"), cancellable = true)
    private void onScheduleChunkRender(int chunkX, int chunkZ, boolean important, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        if (!PerformanceTuner.isLowPowerMode()) return;
        if (throttleRate <= 1) return;

        // 计算玩家到区块中心的距离（粗略转换）
        int px = (int) client.player.getX() >> 4;
        int pz = (int) client.player.getZ() >> 4;
        int dx = Math.abs(px - chunkX);
        int dz = Math.abs(pz - chunkZ);
        int distance = Math.max(dx, dz) * 16; // 转为方块距离

        int rate = throttleRate;
        if (distance <= 16) rate = 1; // 近处不节流
        else if (distance <= 32) rate = Math.min(rate, 3);
        else if (distance <= 64) rate = Math.min(rate, throttleRate);
        else rate = throttleRate;

        if (rate <= 1) return;

        long worldTime = client.world.getTime();
        if (worldTime % rate != 0) {
            ci.cancel();
        }
    }
}
