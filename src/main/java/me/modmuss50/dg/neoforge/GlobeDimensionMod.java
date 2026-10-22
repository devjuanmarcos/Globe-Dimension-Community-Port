package me.modmuss50.dg.neoforge;

import net.neoforged.fml.common.Mod;
import me.modmuss50.dg.neoforge.registry.ModBlocks;
import me.modmuss50.dg.neoforge.registry.ModBlockEntities;
import me.modmuss50.dg.neoforge.registry.ModCreativeTabs;
import me.modmuss50.dg.neoforge.registry.ModItems;
import me.modmuss50.dg.neoforge.registry.ModRecipes;
import me.modmuss50.dg.neoforge.registry.ModWorldgen;
import me.modmuss50.dg.neoforge.network.GlobeNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import me.modmuss50.dg.neoforge.world.GlobeManager;
import me.modmuss50.dg.neoforge.block.GlobeExplosionProtection;
import me.modmuss50.dg.neoforge.block.GlobeProtectionEvents;

@Mod(GlobeDimensionMod.MOD_ID)
public final class GlobeDimensionMod {
    public static final String MOD_ID = "globedimension";

    public GlobeDimensionMod(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModWorldgen.CHUNK_GENERATORS.register(modEventBus);
        GlobeNetworking.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(GlobeManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(GlobeExplosionProtection::onDetonate);
        NeoForge.EVENT_BUS.addListener(GlobeProtectionEvents::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(GlobeProtectionEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(GlobeProtectionEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(GlobeProtectionEvents::onPlayerTick);
    }
}
