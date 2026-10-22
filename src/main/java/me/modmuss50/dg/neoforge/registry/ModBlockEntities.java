package me.modmuss50.dg.neoforge.registry;

import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import me.modmuss50.dg.neoforge.block.entity.GlobeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GlobeDimensionMod.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GlobeBlockEntity>> GLOBE =
            BLOCK_ENTITIES.register("globe", () -> BlockEntityType.Builder.of(GlobeBlockEntity::new, ModBlocks.GLOBE.get()).build(null));

    private ModBlockEntities() {}
}
