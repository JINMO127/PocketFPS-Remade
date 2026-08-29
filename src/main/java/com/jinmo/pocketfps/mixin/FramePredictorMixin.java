package com.jinmo.pocketfps.mixin;

import com.jinmo.pocketfps.gpu.FramePredictor;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class FramePredictorMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        // tick() 现在内部处理所有状态，不需要外部判断
        FramePredictor.CacheState state = FramePredictor.tick();
        // 状态由 blitCachedFrame 和 captureFrame 内部处理
    }

    @Inject(method = "renderWorld", at = @At("HEAD"), cancellable = true)
    private void onRenderWorld(float tickDelta, long limitTime, CallbackInfo ci) {
        // 获取缓存状态
        FramePredictor.CacheState state = FramePredictor.tick();

        // 只有 READY 状态才跳过世界渲染
        if (state == FramePredictor.CacheState.READY) {
            MinecraftClient client = MinecraftClient.getInstance();
            Framebuffer mainBuffer = client.getFramebuffer();
            if (mainBuffer != null && mainBuffer.fbo != -1) {
                if (FramePredictor.blitCachedFrame(mainBuffer)) {
                    ci.cancel();
                    return;
                }
            }
        }

        // NEEDS_REFRESH 或 INVALID 状态：正常渲染
        // 渲染完成后由 onRenderReturn 捕获
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (!FramePredictor.isActive()) return;

        // 只有 NEEDS_REFRESH 状态才捕获
        FramePredictor.CacheState state = FramePredictor.tick();
        if (state == FramePredictor.CacheState.NEEDS_REFRESH) {
            Framebuffer mainBuffer = client.getFramebuffer();
            if (mainBuffer != null && mainBuffer.fbo != -1) {
                FramePredictor.captureFrame(mainBuffer);
            }
        }
    }
}
