package me.modmuss50.dg.neoforge.registry;

import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import me.modmuss50.dg.neoforge.item.GlobeBlockItem;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GlobeDimensionMod.MOD_ID);
    public static final DeferredItem<GlobeBlockItem> GLOBE = ITEMS.register("globe", () -> new GlobeBlockItem(ModBlocks.GLOBE.get(), new BlockItem.Properties()));

    private ModItems() {}
}
