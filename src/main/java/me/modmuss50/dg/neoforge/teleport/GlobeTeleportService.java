package me.modmuss50.dg.neoforge.teleport;

import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import me.modmuss50.dg.neoforge.block.entity.GlobeBlockEntity;
import me.modmuss50.dg.neoforge.registry.ModBlocks;
import me.modmuss50.dg.neoforge.world.GlobeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

public final class GlobeTeleportService {
    public static final ResourceKey<Level> GLOBE_DIMENSION = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(GlobeDimensionMod.MOD_ID, "globe"));

    private GlobeTeleportService() {}

    public static void use(GlobeBlockEntity globe, ServerPlayer player) {
        if (!(globe.getLevel() instanceof ServerLevel source)) return;
        if (source.dimension().equals(GLOBE_DIMENSION)) {
            leaveGlobe(globe, player);
        } else {
            enterGlobe(globe, source, player);
        }
    }

    private static void enterGlobe(GlobeBlockEntity globe, ServerLevel source, ServerPlayer player) {
        ServerLevel destination = player.getServer().getLevel(GLOBE_DIMENSION);
        if (destination == null) return;

        GlobeManager globeManager = GlobeManager.get(source);
        int id = globe.getGlobeId();
        if (!globeManager.isAllocated(id)) {
            id = globeManager.allocateId();
            globe.setGlobeId(id);
        }

        BlockPos origin = globeOrigin(id);
        BlockPos portalPos = origin.offset(8, 0, 8);
        globeManager.keepDimensionLoaded(id);
        prepareInterior(destination, id, source.dimension(), globe.getBlockPos(), globe.getBaseBlock());
        player.teleportTo(destination, portalPos.getX() + 0.5, portalPos.getY() + 1.0, portalPos.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot());
    }

    public static BlockPos globeOrigin(int id) {
        if (id < 0 || id > GlobeManager.MAX_GLOBE_ID) {
            throw new IllegalArgumentException("Globe ID is outside the supported world border: " + id);
        }
        return new BlockPos(0, 128, Math.multiplyExact(id, 1600));
    }

    public static void prepareInterior(ServerLevel destination, int id,
                                       ResourceKey<Level> returnDimension, BlockPos returnPos,
                                       net.minecraft.world.level.block.Block baseBlock) {
        BlockPos origin = globeOrigin(id);
        BlockPos portalPos = origin.offset(8, 0, 8);
        // Migrate only our own misplaced exit from the experimental build. Never clear
        // the rest of the volume: it may already contain a player's constructions.
        BlockPos experimentalExit = origin.offset(8, 8, 8);
        if (destination.getBlockEntity(experimentalExit) instanceof GlobeBlockEntity oldExit
                && oldExit.getGlobeId() == id) {
            destination.setBlockAndUpdate(experimentalExit, Blocks.AIR.defaultBlockState());
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 17; x++) {
            for (int y = 0; y < 17; y++) {
                for (int z = 0; z < 17; z++) {
                    if (x == 0 || x == 16 || y == 0 || y == 16 || z == 0 || z == 16) {
                        cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                        if (!destination.getBlockState(cursor).is(Blocks.BARRIER)) {
                            destination.setBlockAndUpdate(cursor, Blocks.BARRIER.defaultBlockState());
                        }
                    }
                }
            }
        }
        destination.setBlockAndUpdate(portalPos, ModBlocks.GLOBE.get().defaultBlockState());
        if (destination.getBlockEntity(portalPos) instanceof GlobeBlockEntity exitGlobe) {
            exitGlobe.setGlobeId(id);
            exitGlobe.setBaseBlock(baseBlock);
            exitGlobe.setReturnLocation(returnPos, returnDimension);
        }
    }

    private static void leaveGlobe(GlobeBlockEntity globe, ServerPlayer player) {
        ServerLevel fallback = player.getServer().overworld();
        ResourceKey<Level> returnDimension = globe.getReturnDimension();
        ServerLevel destination = returnDimension == null ? fallback : player.getServer().getLevel(returnDimension);
        if (destination == null) destination = fallback;

        BlockPos target = globe.getReturnPos();
        if (target == null || !destination.isInWorldBounds(target)) target = destination.getSharedSpawnPos();
        target = findSafeExit(destination, target);
        player.teleportTo(destination, target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot());
    }

    private static BlockPos findSafeExit(ServerLevel level, BlockPos requested) {
        for (int offset = 1; offset <= 4; offset++) {
            BlockPos candidate = requested.above(offset);
            if (level.isInWorldBounds(candidate)
                    && level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above()).isAir()) return candidate;
        }
        return level.getSharedSpawnPos().above();
    }
}
