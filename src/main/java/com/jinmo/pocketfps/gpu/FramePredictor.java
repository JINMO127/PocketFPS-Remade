package com.jinmo.pocketfps.gpu;

import com.jinmo.pocketfps.PerformanceTuner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * 帧预测缓存
 */
public class FramePredictor {

    // ==================== 公共配置 ====================
    public static final class Config {
        public static int maxFboSize = 8192;
        public static long cacheTimeoutMs = 100;        // 固定 100ms，1.16.5 无 getCurrentFps
        public static long glCheckIntervalMs = 500;
        public static long reinitCooldownMs = 200;
        public static boolean debug = false;
    }

    // ==================== 状态枚举 ====================
    public enum CacheState {
        INVALID,
        READY,
        NEEDS_REFRESH
    }

    // ==================== 常量 ====================
    private static final Object LOCK = new Object();

    // ==================== 状态 ====================
    private static boolean active = false;
    private static boolean initialized = false;
    private static Framebuffer cachedFrameBuffer = null;
    private static int cachedWidth = 0;
    private static int cachedHeight = 0;
    private static int maxTextureSize = 0;

    private static long lastCaptureTime = 0;
    private static long lastReinitTime = 0;

    private static boolean lastGLContextValid = false;
    private static long lastGLContextCheck = 0;

    // 性能统计
    private static int cacheHits = 0;
    private static int cacheMisses = 0;

    // ==================== 公共 API ====================

    public static void enable(boolean enable) {
        synchronized (LOCK) {
            if (enable && !active) {
                active = true;
                initialized = false;
                lastCaptureTime = 0;
                cacheHits = 0;
                cacheMisses = 0;
                PerformanceTuner.LOGGER.debug("FramePredictor 已启用");
            } else if (!enable && active) {
                disableInternal();
            }
        }
    }

    public static void disable() {
        synchronized (LOCK) {
            disableInternal();
        }
    }

    public static boolean isActive() {
        synchronized (LOCK) {
            return active;
        }
    }

    public static float getHitRate() {
        int total = cacheHits + cacheMisses;
        return total == 0 ? 0 : (float) cacheHits / total;
    }

