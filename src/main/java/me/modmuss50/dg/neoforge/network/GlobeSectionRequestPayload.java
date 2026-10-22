package me.modmuss50.dg.neoforge.network;

import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GlobeSectionRequestPayload(int globeId) implements CustomPacketPayload {
    public static final Type<GlobeSectionRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GlobeDimensionMod.MOD_ID, "section_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GlobeSectionRequestPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeVarInt(payload.globeId),
                    buffer -> new GlobeSectionRequestPayload(buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
