package me.modmuss50.dg.neoforge.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import me.modmuss50.dg.neoforge.teleport.GlobeTeleportService;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class GlobeManager extends SavedData {
    private static final String STORAGE_NAME = "globedimension_globes";
    private static final String NEXT_ID = "next_id";
    public static final int MAX_GLOBE_ID = 18_749;
    private static final long KEEP_ALIVE_TICKS = 200L;
    private final MinecraftServer server;
    private int nextId;
    private final Map<Integer, Long> activeGlobeTicks = new HashMap<>();

    public static GlobeManager get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        MinecraftServer server = overworld.getServer();
        Factory<GlobeManager> factory = new Factory<>(() -> new GlobeManager(server),
                (tag, registries) -> GlobeManager.load(server, tag, registries));
        return overworld.getDataStorage().computeIfAbsent(factory, STORAGE_NAME);
    }

    private GlobeManager(MinecraftServer server) {
        this.server = server;
    }

    public int allocateId() {
        if (nextId > MAX_GLOBE_ID) throw new IllegalStateException("Globe ID space exhausted");
        int allocated = nextId++;
        setDirty();
        return allocated;
    }

    public boolean isAllocated(int id) {
        return id >= 0 && id < nextId && id <= MAX_GLOBE_ID;
    }

    public void keepDimensionLoaded(int id) {
        if (id < 0 || id > MAX_GLOBE_ID) return;
        ServerLevel globeLevel = this.getServerLevel();
        if (globeLevel == null) return;
        BlockPos origin = GlobeTeleportService.globeOrigin(id);
        int minChunkX = origin.getX() >> 4;
        int maxChunkX = (origin.getX() + 16) >> 4;
        int minChunkZ = origin.getZ() >> 4;
        int maxChunkZ = (origin.getZ() + 16) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                globeLevel.setChunkForced(chunkX, chunkZ, true);
            }
        }
        activeGlobeTicks.put(id, this.getOverworld().getGameTime());
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        GlobeManager manager = get(event.getServer().overworld());
        long now = event.getServer().overworld().getGameTime();
        ServerLevel globeLevel = event.getServer().getLevel(GlobeTeleportService.GLOBE_DIMENSION);
        if (globeLevel == null) return;
        Iterator<Map.Entry<Integer, Long>> iterator = manager.activeGlobeTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Long> entry = iterator.next();
            if (now - entry.getValue() <= KEEP_ALIVE_TICKS) continue;
            BlockPos origin = GlobeTeleportService.globeOrigin(entry.getKey());
            int minChunkX = origin.getX() >> 4;
            int maxChunkX = (origin.getX() + 16) >> 4;
            int minChunkZ = origin.getZ() >> 4;
            int maxChunkZ = (origin.getZ() + 16) >> 4;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    globeLevel.setChunkForced(chunkX, chunkZ, false);
                }
            }
            iterator.remove();
        }
    }

    private ServerLevel getOverworld() {
        return server.overworld();
    }

    private ServerLevel getServerLevel() {
        return server.getLevel(GlobeTeleportService.GLOBE_DIMENSION);
    }

    private static GlobeManager load(MinecraftServer server, CompoundTag tag, HolderLookup.Provider registries) {
        GlobeManager manager = new GlobeManager(server);
        manager.nextId = Math.max(0, tag.getInt(NEXT_ID));
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(NEXT_ID, nextId);
        return tag;
    }
}
