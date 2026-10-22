package me.modmuss50.dg.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobeFluidVertexConsumerTest {
    @Test
    void waterInTheMiniatureStaysInsideOneExteriorBlock() {
        PoseStack pose = new PoseStack();
        pose.translate(-1.0 / 32.0, 0.0, -1.0 / 32.0);
        pose.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
        pose.translate(3, 2, 4);
        Recorder output = new Recorder();
        var fluid = new GlobeFluidVertexConsumer(output, pose.last(), new BlockPos(3, 2, 4));
        fluid.addVertex(3, 2.5f, 4);
        assertEquals(0.15625f, output.x);
        assertEquals(0.15625f, output.y);
        assertEquals(0.21875f, output.z);
    }

    @Test
    void exteriorWaterUsesTheGiantSceneryTransformAndDoesNotAppearInTheRoom() {
        PoseStack pose = new PoseStack();
        var transform = GlobeRenderTransform.forInterior();
        pose.translate(transform.offsetX(), transform.offsetY(), transform.offsetZ());
        pose.scale(transform.scale(), transform.scale(), transform.scale());
        pose.translate(9, 8, 8);
        Recorder output = new Recorder();
        var fluid = new GlobeFluidVertexConsumer(output, pose.last(), new BlockPos(9, 8, 8));
        fluid.addVertex(9, 8.5f, 8);
        assertEquals(8.5f, output.x); // Plus exit X=8 gives room X=16.5, beyond its wall.
        assertEquals(8.0f, output.y);
        assertEquals(-7.5f, output.z);
    }

    private static final class Recorder implements VertexConsumer {
        float x, y, z;
        public VertexConsumer addVertex(float x, float y, float z) { this.x=x; this.y=y; this.z=z; return this; }
        public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        public VertexConsumer setUv(float u, float v) { return this; }
        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) { return this; }
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
