package me.modmuss50.dg.neoforge.world;

import me.modmuss50.dg.neoforge.block.entity.GlobeBlockEntity;
import me.modmuss50.dg.neoforge.network.GlobeSectionPayload;
import me.modmuss50.dg.neoforge.registry.ModItems;
import me.modmuss50.dg.neoforge.teleport.GlobeTeleportService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GlobeSectionManager {
    private static final Map<UUID, Long> LAST_ITEM_REQUEST_TICK = new HashMap<>();
    private GlobeSectionManager() {}

    public static void updateAndSync(GlobeBlockEntity globe, boolean includeBlocks) {
        if (!(globe.getLevel() instanceof ServerLevel source)) return;
        // This path only reads snapshots. The interior's player-built blocks must
        // never be replaced with exterior blocks, including on periodic updates.
        GlobeManager manager = GlobeManager.get(source);
        if (!manager.isAllocated(globe.getGlobeId())) return;
        boolean inner = globe.isInsideGlobe();
        ServerLevel viewed = inner
                ? (globe.getReturnDimension() == null ? null : source.getServer().getLevel(globe.getReturnDimension()))
                : source.getServer().getLevel(GlobeTeleportService.GLOBE_DIMENSION);
        if (viewed == null) return;

        BlockPos origin;
        if (inner) {
            if (globe.getReturnPos() == null) return;
            origin = globe.getReturnPos().offset(-8, -8, -8);
        } else {
            origin = GlobeTeleportService.globeOrigin(globe.getGlobeId());
        }

        boolean hasNearbyPlayers = source.players().stream().anyMatch(player ->
                player.distanceToSqr(Vec3.atLowerCornerOf(globe.getBlockPos())) < 64.0);
        if (!hasNearbyPlayers) return;
        manager.keepDimensionLoaded(globe.getGlobeId());
        GlobeSectionPayload payload = createPayload(globe.getGlobeId(), inner, viewed, origin, includeBlocks, true);
        for (ServerPlayer player : source.players()) {
            if (player.distanceToSqr(Vec3.atLowerCornerOf(globe.getBlockPos())) < 64.0) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static void sendItemSection(ServerPlayer player, int globeId) {
        if (!playerCarriesGlobeId(player, globeId)) return;
        long now = player.getServer().overworld().getGameTime();
        long lastRequest = LAST_ITEM_REQUEST_TICK.getOrDefault(player.getUUID(), -20L);
        if (now - lastRequest < 20L) return;
        LAST_ITEM_REQUEST_TICK.put(player.getUUID(), now);
        GlobeManager manager = GlobeManager.get(player.serverLevel());
        if (!manager.isAllocated(globeId)) return;
        ServerLevel viewed = player.getServer().getLevel(GlobeTeleportService.GLOBE_DIMENSION);
        if (viewed == null) return;
        manager.keepDimensionLoaded(globeId);
        GlobeSectionPayload payload = createPayload(globeId, false, viewed,
                GlobeTeleportService.globeOrigin(globeId), true, true);
        PacketDistributor.sendToPlayer(player, payload);
    }

    private static boolean playerCarriesGlobeId(ServerPlayer player, int globeId) {
        for (ItemStack stack : player.getInventory().items) {
            if (hasGlobeId(stack, globeId)) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (hasGlobeId(stack, globeId)) return true;
        }
        return false;
    }

    private static boolean hasGlobeId(ItemStack stack, int globeId) {
        if (!stack.is(ModItems.GLOBE.get())) return false;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().getInt("globe_id") == globeId;
    }

    private static GlobeSectionPayload createPayload(int globeId, boolean inner, ServerLevel viewed, BlockPos origin,
                                                     boolean includeBlocks, boolean includeEntities) {
        CompoundTag encodedBlocks = new CompoundTag();
        int blockCount = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        if (includeBlocks) {
            for (int x = 1; x <= 15; x++) {
                for (int y = 1; y <= 15; y++) {
                    for (int z = 1; z <= 15; z++) {
                        cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                        BlockState state = viewed.getBlockState(cursor);
                        if (state.isAir()) continue;
                        CompoundTag entry = new CompoundTag();
                        entry.put("state", NbtUtils.writeBlockState(state));
                        entry.putLong("pos", BlockPos.asLong(x, y, z));
                        encodedBlocks.put("block_" + blockCount++, entry);
                    }
                }
            }
        }

        CompoundTag encodedEntities = new CompoundTag();
        int entityCount = 0;
        AABB captureBounds = new AABB(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + 17.0, origin.getY() + 17.0, origin.getZ() + 17.0);
        for (Entity entity : includeEntities ? viewed.getEntities(null, captureBounds) : java.util.List.<Entity>of()) {
            // Player inventories and profile data are private; the old implementation serialized these to nearby clients.
            if (entity instanceof Player || entity.isRemoved() || entityCount >= 32) continue;
            CompoundTag entityData = new CompoundTag();
            entity.saveWithoutId(entityData);
            entityData.remove("Passengers");
            if (entityData.toString().length() > 8192) continue;
            CompoundTag entry = new CompoundTag();
            ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            entry.putString("type", typeId.toString());
            entry.putString("uuid", entity.getStringUUID());
            entry.putFloat("yaw", entity.getYRot());
            entry.putFloat("pitch", entity.getXRot());
            entry.putDouble("x", entity.getX() - origin.getX());
            entry.putDouble("y", entity.getY() - origin.getY());
            entry.putDouble("z", entity.getZ() - origin.getZ());
            entry.put("data", entityData);
            encodedEntities.put("entity_" + entityCount++, entry);
        }
        return new GlobeSectionPayload(globeId, inner, includeBlocks, includeEntities, encodedBlocks, encodedEntities);
    }
}
