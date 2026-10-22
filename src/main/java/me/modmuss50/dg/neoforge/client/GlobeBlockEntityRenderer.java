package me.modmuss50.dg.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import me.modmuss50.dg.neoforge.block.entity.GlobeBlockEntity;
import me.modmuss50.dg.neoforge.registry.ModBlocks;
import me.modmuss50.dg.neoforge.world.GlobeRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import org.slf4j.Logger;

public final class GlobeBlockEntityRenderer implements BlockEntityRenderer<GlobeBlockEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int renderDepth;

    public GlobeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    /**
     * The visible scene can fill the 16-block interior although its owning block entity is
     * only one block wide.  Default frustum culling would otherwise discard the whole scene
     * whenever that single block is outside the camera frustum.
     */
    @Override
    public boolean shouldRenderOffScreen(GlobeBlockEntity globe) {
        return true;
    }

    @Override
    public boolean shouldRender(GlobeBlockEntity globe, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(GlobeBlockEntity globe) {
        return GlobeRenderTransform.renderBounds(globe.getBlockPos(), globe.isInsideGlobe());
    }

    @Override
    public void render(GlobeBlockEntity globe, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (globe.getLevel() == null) return;
        boolean inner = globe.isInsideGlobe();
        if (renderDepth > 2) return;
        renderDepth++;
        try {
            if (globe.getGlobeId() >= 0) {
            pose.pushPose();
            if (inner) {
                var transform = GlobeRenderTransform.forInterior();
                pose.translate(transform.offsetX(), transform.offsetY(), transform.offsetZ());
                pose.scale(transform.scale(), transform.scale(), transform.scale());
            } else {
                pose.translate(-1.0 / 32.0, 0.0, -1.0 / 32.0);
                pose.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
            }
                Map<BlockPos, BlockState> snapshot = GlobeClientSections.get(globe.getGlobeId(), inner);
                GlobeRenderDiagnostics.markRendered(globe.getGlobeId(), snapshot.size());
                {
                    GlobeRenderView snapshotView = new GlobeRenderView(snapshot, globe.getLevel(),
                            globe.getBlockPos(), inner ? LightTexture.FULL_BRIGHT : packedLight);
                    for (Map.Entry<BlockPos, BlockState> entry : snapshot.entrySet()) {
                        BlockPos pos = entry.getKey();
                        if (!GlobeRegion.isInteriorBlock(pos.getX(), pos.getY(), pos.getZ())) continue;
                        if (entry.getValue().is(ModBlocks.GLOBE.get())) continue;
                        // The central exterior block is the entire construction room.
                        // Leave that whole 16-block-wide volume free of scenery.
                        if (inner && pos.getX() == 8 && pos.getY() == 8 && pos.getZ() == 8) continue;
                        pose.pushPose();
                        try {
                            pose.translate(pos.getX(), pos.getY(), pos.getZ());
                            GlobeBlockModelRenderer.renderInSnapshot(entry.getValue(), pos, snapshotView, pose, buffers);
                        } catch (RuntimeException | Error exception) {
                            LOGGER.error("Globe snapshot block render failed: globeId={}, inner={}, dimension={}, blockPos={}, blockState={}",
                                    globe.getGlobeId(), inner, globe.getLevel().dimension().location(), pos,
                                    entry.getValue(), exception);
                            throw exception;
                        } finally {
                            pose.popPose();
                        }
                    }
                }
            for (GlobeClientSections.RenderedEntity rendered : GlobeClientSections.getEntities(globe.getGlobeId(), inner)) {
                pose.pushPose();
                try {
                    var position = rendered.position(partialTick);
                    pose.translate(position.x, position.y, position.z);
                    Minecraft.getInstance().getEntityRenderDispatcher().render(rendered.entity(), 0.0, 0.0, 0.0,
                            rendered.yaw(partialTick), partialTick, pose, buffers, packedLight);
                } catch (RuntimeException | Error exception) {
                    LOGGER.error("Globe snapshot entity render failed: globeId={}, inner={}, dimension={}, entityType={}, entityUuid={}",
                            globe.getGlobeId(), inner, globe.getLevel().dimension().location(),
                            rendered.entity().getType(), rendered.entity().getUUID(), exception);
                    throw exception;
                } finally {
                    pose.popPose();
                }
            }
            pose.popPose();
            }
            if (inner) {
                pose.pushPose();
                pose.translate(0.0, 1.0, 0.0);
                GlobeBaseModel.render(Blocks.STONE, pose, buffers, packedLight, packedOverlay);
                pose.popPose();

                pose.pushPose();
                // The construction room starts above the physical barrier floor.
                // Its contents are real blocks; only the pedestal's surface is visual.
                pose.translate(-8.0, 0.0, -8.0);
                pose.scale(17.0f, 17.0f, 17.0f);
                GlobeBaseModel.render(globe.getBaseBlock(), pose, buffers, packedLight, packedOverlay,
                        1.0f / 17.0f);
                pose.popPose();
            } else {
                GlobeBaseModel.render(globe.getBaseBlock(), pose, buffers, packedLight, packedOverlay);
            }
        } finally {
            renderDepth--;
        }
    }
}
