package me.modmuss50.dg.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class GlobeBaseModel {
    private GlobeBaseModel() {}

    static void render(Block baseBlock, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        render(baseBlock, pose, buffers, light, overlay, 1.0f / 16.0f);
    }

    static void render(Block baseBlock, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                       float thickness) {
        BlockState state = (baseBlock == null ? Blocks.STONE : baseBlock).defaultBlockState();
        pose.pushPose();
        pose.scale(1.0f, thickness, 1.0f);
        // renderSingleBlock asks BlockColors for a tint with null level/position. Some modded
        // handlers (including SecurityCraft 1.10.2.1 reinforced wool) dereference that level.
        // Render the baked model directly with neutral tint because this item renderer has no
        // world context to supply safely.
        GlobeBlockModelRenderer.renderWithoutWorldTint(state, pose, buffers, light, overlay);
        pose.popPose();
    }
}
