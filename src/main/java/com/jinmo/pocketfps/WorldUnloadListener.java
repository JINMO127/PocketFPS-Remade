package com.jinmo.pocketfps;

import com.jinmo.pocketfps.lod.EntityLODManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class WorldUnloadListener {
    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            clearAllCaches();
        });
        ClientLifecycleEvents.CLIENT_STOPPED.register(client -> {
            clearAllCaches();
        });
    }
    
    private static void clearAllCaches() {
        EntityLODManager.clearCache();
        PathMerger.clearAllCache();
        FramePredictor.disable();
    }
}