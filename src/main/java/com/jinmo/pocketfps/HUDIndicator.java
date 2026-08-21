package com.jinmo.pocketfps;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

public class HUDIndicator {
    private static boolean registered = false;
    
    public static void register() {
        if (registered) return;
        registered = true;
        
        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            if (!ConfigManager.get().showHUDIndicator) return;
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;
            if (!PerformanceTuner.isLowPowerMode()) return;
            
            TextRenderer textRenderer = client.textRenderer;
            if (textRenderer == null) return;
            
            float fps = PerformanceTuner.getSmoothedFps();
            String level = PerformanceScheduler.getCurrentLevel().name();
            String text = String.format("⚡ 低功耗 [%s] %.1f FPS", level, fps);
            
            int color;
            switch (PerformanceScheduler.getCurrentLevel()) {
                case LIGHT: color = 0x55FF55; break;
                case MEDIUM: color = 0xFFFF55; break;
                case HEAVY: color = 0xFF5555; break;
                default: color = 0xAAAAAA;
            }
            
            matrixStack.push();
            matrixStack.translate(10, 10, 0);
            textRenderer.drawWithShadow(matrixStack, text, 0, 0, color);
            matrixStack.pop();
        });
    }
}