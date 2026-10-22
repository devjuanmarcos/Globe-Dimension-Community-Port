package me.modmuss50.dg.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

import java.util.Map;

public final class GlobeItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final GlobeItemRenderer INSTANCE = new GlobeItemRenderer();

    private GlobeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack pose,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData == null ? new CompoundTag() : customData.copyTag();
        net.minecraft.resources.ResourceLocation baseId =
                net.minecraft.resources.ResourceLocation.tryParse(tag.getString("base_block"));
        Block base = baseId == null ? Blocks.OAK_PLANKS
                : BuiltInRegistries.BLOCK.getOptional(baseId).orElse(Blocks.OAK_PLANKS);

        if (tag.contains("globe_id")) {
            int globeId = tag.getInt("globe_id");
            GlobeClientSections.requestItemSection(globeId);
            pose.pushPose();
            pose.translate(-1.0 / 32.0, 0.0, -1.0 / 32.0);
            pose.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
            for (Map.Entry<BlockPos, BlockState> entry : GlobeClientSections.get(globeId, false).entrySet()) {
                pose.pushPose();
                pose.translate(entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ());
                GlobeBlockModelRenderer.render(entry.getValue(), pose, buffers, packedLight,
                        OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            for (GlobeClientSections.RenderedEntity rendered : GlobeClientSections.getEntities(globeId, false)) {
                pose.pushPose();
                var position = rendered.position(0.0f);
                pose.translate(position.x, position.y, position.z);
                Minecraft.getInstance().getEntityRenderDispatcher().render(rendered.entity(), 0.0, 0.0, 0.0,
                        rendered.yaw(0.0f), 0.0f, pose, buffers, packedLight);
                pose.popPose();
            }
            pose.popPose();
        }

        // Item display transforms shrink the model; use a sturdier pedestal so an empty globe
        // remains visible in first-person and third-person hand views.
        GlobeBaseModel.render(base, pose, buffers, packedLight, OverlayTexture.NO_OVERLAY, 0.25f);
    }
}
