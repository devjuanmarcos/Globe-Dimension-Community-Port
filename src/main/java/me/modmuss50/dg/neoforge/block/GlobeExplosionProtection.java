package me.modmuss50.dg.neoforge.block;

import me.modmuss50.dg.neoforge.teleport.GlobeTeleportService;
import me.modmuss50.dg.neoforge.registry.ModBlocks;
import net.neoforged.neoforge.event.level.ExplosionEvent;

public final class GlobeExplosionProtection {
    private GlobeExplosionProtection() {}

    public static void onDetonate(ExplosionEvent.Detonate event) {
        if (!event.getLevel().dimension().equals(GlobeTeleportService.GLOBE_DIMENSION)) return;
        event.getAffectedBlocks().removeIf(pos -> event.getLevel().getBlockState(pos).is(ModBlocks.GLOBE.get()));
    }
}
