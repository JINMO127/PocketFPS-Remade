package com.jinmo.pocketfps.gpu;

import com.jinmo.pocketfps.PerformanceTuner;
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
        
        int w = mainBuffer.textureWidth;
        int h = mainBuffer.textureHeight;
        if (cachedFrameBuffer == null || cachedWidth != w || cachedHeight != h) {
            initCache(w, h);
        }
        
        shouldSkipWorldRender = cachedFrameBuffer != null && cachedFrameBuffer.fbo != -1;
    }
    
    public static boolean blitCachedFrame(Framebuffer target) {
        if (!active || cachedFrameBuffer == null || cachedFrameBuffer.fbo == -1) {
            return false;
        }
        
        if (!isGLContextValid()) {
            PerformanceTuner.LOGGER.warn("FramePredictor: OpenGL 上下文无效，已禁用");
            disable();
            return false;
        }
        
        try {
            // ✅ 修复：使用 GL30 直接调用而不是 GlStateManager._glBindFramebuffer
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, cachedFrameBuffer.fbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.fbo);
            
            GL30.glBlitFramebuffer(
                0, 0, cachedWidth, cachedHeight,
                0, 0, target.textureWidth, target.textureHeight,
                GL11.GL_COLOR_BUFFER_BIT,
                GL11.GL_NEAREST
            );
            
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.fbo);
            return true;
        } catch (Exception e) {
            PerformanceTuner.LOGGER.warn("FramePredictor blit 失败，已禁用", e);
            disable();
            return false;
        }
    }
    
    public static void captureFrame(Framebuffer source) {
        if (!active || cachedFrameBuffer == null || cachedFrameBuffer.fbo == -1) return;
        
        if (!isGLContextValid()) {
            PerformanceTuner.LOGGER.warn("FramePredictor: OpenGL 上下文无效，已禁用");
            disable();
            return;
        }
        
        try {
            // ✅ 修复：使用 GL30 直接调用而不是 GlStateManager._glBindFramebuffer
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.fbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, cachedFrameBuffer.fbo);
            
            GL30.glBlitFramebuffer(
                0, 0, source.textureWidth, source.textureHeight,
                0, 0, cachedWidth, cachedHeight,
                GL11.GL_COLOR_BUFFER_BIT,
                GL11.GL_NEAREST
            );
            
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, source.fbo);
        } catch (Exception e) {
            PerformanceTuner.LOGGER.warn("FramePredictor capture 失败，已禁用", e);
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
        
        try {
            boolean isMac = MinecraftClient.IS_SYSTEM_MAC;
            cachedFrameBuffer = new Framebuffer(width, height, true, isMac);
            cachedFrameBuffer.setClearColor(0, 0, 0, 0);
            cachedWidth = width;
            cachedHeight = height;
        } catch (Exception e) {
            PerformanceTuner.LOGGER.warn("FramePredictor 缓存创建失败", e);
            disable();
        }
    }
    
    private static boolean isGLContextValid() {
        try {
            int error = GL11.glGetError();
            return error == GL11.GL_NO_ERROR || error >= 0;
        } catch (Exception e) {
            return false;
        }
    }
}
