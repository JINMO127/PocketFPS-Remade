package com.jinmo.pocketfps.lod.mixin;

import com.jinmo.pocketfps.PerformanceTuner;
import com.jinmo.pocketfps.PocketFPSCommand;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_1937.class)
public class RedstoneLimiterMixin {

    @Unique
    private static int maxDistance = -1;

    // ✅ 注入还在，但方法体是空的（功能被禁用）
    @Inject(method = "updateNeighborsAlways", at = @At("HEAD"), cancellable = true)
    private void onUpdateNeighbors(class_2338 pos, class_2248 sourceBlock, CallbackInfo ci) {
        // 什么都不做，让红石更新正常进行
        // 相当于这个功能被“关闭”了
    }

    @Unique
    private static void setMaxDistance(int distance) {
        // 什么都不做，只是接收调用
    }

    // ✅ 对外暴露的 API，供 PocketFPSCommand 和 PerformanceScheduler 调用
    public static class Api {
        public static void setRedstoneLimit(int distance) {
            // 什么都不做，避免崩溃
        }
    }
}
