package me.modmuss50.dg.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Rendering helpers for block models drawn outside a real level context. */
final class GlobeBlockModelRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> WARNED_TINT_BLOCKS = ConcurrentHashMap.newKeySet();

    private GlobeBlockModelRenderer() {}

    static void render(BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockRenderDispatcher renderer = Minecraft.getInstance().getBlockRenderer();
        try {
            renderer.renderSingleBlock(state, pose, buffers, light, overlay);
        } catch (NullPointerException exception) {
            if (!isWorldlessTintFailure(exception)) throw exception;
            renderWithoutWorldTint(state, pose, buffers, light, overlay, renderer);
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if (WARNED_TINT_BLOCKS.add(blockId)) {
                LOGGER.warn("Block {} uses a color handler that requires a level; rendering the globe preview without its world tint.", blockId);
            }
        }
    }

    static void renderInSnapshot(BlockState state, BlockPos pos, BlockAndTintGetter snapshot,
                                 PoseStack pose, MultiBufferSource buffers) {
        BlockRenderDispatcher renderer = Minecraft.getInstance().getBlockRenderer();
        var renderType = ItemBlockRenderTypes.getChunkRenderType(state);
        renderer.renderBatched(state, pos, snapshot, pose, buffers.getBuffer(renderType), true,
                RandomSource.create(pos.asLong()), ModelData.EMPTY, renderType);
        if (!state.getFluidState().isEmpty()) {
            var fluidRenderType = ItemBlockRenderTypes.getRenderLayer(state.getFluidState());
            renderer.renderLiquid(pos, snapshot,
                    new GlobeFluidVertexConsumer(buffers.getBuffer(fluidRenderType), pose.last(), pos),
                    state, state.getFluidState());
        }
    }

    static void renderWithoutWorldTint(BlockState state, PoseStack pose, MultiBufferSource buffers,
                                       int light, int overlay) {
        renderWithoutWorldTint(state, pose, buffers, light, overlay,
                Minecraft.getInstance().getBlockRenderer());
    }

    private static void renderWithoutWorldTint(BlockState state, PoseStack pose, MultiBufferSource buffers,
                                               int light, int overlay, BlockRenderDispatcher renderer) {
        var renderType = ItemBlockRenderTypes.getRenderType(state, false);
        renderer.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(renderType), state,
                renderer.getBlockModel(state), 1.0f, 1.0f, 1.0f, light, overlay,
                ModelData.EMPTY, renderType);
    }

    private static boolean isWorldlessTintFailure(NullPointerException exception) {
        boolean blockColorLookup = false;
        boolean singleBlockRender = false;
        for (StackTraceElement frame : exception.getStackTrace()) {
            if (frame.getClassName().equals(BlockColors.class.getName())
                    && frame.getMethodName().equals("getColor")) {
                blockColorLookup = true;
            } else if (frame.getClassName().equals(BlockRenderDispatcher.class.getName())
                    && frame.getMethodName().equals("renderSingleBlock")) {
                singleBlockRender = true;
            }
        }
        return blockColorLookup && singleBlockRender;
    }
}
