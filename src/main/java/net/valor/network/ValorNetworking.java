package net.valor.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.valor.world.item.EchoFluteItem;

/**
 * Registers all of Valor's packets and their server-side receivers.
 * Client-side receivers live in {@code ValorClientNetworking}.
 */
public final class ValorNetworking {

	private ValorNetworking() {}

	public static void init() {
		// Payload types – both directions must be registered on both sides.
		PayloadTypeRegistry.playC2S().register(ServerboundPlayEchoFluteNotePayload.TYPE,
				ServerboundPlayEchoFluteNotePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ClientboundEchoFluteNotePayload.TYPE,
				ClientboundEchoFluteNotePayload.STREAM_CODEC);

		// Server receivers (run on the server thread).
		ServerPlayNetworking.registerGlobalReceiver(ServerboundPlayEchoFluteNotePayload.TYPE, (payload, context) ->
				EchoFluteItem.playNote(context.player(), payload.hand(), payload.note()));
	}
}