    public static CacheState tick() {
        synchronized (LOCK) {
            if (!active) return CacheState.INVALID;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || !client.isOnThread()) return CacheState.INVALID;

            if (client.world == null || client.player == null) {
                if (initialized) disableInternal();
                return CacheState.INVALID;
            }

            if (!isGLContextValid()) {
                PerformanceTuner.LOGGER.warn("FramePredictor: OpenGL 上下文无效，已禁用");
                disableInternal();
                return CacheState.INVALID;
            }

            Framebuffer mainBuffer = client.getFramebuffer();
            if (mainBuffer == null || mainBuffer.fbo == -1) return CacheState.INVALID;

            int w = mainBuffer.textureWidth;
            int h = mainBuffer.textureHeight;
            if (w <= 0 || h <= 0) return CacheState.INVALID;

            boolean needsReinit = !initialized || cachedFrameBuffer == null ||
                    cachedFrameBuffer.fbo == -1 ||
                    cachedWidth != w || cachedHeight != h;

            if (needsReinit) {
                long now = System.currentTimeMillis();
                if (now - lastReinitTime < Config.reinitCooldownMs) {
                    return CacheState.INVALID;
                }
                lastReinitTime = now;

                if (!initCache(w, h)) {
                    disableInternal();
                    return CacheState.INVALID;
                }
                PerformanceTuner.LOGGER.debug("FramePredictor 缓存已初始化 ({}x{})", w, h);
                return CacheState.NEEDS_REFRESH;
            }

            long now = System.currentTimeMillis();
            if (lastCaptureTime == 0 || (now - lastCaptureTime) > Config.cacheTimeoutMs) {
                cacheMisses++;
                return CacheState.NEEDS_REFRESH;
            }

            cacheHits++;
            return CacheState.READY;
        }
    }

    public static boolean blitCachedFrame(Framebuffer target) {
        synchronized (LOCK) {
            if (!active || !initialized || cachedFrameBuffer == null || cachedFrameBuffer.fbo == -1) {
                return false;
            }

            if (target == null || target.fbo == -1) return false;
            if (!isGLContextValid()) {
                disableInternal();
                return false;
            }

            int previousDrawFbo = 0;
            int previousReadFbo = 0;

            try {
                previousDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
                previousReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);

                if (target.textureWidth <= 0 || target.textureHeight <= 0) return false;

                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, cachedFrameBuffer.fbo);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.fbo);

                if (Config.debug) {
                    int status = GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER);
                    if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                        PerformanceTuner.LOGGER.warn("FramePredictor: 源 FBO 不完整, status={}", status);
                        return false;
                    }
                    status = GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER);
                    if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                        PerformanceTuner.LOGGER.warn("FramePredictor: 目标 FBO 不完整, status={}", status);
                        return false;
                    }
                }

                GL30.glBlitFramebuffer(
                        0, 0, cachedWidth, cachedHeight,
                        0, 0, target.textureWidth, target.textureHeight,
                        GL11.GL_COLOR_BUFFER_BIT,
                        GL11.GL_NEAREST
                );

                GL11.glFlush();
                return true;

            } catch (Exception e) {
                PerformanceTuner.LOGGER.warn("FramePredictor blit 失败", e);
                return false;
            } finally {
                try {
                    if (previousReadFbo != 0) {
                        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFbo);
                    }
                    if (previousDrawFbo != 0) {
                        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFbo);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public static void captureFrame(Framebuffer source) {
        synchronized (LOCK) {
            if (!active || !initialized || cachedFrameBuffer == null || cachedFrameBuffer.fbo == -1) {
                return;
            }

            if (source == null || source.fbo == -1) return;
            if (!isGLContextValid()) {
                disableInternal();
                return;
            }

            int previousDrawFbo = 0;
            int previousReadFbo = 0;

            try {
                previousDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
                previousReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);

                if (source.textureWidth <= 0 || source.textureHeight <= 0) return;

                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.fbo);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, cachedFrameBuffer.fbo);

                GL30.glBlitFramebuffer(
                        0, 0, source.textureWidth, source.textureHeight,
                        0, 0, cachedWidth, cachedHeight,
                        GL11.GL_COLOR_BUFFER_BIT,
                        GL11.GL_NEAREST
                );

                GL11.glFlush();
                lastCaptureTime = System.currentTimeMillis();

            } catch (Exception e) {
                PerformanceTuner.LOGGER.warn("FramePredictor capture 失败", e);
                initialized = false;
                lastCaptureTime = 0;
                if (cachedFrameBuffer != null) {
                    try { cachedFrameBuffer.delete(); } catch (Exception ignored) {}
                    cachedFrameBuffer = null;
                }
                cachedWidth = 0;
                cachedHeight = 0;
            } finally {
                try {
                    if (previousReadFbo != 0) {
                        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFbo);
                    }
                    if (previousDrawFbo != 0) {
                        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFbo);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public static void forceCleanup() {
        synchronized (LOCK) {
            if (cachedFrameBuffer != null) {
                try {
                    if (cachedFrameBuffer.fbo != -1) {
                        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                    }
                    cachedFrameBuffer.delete();
                } catch (Exception ignored) {}
                cachedFrameBuffer = null;
            }
            active = false;
            initialized = false;
            cachedWidth = 0;
            cachedHeight = 0;
            lastCaptureTime = 0;
            lastReinitTime = 0;
            lastGLContextValid = false;
            lastGLContextCheck = 0;
            cacheHits = 0;
            cacheMisses = 0;
        }
    }

    // ==================== 内部方法 ====================

    private static void disableInternal() {
        active = false;
        initialized = false;
        lastCaptureTime = 0;

        if (cachedFrameBuffer != null) {
            try {
                if (cachedFrameBuffer.fbo != -1 && isGLContextValid()) {
                    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                }
                cachedFrameBuffer.delete();
            } catch (Exception ignored) {}
            cachedFrameBuffer = null;
        }

        cachedWidth = 0;
        cachedHeight = 0;
        lastGLContextValid = false;
        lastGLContextCheck = 0;
        PerformanceTuner.LOGGER.debug("FramePredictor 已禁用");
    }

    private static int getMaxTextureSize() {
        if (maxTextureSize == 0) {
            try {
                if (isGLContextValid()) {
                    maxTextureSize = GL11.glGetInteger(GL30.GL_MAX_TEXTURE_SIZE);
                } else {
                    maxTextureSize = Config.maxFboSize;
                }
            } catch (Exception | LinkageError e) {
                maxTextureSize = Config.maxFboSize;
            }
            if (maxTextureSize <= 0) maxTextureSize = Config.maxFboSize;
            PerformanceTuner.LOGGER.debug("FramePredictor: 最大纹理尺寸 {}", maxTextureSize);
        }
        return maxTextureSize;
    }

    private static boolean isGLContextValid() {
        long now = System.currentTimeMillis();
        if (now - lastGLContextCheck < Config.glCheckIntervalMs) {
            return lastGLContextValid;
        }
        lastGLContextCheck = now;

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || !client.isOnThread()) {
                lastGLContextValid = false;
                return false;
            }

            GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            String version = GL11.glGetString(GL11.GL_VERSION);
            lastGLContextValid = version != null && !version.isEmpty();
            return lastGLContextValid;
        } catch (Exception e) {
            lastGLContextValid = false;
            return false;
        }
    }

    private static boolean initCache(int width, int height) {
        if (cachedFrameBuffer != null) {
            try {
                if (cachedFrameBuffer.fbo != -1 && isGLContextValid()) {
                    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                }
                cachedFrameBuffer.delete();
            } catch (Exception ignored) {}
            cachedFrameBuffer = null;
        }

        cachedWidth = 0;
        cachedHeight = 0;
        initialized = false;

        try {
            int maxSize = getMaxTextureSize();
            if (width <= 0 || height <= 0 || width > maxSize || height > maxSize) {
                PerformanceTuner.LOGGER.warn("FramePredictor: 尺寸超限 {}x{} (最大 {})", width, height, maxSize);
                return false;
            }

            boolean isMac = MinecraftClient.IS_SYSTEM_MAC;
            cachedFrameBuffer = new Framebuffer(width, height, false, isMac);
            cachedFrameBuffer.setClearColor(0, 0, 0, 0);
            cachedWidth = width;
            cachedHeight = height;
            initialized = true;
            return true;
        } catch (Exception e) {
            PerformanceTuner.LOGGER.warn("FramePredictor 缓存创建失败", e);
            if (cachedFrameBuffer != null) {
                try { cachedFrameBuffer.delete(); } catch (Exception ignored) {}
                cachedFrameBuffer = null;
            }
            cachedWidth = 0;
            cachedHeight = 0;
            initialized = false;
            return false;
        }
    }
}
