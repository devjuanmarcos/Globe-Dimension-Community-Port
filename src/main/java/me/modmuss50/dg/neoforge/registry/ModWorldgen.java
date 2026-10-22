package me.modmuss50.dg.neoforge.registry;

import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import me.modmuss50.dg.neoforge.world.GlobeChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import com.mojang.serialization.MapCodec;

public final class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, GlobeDimensionMod.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<GlobeChunkGenerator>> GLOBE =
            CHUNK_GENERATORS.register("globe", () -> GlobeChunkGenerator.CODEC);

    private ModWorldgen() {}
}
