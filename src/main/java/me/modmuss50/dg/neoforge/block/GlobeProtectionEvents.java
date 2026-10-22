package me.modmuss50.dg.neoforge.block;

import me.modmuss50.dg.neoforge.registry.ModBlocks;
import me.modmuss50.dg.neoforge.teleport.GlobeTeleportService;
import me.modmuss50.dg.neoforge.world.GlobeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;

/** Restores the original interior's protected shell and keeps players inside their globe cell. */
public final class GlobeProtectionEvents {
    private GlobeProtectionEvents() {}

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        if (!isGlobeDimension(level)) return;
        var state = level.getBlockState(event.getPos());
        if (state.is(Blocks.BARRIER) || state.is(ModBlocks.GLOBE.get())) {
            event.setCanceled(true);
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isGlobeDimension(event.getLevel()) && event.getItemStack().is(Items.BARRIER)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isGlobeDimension(level)) return;
        if (event.getState().is(Blocks.BARRIER) || event.getState().is(ModBlocks.GLOBE.get())) {
            event.setCanceled(true);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !isGlobeDimension(player.level())) return;

        // Globe centers are 1,600 blocks apart on Z. Rounding to the nearest center keeps
        // identifying the same cell even after a player has crossed one of its walls.
        int globeId = (int) Math.round((player.getZ() - 8.0) / 1600.0);
        globeId = Math.max(0, Math.min(GlobeManager.MAX_GLOBE_ID, globeId));
        BlockPos origin = GlobeTeleportService.globeOrigin(globeId);
        var box = player.getBoundingBox();
        boolean outside = box.minX < origin.getX() + 1.0 || box.maxX > origin.getX() + 16.0
                || box.minY < origin.getY() + 1.0 || box.maxY > origin.getY() + 16.0
                || box.minZ < origin.getZ() + 1.0 || box.maxZ > origin.getZ() + 16.0;
        if (outside) {
            BlockPos exit = origin.offset(8, 0, 8);
            player.teleportTo((ServerLevel) player.level(), exit.getX() + 0.5, exit.getY() + 1.0,
                    exit.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot());
        }
    }

    private static boolean isGlobeDimension(Level level) {
        return level.dimension().equals(GlobeTeleportService.GLOBE_DIMENSION);
    }
}
