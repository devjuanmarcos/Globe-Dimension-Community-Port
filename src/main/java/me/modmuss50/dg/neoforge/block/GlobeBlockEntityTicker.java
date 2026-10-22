package me.modmuss50.dg.neoforge.block;

import me.modmuss50.dg.neoforge.block.entity.GlobeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

final class GlobeBlockEntityTicker {
    private GlobeBlockEntityTicker() {}

    static void tick(Level level, BlockPos pos, BlockState state, GlobeBlockEntity globe) {
        if (!level.isClientSide) {
            boolean includeBlocks = level.getGameTime() % 20L == 0L;
            me.modmuss50.dg.neoforge.world.GlobeSectionManager.updateAndSync(globe, includeBlocks);
        }
    }
}
