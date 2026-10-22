package me.modmuss50.dg.neoforge.block;

import com.mojang.serialization.MapCodec;
import me.modmuss50.dg.neoforge.block.entity.GlobeBlockEntity;
import me.modmuss50.dg.neoforge.registry.ModBlockEntities;
import me.modmuss50.dg.neoforge.registry.ModItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public final class GlobeBlock extends BaseEntityBlock {
    public static final MapCodec<GlobeBlock> CODEC = BlockBehaviour.simpleCodec(GlobeBlock::new);

    public GlobeBlock(BlockBehaviour.Properties properties) {
        super(properties.noOcclusion());
    }

    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GlobeBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof GlobeBlockEntity globe) {
            globe.use(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            ItemStack drop = getGlobeStack(level, pos);
            if (!drop.isEmpty()) popResource(level, pos, drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return getGlobeStack(level, pos);
    }

    private ItemStack getGlobeStack(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GlobeBlockEntity globe) {
            return ModItems.GLOBE.get().withBase(globe.getBaseBlock(), globe.getGlobeId());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.GLOBE.get(), GlobeBlockEntityTicker::tick);
    }
}
