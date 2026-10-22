package me.modmuss50.dg.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;

/** LiquidBlockRenderer emits section-local vertices without applying a PoseStack. */
final class GlobeFluidVertexConsumer implements VertexConsumer {
    private final VertexConsumer target;
    private final PoseStack.Pose pose;
    private final int blockX, blockY, blockZ;

    GlobeFluidVertexConsumer(VertexConsumer target, PoseStack.Pose pose, BlockPos block) {
        this.target = target;
        this.pose = pose;
        blockX = block.getX() & 15;
        blockY = block.getY() & 15;
        blockZ = block.getZ() & 15;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        // The caller's pose already includes this block's translation. Strip the
        // tessellator's section offset before scaling/placing it with that pose.
        target.addVertex(pose, x - blockX, y - blockY, z - blockZ);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) { target.setColor(r, g, b, a); return this; }
    @Override
    public VertexConsumer setUv(float u, float v) { target.setUv(u, v); return this; }
    @Override
    public VertexConsumer setUv1(int u, int v) { target.setUv1(u, v); return this; }
    @Override
    public VertexConsumer setUv2(int u, int v) { target.setUv2(u, v); return this; }
    @Override
    public VertexConsumer setNormal(float x, float y, float z) { target.setNormal(pose, x, y, z); return this; }
}
