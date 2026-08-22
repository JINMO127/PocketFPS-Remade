package com.jinmo.pocketfps;

import com.jinmo.pocketfps.gpu.FramePredictor;
import com.jinmo.pocketfps.lod.EntityLODManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class WorldUnloadListener {
    public static void register() {
        ClientLifecycleEvents.CLIENT_STOPPED.register(client -> {
            clearAllCaches();
        });
    }

    private static void clearAllCaches() {
        EntityLODManager.clearCache();
        PathMerger.clearAllCache();
        FramePredictor.disable();
        try {
            PerformanceTuner.LOGGER.info("World unloaded — caches cleared");
        } catch (Throwable t) {
        }
    }
}
