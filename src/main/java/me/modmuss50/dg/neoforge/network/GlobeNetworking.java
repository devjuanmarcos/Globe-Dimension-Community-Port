package me.modmuss50.dg.neoforge.network;

import me.modmuss50.dg.neoforge.client.GlobeClientSections;
import me.modmuss50.dg.neoforge.world.GlobeSectionManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class GlobeNetworking {
    private GlobeNetworking() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GlobeNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(GlobeSectionPayload.TYPE, GlobeSectionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> GlobeClientSections.accept(payload)));
        registrar.playToServer(GlobeSectionRequestPayload.TYPE, GlobeSectionRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        GlobeSectionManager.sendItemSection(player, payload.globeId());
                    }
                }));
    }
}
