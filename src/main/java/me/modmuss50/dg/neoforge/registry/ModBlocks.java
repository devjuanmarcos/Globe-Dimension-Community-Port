package me.modmuss50.dg.neoforge.registry;

import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import me.modmuss50.dg.neoforge.block.GlobeBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GlobeDimensionMod.MOD_ID);
    public static final DeferredBlock<GlobeBlock> GLOBE = BLOCKS.register("globe", () -> new GlobeBlock(GlobeBlock.createProperties()));

    private ModBlocks() {}
}
