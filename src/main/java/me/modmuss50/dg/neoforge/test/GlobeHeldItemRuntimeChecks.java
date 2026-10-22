package me.modmuss50.dg.neoforge.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import me.modmuss50.dg.neoforge.client.GlobeClientSections;
import me.modmuss50.dg.neoforge.network.GlobeSectionPayload;
import me.modmuss50.dg.neoforge.registry.ModItems;
import me.modmuss50.dg.neoforge.world.GlobeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/** Opt-in client integration check. Runs at the title screen without touching a save. */
@EventBusSubscriber(modid = GlobeDimensionMod.MOD_ID, value = Dist.CLIENT)
public final class GlobeHeldItemRuntimeChecks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean checked;

    private GlobeHeldItemRuntimeChecks() {}

    @SubscribeEvent
    public static void check(ClientTickEvent.Post event) {
        if (checked || !Boolean.getBoolean("globedimension.verifyHeldItem")) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getOverlay() != null || minecraft.getItemRenderer() == null) return;
        checked = true;
        int id = GlobeManager.MAX_GLOBE_ID;
        ItemStack empty = ModItems.GLOBE.get().withBase(Blocks.OAK_PLANKS);
        ItemStack built = ModItems.GLOBE.get().withBase(Blocks.OAK_PLANKS, id);
        CompoundTag blocks = new CompoundTag();
        CompoundTag block = new CompoundTag();
        block.put("state", NbtUtils.writeBlockState(Blocks.STONE.defaultBlockState()));
        block.putLong("pos", BlockPos.asLong(8, 1, 8));
        blocks.put("block_0", block);
        GlobeClientSections.accept(new GlobeSectionPayload(id, false, true, false, blocks, new CompoundTag()));
        try {
            boolean holdMyItems = ModList.get().isLoaded("holdmyitemsnf");
            if (holdMyItems) {
                var method = Class.forName("de.bene2212.holdmyitemsnf.util.RenderHelper")
                        .getMethod("shouldRenderCustom", ItemStack.class);
                for (ItemStack stack : new ItemStack[]{empty, built}) {
                    if (!Boolean.FALSE.equals(method.invoke(null, stack))) {
                        throw new IllegalStateException("Hold My Items bypasses the globe item renderer");
                    }
                }
                if (!Boolean.TRUE.equals(method.invoke(null, Items.STONE.getDefaultInstance()))) {
                    throw new IllegalStateException("The compatibility patch changed normal stone handling");
                }
            }
            for (ItemDisplayContext context : new ItemDisplayContext[]{
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    ItemDisplayContext.GUI}) {
                Counter emptyOutput = render(minecraft, empty, context);
                Counter builtOutput = render(minecraft, built, context);
                if (emptyOutput.vertices == 0 || builtOutput.vertices <= emptyOutput.vertices) {
                    throw new IllegalStateException("Missing globe geometry in " + context
                            + ": empty=" + emptyOutput.vertices + ", built=" + builtOutput.vertices);
                }
                LOGGER.info("[Globe held item verification] context={}, emptyVertices={}, builtVertices={}",
                        context, emptyOutput.vertices, builtOutput.vertices);
            }
            LOGGER.info("[Globe held item verification] PASS: holdMyItems={}, empty/built, five display contexts",
                    holdMyItems);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot verify Hold My Items globe compatibility", exception);
        } finally {
            GlobeClientSections.accept(new GlobeSectionPayload(id, false, true, true,
                    new CompoundTag(), new CompoundTag()));
        }
        if (Boolean.getBoolean("globedimension.exitAfterHeldItemVerification")) minecraft.stop();
    }

    private static Counter render(Minecraft minecraft, ItemStack stack, ItemDisplayContext context) {
        Counter counter = new Counter();
        minecraft.getItemRenderer().renderStatic(stack, context, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, new PoseStack(), type -> counter, null, 0);
        return counter;
    }

    private static final class Counter implements VertexConsumer {
        int vertices;
        public VertexConsumer addVertex(float x, float y, float z) { vertices++; return this; }
        public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        public VertexConsumer setUv(float u, float v) { return this; }
        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) { return this; }
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
