package me.modmuss50.dg.neoforge.client;

import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobeRenderViewTest {
    @Test
    void miniatureUsesDisplayLightEvenWhenLocalCoordinatesAreDark() {
        BlockAndTintGetter darkHost = host((method, args) -> {
            if (method.equals("getBrightness")) return 0;
            throw new AssertionError("Unexpected host lookup: " + method);
        });
        GlobeRenderView view = new GlobeRenderView(Map.of(), darkHost,
                new BlockPos(150, 90, -200), LightTexture.pack(7, 15));
        for (BlockPos local : new BlockPos[]{new BlockPos(1, 1, 1), new BlockPos(15, 15, 15)}) {
            assertEquals(15, view.getBrightness(LightLayer.SKY, local));
            assertEquals(7, view.getBrightness(LightLayer.BLOCK, local));
        }
    }

    @Test
    void miniatureBiomeTintUsesGlobeLocationInsteadOfSnapshotCoordinates() {
        BlockPos displayPos = new BlockPos(150, 90, -200);
        BlockAndTintGetter host = host((method, args) -> {
            if (!method.equals("getBlockTint")) throw new AssertionError("Unexpected lookup: " + method);
            assertEquals(displayPos, args[0]);
            return 0x73AB42;
        });
        GlobeRenderView view = new GlobeRenderView(Map.of(), host, displayPos, LightTexture.FULL_BRIGHT);
        assertEquals(0x73AB42, view.getBlockTint(new BlockPos(1, 1, 1), (biome, x, z) -> 0x73AB42));
    }

    private interface HostLookup { Object invoke(String method, Object[] args); }

    private static BlockAndTintGetter host(HostLookup lookup) {
        return (BlockAndTintGetter) Proxy.newProxyInstance(BlockAndTintGetter.class.getClassLoader(),
                new Class<?>[]{BlockAndTintGetter.class},
                (proxy, method, args) -> lookup.invoke(method.getName(), args));
    }
}
