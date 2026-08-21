package com.jinmo.pocketfps.gpu;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class FramePredictor {
    private static boolean active = false;
    private static Framebuffer cachedFrameBuffer = null;
    private static int cachedWidth = 0;
    private static int cachedHeight = 0;
    private static boolean shouldSkipWorldRender = false;
    
    public static void enable(boolean enable) {
        if (enable && !active) {
            active = true;
            shouldSkipWorldRender = false;
            initCache();
        } else if (!enable && active) {
            disable();
        }
    }
    
    public static void disable() {
        active = false;
        shouldSkipWorldRender = false;
        if (cachedFrameBuffer != null) {
            try {
                cachedFrameBuffer.delete();
            } catch (Exception ignored) {}
            cachedFrameBuffer = null;
        }
    }
    
    public static boolean isActive() {
        return active;
    }
    
    public static boolean shouldSkipWorldRender() {
        return shouldSkipWorldRender;
    }
    
    public static void tick() {
        if (!active) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            disable();
            return;
        }
        
        Framebuffer mainBuffer = client.getFramebuffer();
        if (mainBuffer == null || mainBuffer.fbo == -1) {
            disable();
            return;
        }
        
        // 检查尺寸变化
        int w = mainBuffer.textureWidth;
        int h = mainBuffer.textureHeight;
        if (cachedFrameBuffer == null || cachedWidth != w || cachedHeight != h) {
            initCache(w, h);
        }
        
        // 决定是否跳过世界渲染（只在有缓存时跳）
        shouldSkipWorldRender = cachedFrameBuffer != null && cachedFrameBuffer.fbo != -1;
    }
    
    public static boolean blitCachedFrame(Framebuffer target) {
        if (!active || cachedFrameBuffer == null || cachedFrameBuffer.fbo == -1) {
            return false;
        }
        
        try {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, cachedFrameBuffer.fbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.fbo);
            
            GL30.glBlitFramebuffer(
                0, 0, cachedWidth, cachedHeight,
                0, 0, target.textureWidth, target.textureHeight,
                GL11.GL_COLOR_BUFFER_BIT,
                GL11.GL_NEAREST
            );
            
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.fbo);
            return true;
        } catch (Exception e) {
            disable();
            return false;
        }
    }
    
    public static void captureFrame(Framebuffer source) {
        if (!active || cachedFrameBuffer == null || cachedFrameBuffer.fbo == -1) return;
        
        try {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.fbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, cachedFrameBuffer.fbo);
            
            GL30.glBlitFramebuffer(
                0, 0, source.textureWidth, source.textureHeight,
                0, 0, cachedWidth, cachedHeight,
                GL11.GL_COLOR_BUFFER_BIT,
                GL11.GL_NEAREST
            );
            
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, source.fbo);
        } catch (Exception e) {
            disable();
        }
    }
    
    private static void initCache() {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer mainBuffer = client.getFramebuffer();
        if (mainBuffer != null) {
            initCache(mainBuffer.textureWidth, mainBuffer.textureHeight);
        }
    }
    
    private static void initCache(int width, int height) {
        if (cachedFrameBuffer != null) {
            try {
                cachedFrameBuffer.delete();
            } catch (Exception ignored) {}
            cachedFrameBuffer = null;
        }
        cachedFrameBuffer = new Framebuffer(width, height, true, MinecraftClient.IS_SYSTEM_MAC);
        cachedFrameBuffer.setClearColor(0, 0, 0, 0);
        cachedWidth = width;
        cachedHeight = height;
    }
}