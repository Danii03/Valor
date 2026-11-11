package net.valor.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.valor.client.particle.EchoFluteParticles;
import net.valor.network.ClientboundEchoFluteNotePayload;

/** Client-side packet receivers. Payload types are registered in {@code ValorNetworking}. */
public final class ValorClientNetworking {

	private ValorClientNetworking() {}

	public static void init() {
		// Another player played a note. The sound comes from the server; only the particles are spawned here.
		ClientPlayNetworking.registerGlobalReceiver(ClientboundEchoFluteNotePayload.TYPE, (payload, context) -> {
			Entity entity = context.player().level().getEntity(payload.entityId());
			if (entity instanceof Player player) {
				EchoFluteParticles.spawn(player, payload.hand(), payload.note());
			}
		});
	}
}
