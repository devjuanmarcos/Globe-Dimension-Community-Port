package me.modmuss50.dg.neoforge.client;

import com.mojang.logging.LogUtils;
import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/** Temporary runtime telemetry for diagnosing a camera-dependent globe render. */
@EventBusSubscriber(modid = GlobeDimensionMod.MOD_ID, value = Dist.CLIENT)
final class GlobeRenderDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation GLOBE_DIMENSION = ResourceLocation.fromNamespaceAndPath(GlobeDimensionMod.MOD_ID, "globe");
    private static long lastRenderNanos;
    private static int lastBlockCount;
    private static int lastGlobeId = -1;
    private static long lastReportedTick = Long.MIN_VALUE;

    private GlobeRenderDiagnostics() {}

    static void markRendered(int globeId, int blockCount) {
        lastRenderNanos = System.nanoTime();
        lastGlobeId = globeId;
        lastBlockCount = blockCount;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !minecraft.level.dimension().location().equals(GLOBE_DIMENSION)) return;
        long tick = minecraft.level.getGameTime();
        if (tick == lastReportedTick || tick % 20L != 0L) return;
        lastReportedTick = tick;
        double sinceRender = lastRenderNanos == 0L ? -1.0 : (System.nanoTime() - lastRenderNanos) / 1_000_000.0;
        var camera = minecraft.gameRenderer.getMainCamera();
        LOGGER.info("[Globe diagnostics] id={}, snapshotBlocks={}, msSinceRender={}, camera=({}, {}, {}), yaw={}, pitch={}",
                lastGlobeId, lastBlockCount, String.format("%.2f", sinceRender),
                String.format("%.2f", camera.getPosition().x), String.format("%.2f", camera.getPosition().y),
                String.format("%.2f", camera.getPosition().z), camera.getYRot(), camera.getXRot());
    }
}
