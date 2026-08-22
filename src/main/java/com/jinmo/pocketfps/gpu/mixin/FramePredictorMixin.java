package com.jinmo.pocketfps.gpu.mixin;

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
        FramePredictor.tick();
    }
    
    @Inject(method = "renderWorld", at = @At("HEAD"), cancellable = true)
    private void onRenderWorld(float tickDelta, long limitTime, CallbackInfo ci) {
        if (!FramePredictor.shouldSkipWorldRender()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer mainBuffer = client.getFramebuffer();
        if (mainBuffer == null || mainBuffer.fbo == -1) return;
        
        if (FramePredictor.blitCachedFrame(mainBuffer)) {
            ci.cancel();
        }
    }
    
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (!FramePredictor.isActive()) return;
        
        Framebuffer mainBuffer = client.getFramebuffer();
        if (mainBuffer != null && mainBuffer.fbo != -1) {
            FramePredictor.captureFrame(mainBuffer);
        }
    }
}
