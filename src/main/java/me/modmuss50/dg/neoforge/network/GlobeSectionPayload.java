package me.modmuss50.dg.neoforge.network;

import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record GlobeSectionPayload(int globeId, boolean inner, boolean hasBlocks, boolean hasEntities,
                                  CompoundTag blocks, CompoundTag entities) implements CustomPacketPayload {
    public static final Type<GlobeSectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GlobeDimensionMod.MOD_ID, "section_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GlobeSectionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.globeId);
                buffer.writeBoolean(payload.inner);
                buffer.writeBoolean(payload.hasBlocks);
                buffer.writeBoolean(payload.hasEntities);
                buffer.writeNbt(payload.blocks);
                buffer.writeNbt(payload.entities);
            },
            GlobeSectionPayload::decode);

    private static GlobeSectionPayload decode(RegistryFriendlyByteBuf buffer) {
        int id = buffer.readVarInt();
        boolean inner = buffer.readBoolean();
        boolean hasBlocks = buffer.readBoolean();
        boolean hasEntities = buffer.readBoolean();
        CompoundTag blocks = buffer.readNbt();
        CompoundTag entities = buffer.readNbt();
        return new GlobeSectionPayload(id, inner, hasBlocks, hasEntities, blocks == null ? new CompoundTag() : blocks,
                entities == null ? new CompoundTag() : entities);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
