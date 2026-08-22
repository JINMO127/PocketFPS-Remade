package com.jinmo.pocketfps;

import com.jinmo.pocketfps.gpu.FramePredictor;
import com.jinmo.pocketfps.lod.EntityLODManager;
import com.jinmo.pocketfps.lod.mixin.ChunkUpdateThrottlerMixin;
import com.jinmo.pocketfps.lod.mixin.RedstoneLimiterMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.LiteralText;

import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;

public class PocketFPSCommand {
    
    private static boolean predictorEnabled = true;
    private static boolean throttlerEnabled = true;
    private static boolean redstoneEnabled = true;
    private static boolean entityFreezeEnabled = true;
    
    public static void register() {
        CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getDispatcher();
        
        dispatcher.register(literal("pocketfps")
            .executes(context -> {
                sendStatus(context.getSource());
                return 1;
            })
            
            .then(literal("predictor")
                .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        setPredictorEnabled(enabled);
                        context.getSource().sendFeedback(new LiteralText(
                            "§6帧预测: " + (enabled ? "§a已开启" : "§c已关闭")
                        ));
                        return 1;
                    })
                )
                .executes(context -> {
                    context.getSource().sendFeedback(new LiteralText(
                        "§6帧预测: " + (predictorEnabled ? "§a已开启" : "§c已关闭") + "\n" +
                        "§7用法: /pocketfps predictor <true/false>"
                    ));
                    return 1;
                })
            )
            
            .then(literal("throttler")
                .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        setThrottlerEnabled(enabled);
                        context.getSource().sendFeedback(new LiteralText(
                            "§6区块更新节流: " + (enabled ? "§a已开启" : "§c已关闭")
                        ));
                        return 1;
                    })
                )
                .executes(context -> {
                    context.getSource().sendFeedback(new LiteralText(
                        "§6区块更新节流: " + (throttlerEnabled ? "§a已开启" : "§c已关闭") + "\n" +
                        "§7用法: /pocketfps throttler <true/false>"
                    ));
                    return 1;
                })
            )
            
            .then(literal("redstone")
                .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        setRedstoneEnabled(enabled);
                        context.getSource().sendFeedback(new LiteralText(
                            "§6红石限制: " + (enabled ? "§a已开启" : "§c已关闭")
                        ));
                        return 1;
                    })
                )
                .executes(context -> {
                    context.getSource().sendFeedback(new LiteralText(
                        "§6红石限制: " + (redstoneEnabled ? "§a已开启" : "§c已关闭") + "\n" +
                        "§7用法: /pocketfps redstone <true/false>"
                    ));
                    return 1;
                })
            )
            
            .then(literal("entity")
                .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        setEntityFreezeEnabled(enabled);
                        context.getSource().sendFeedback(new LiteralText(
                            "§6实体冻结: " + (enabled ? "§a已开启" : "§c已关闭")
                        ));
                        return 1;
                    })
                )
                .executes(context -> {
                    context.getSource().sendFeedback(new LiteralText(
                        "§6实体冻结: " + (entityFreezeEnabled ? "§a已开启" : "§c已关闭") + "\n" +
                        "§7用法: /pocketfps entity <true/false>"
                    ));
                    return 1;
                })
            )
            
            .then(literal("reset")
                .executes(context -> {
                    resetAll();
                    context.getSource().sendFeedback(new LiteralText(
                        "§a✅ 所有功能已重置为默认状态\n" +
                        "§7帧预测: 开启 | 区块节流: 开启 | 红石限制: 开启 | 实体冻结: 开启"
                    ));
                    return 1;
                })
            )
            
            .then(literal("status")
                .executes(context -> {
                    sendStatus(context.getSource());
                    return 1;
                })
            )
        );
    }
    
    private static void sendStatus(FabricClientCommandSource source) {
        source.sendFeedback(new LiteralText(
            "§6=== PocketFPS 功能状态 ===\n" +
            "§7帧预测 (predictor): " + (predictorEnabled ? "§a✅ 开启" : "§c❌ 关闭") + "\n" +
            "§7区块更新节流 (throttler): " + (throttlerEnabled ? "§a✅ 开启" : "§c❌ 关闭") + "\n" +
            "§7红石限制 (redstone): " + (redstoneEnabled ? "§a✅ 开启" : "§c❌ 关闭") + "\n" +
            "§7实体冻结 (entity): " + (entityFreezeEnabled ? "§a✅ 开启" : "§c❌ 关闭") + "\n" +
            "§7─────────────────────\n" +
            "§7低功耗模式: " + (PerformanceTuner.isLowPowerMode() ? "§a开启" : "§c关闭") + "\n" +
            "§7当前 FPS: §e" + String.format("%.1f", PerformanceTuner.getSmoothedFps()) + "\n" +
            "§7优化等级: §b" + PerformanceScheduler.getCurrentLevel().name() + "\n" +
            "§7冻结实体数: §b" + EntityLODManager.getFrozenCount()
        ));
    }
    
    public static void setPredictorEnabled(boolean enabled) {
        setPredictorEnabled(enabled, true);
    }
    
    public static void setPredictorEnabled(boolean enabled, boolean save) {
        predictorEnabled = enabled;
        if (!enabled) {
            FramePredictor.disable();
        }
        if (save) {
            ConfigManager.get().predictorEnabled = enabled;
            ConfigManager.saveConfig();
        }
    }
    
    public static void setThrottlerEnabled(boolean enabled) {
        setThrottlerEnabled(enabled, true);
    }
    
    public static void setThrottlerEnabled(boolean enabled, boolean save) {
        throttlerEnabled = enabled;
        if (!enabled) {
            ChunkUpdateThrottlerMixin.setThrottleRate(1);
        }
        if (save) {
            ConfigManager.get().throttlerEnabled = enabled;
            ConfigManager.saveConfig();
        }
    }
    
    public static void setRedstoneEnabled(boolean enabled) {
        setRedstoneEnabled(enabled, true);
    }
    
    public static void setRedstoneEnabled(boolean enabled, boolean save) {
        redstoneEnabled = enabled;
        if (!enabled) {
            RedstoneLimiterMixin.setMaxDistance(-1);
        }
        if (save) {
            ConfigManager.get().redstoneEnabled = enabled;
            ConfigManager.saveConfig();
        }
    }
    
    public static void setEntityFreezeEnabled(boolean enabled) {
        setEntityFreezeEnabled(enabled, true);
    }
    
    public static void setEntityFreezeEnabled(boolean enabled, boolean save) {
        entityFreezeEnabled = enabled;
        if (!enabled) {
            EntityLODManager.clearCache();
        }
        if (save) {
            ConfigManager.get().entityFreezeEnabled = enabled;
            ConfigManager.saveConfig();
        }
    }
    
    public static void resetAll() {
        setPredictorEnabled(true);
        setThrottlerEnabled(true);
        setRedstoneEnabled(true);
        setEntityFreezeEnabled(true);
        PerformanceTuner.setLowPowerMode(false);
        PerformanceScheduler.restoreAll();
    }
    
    public static boolean isPredictorEnabled() { return predictorEnabled; }
    public static boolean isThrottlerEnabled() { return throttlerEnabled; }
    public static boolean isRedstoneEnabled() { return redstoneEnabled; }
    public static boolean isEntityFreezeEnabled() { return entityFreezeEnabled; }
}