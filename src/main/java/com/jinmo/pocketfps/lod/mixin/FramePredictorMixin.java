package com.jinmo.pocketfps.lod.mixin;

import com.jinmo.pocketfps.gpu.FramePredictor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.render.GameRenderer")
public class FramePredictorMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(CallbackInfo ci) {
        if (FramePredictor.isEnabled()) {
            // 预留：插入预测逻辑或帧预热调用
        }
    }
}
