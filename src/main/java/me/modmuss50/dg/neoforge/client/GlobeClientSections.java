package me.modmuss50.dg.neoforge.client;

import me.modmuss50.dg.neoforge.network.GlobeSectionPayload;
import me.modmuss50.dg.neoforge.world.GlobeRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import me.modmuss50.dg.neoforge.network.GlobeSectionRequestPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public final class GlobeClientSections {
    private static final int MAX_BLOCKS = 15 * 15 * 15;
    private static final LinkedHashMap<Key, Map<BlockPos, BlockState>> SECTIONS = new LinkedHashMap<>(16, 0.75f, true);
    private static final LinkedHashMap<Key, List<RenderedEntity>> ENTITIES = new LinkedHashMap<>(16, 0.75f, true);
    private static final LinkedHashMap<Integer, Long> LAST_REQUEST_NANOS = new LinkedHashMap<>(16, 0.75f, true);

    private GlobeClientSections() {}

    public static synchronized void accept(GlobeSectionPayload payload) {
        Key sectionKey = new Key(payload.globeId(), payload.inner());
        if (payload.hasBlocks()) {
            Map<BlockPos, BlockState> states = new HashMap<>();
            CompoundTag tag = payload.blocks();
            int count = 0;
            for (String key : tag.getAllKeys()) {
                if (++count > MAX_BLOCKS) return;
                CompoundTag entry = tag.getCompound(key);
                long packedPos = entry.getLong("pos");
                BlockPos pos = BlockPos.of(packedPos);
                if (!GlobeRegion.isInteriorBlock(pos.getX(), pos.getY(), pos.getZ())) continue;
                BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry.getCompound("state"));
                if (!state.isAir()) states.put(pos, state);
            }
            SECTIONS.put(sectionKey, Map.copyOf(states));
        }

        if (payload.hasEntities() && Minecraft.getInstance().level != null) {
            Map<UUID, RenderedEntity> previous = new HashMap<>();
            for (RenderedEntity rendered : ENTITIES.getOrDefault(sectionKey, List.of())) {
                previous.put(rendered.uuid, rendered);
            }
            List<RenderedEntity> entities = new java.util.ArrayList<>();
            CompoundTag entityTag = payload.entities();
            for (String key : entityTag.getAllKeys()) {
                if (entities.size() >= 32) break;
                CompoundTag entry = entityTag.getCompound(key);
                ResourceLocation typeId = ResourceLocation.tryParse(entry.getString("type"));
                if (typeId == null) continue;
                UUID uuid;
                try {
                    uuid = UUID.fromString(entry.getString("uuid"));
                } catch (IllegalArgumentException exception) {
                    continue;
                }
                Vec3 position = new Vec3(entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"));
                float yaw = entry.getFloat("yaw");
                float pitch = entry.getFloat("pitch");
                RenderedEntity existing = previous.remove(uuid);
                if (existing != null && BuiltInRegistries.ENTITY_TYPE.getKey(existing.entity.getType()).equals(typeId)) {
                    existing.updateSnapshot(position, yaw, pitch);
                    entities.add(existing);
                    continue;
                }
                Entity entity;
                if (typeId.equals(ResourceLocation.withDefaultNamespace("player"))) continue;
                var type = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(null);
                if (type == null) continue;
                entity = type.create(Minecraft.getInstance().level);
                if (entity == null) continue;
                entity.load(entry.getCompound("data"));
                entity.setPos(0.0, 0.0, 0.0);
                entity.xo = entity.yo = entity.zo = 0.0;
                entity.setYRot(yaw);
                entity.setXRot(pitch);
                entities.add(new RenderedEntity(uuid, entity, position, yaw, pitch));
            }
            ENTITIES.put(sectionKey, List.copyOf(entities));
        }
        trimSections();
    }

    public static synchronized Map<BlockPos, BlockState> get(int globeId, boolean inner) {
        return SECTIONS.getOrDefault(new Key(globeId, inner), Map.of());
    }

    public static synchronized java.util.List<RenderedEntity> getEntities(int globeId, boolean inner) {
        List<RenderedEntity> entities = ENTITIES.getOrDefault(new Key(globeId, inner), List.of());
        if (Minecraft.getInstance().level != null) {
            long clientTick = Minecraft.getInstance().level.getGameTime();
            for (RenderedEntity rendered : entities) rendered.advance(clientTick);
        }
        return entities;
    }

    public static synchronized void requestItemSection(int globeId) {
        if (globeId < 0 || globeId > me.modmuss50.dg.neoforge.world.GlobeManager.MAX_GLOBE_ID
                || Minecraft.getInstance().getConnection() == null) return;
        long now = System.nanoTime();
        long previous = LAST_REQUEST_NANOS.getOrDefault(globeId, 0L);
        if (now - previous < 1_000_000_000L) return;
        LAST_REQUEST_NANOS.put(globeId, now);
        while (LAST_REQUEST_NANOS.size() > 256) {
            LAST_REQUEST_NANOS.remove(LAST_REQUEST_NANOS.keySet().iterator().next());
        }
        PacketDistributor.sendToServer(new GlobeSectionRequestPayload(globeId));
    }

    private static void trimSections() {
        while (SECTIONS.size() > 64) {
            Key oldest = SECTIONS.keySet().iterator().next();
            SECTIONS.remove(oldest);
            ENTITIES.remove(oldest);
        }
    }

    public static final class RenderedEntity {
        private final UUID uuid;
        private final Entity entity;
        private Vec3 previousPosition;
        private Vec3 currentPosition;
        private float previousYaw;
        private float currentYaw;
        private float previousPitch;
        private float currentPitch;
        private long lastClientTick = Long.MIN_VALUE;

        private RenderedEntity(UUID uuid, Entity entity, Vec3 position, float yaw, float pitch) {
            this.uuid = uuid;
            this.entity = entity;
            this.previousPosition = position;
            this.currentPosition = position;
            this.previousYaw = yaw;
            this.currentYaw = yaw;
            this.previousPitch = pitch;
            this.currentPitch = pitch;
        }

        private void updateSnapshot(Vec3 position, float yaw, float pitch) {
            previousPosition = currentPosition;
            currentPosition = position;
            previousYaw = currentYaw;
            currentYaw = yaw;
            previousPitch = currentPitch;
            currentPitch = pitch;
        }

        private void advance(long clientTick) {
            if (lastClientTick == clientTick) return;
            lastClientTick = clientTick;
            entity.xo = entity.getX();
            entity.yo = entity.getY();
            entity.zo = entity.getZ();
            entity.yRotO = entity.getYRot();
            entity.xRotO = entity.getXRot();
            entity.setPos(currentPosition);
            entity.setYRot(currentYaw);
            entity.setXRot(currentPitch);
            if (entity instanceof LivingEntity living) living.calculateEntityAnimation(false);
            entity.tickCount++;
        }

        public Entity entity() { return entity; }
        public Vec3 position(float partialTick) {
            return new Vec3(Mth.lerp(partialTick, previousPosition.x, currentPosition.x),
                    Mth.lerp(partialTick, previousPosition.y, currentPosition.y),
                    Mth.lerp(partialTick, previousPosition.z, currentPosition.z));
        }
        public float yaw(float partialTick) { return Mth.rotLerp(partialTick, previousYaw, currentYaw); }
        public float pitch(float partialTick) { return Mth.lerp(partialTick, previousPitch, currentPitch); }
    }

    private record Key(int globeId, boolean inner) {}
}
