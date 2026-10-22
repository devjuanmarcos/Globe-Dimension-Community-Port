package me.modmuss50.dg.neoforge.item;

import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import me.modmuss50.dg.neoforge.block.entity.GlobeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GlobeBlockItem extends BlockItem {
    public GlobeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public ItemStack withBase(Block baseBlock) {
        return withBase(baseBlock, -1);
    }

    public ItemStack withBase(Block baseBlock, int globeId) {
        ItemStack stack = new ItemStack(this);
        CompoundTag tag = new CompoundTag();
        tag.putString("base_block", BuiltInRegistries.BLOCK.getKey(baseBlock).toString());
        if (globeId >= 0) tag.putInt("globe_id", globeId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.getLevel().dimension().location().equals(ResourceLocation.fromNamespaceAndPath(GlobeDimensionMod.MOD_ID, "globe"))) {
            Player player = context.getPlayer();
            if (player != null && !context.getLevel().isClientSide) {
                player.displayClientMessage(Component.translatable("globedimension.block.error"), false);
            }
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack, BlockState state) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && level.getBlockEntity(pos) instanceof GlobeBlockEntity globe) {
            CompoundTag tag = data.copyTag();
            ResourceLocation blockId = ResourceLocation.tryParse(tag.getString("base_block"));
            if (blockId != null) globe.setBaseBlock(BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null));
            if (tag.contains("globe_id")) globe.setGlobeId(tag.getInt("globe_id"));
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }
}
