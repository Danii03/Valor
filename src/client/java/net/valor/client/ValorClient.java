package net.valor.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.valor.client.network.ValorClientNetworking;
import net.valor.client.particle.ValorParticleProviders;
import net.valor.client.renderer.EchoFluteHandAnimation;
import net.valor.client.renderer.ForestMistRenderer;
import net.valor.platform.ClientHooks;

/**
 * Client entrypoint of Valor. Only rendering and other client-only setup belongs here
 * (render layers, entity renderers, model layers, particles, screens, color providers).
 */
public final class ValorClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientHooks.set(new ValorClientHooks());
		ValorRenderLayers.init();
		ValorBlockColors.init();
		ValorParticleProviders.init();
		ValorClientNetworking.init();
		ClientTickEvents.END_CLIENT_TICK.register(EchoFluteHandAnimation::tick);
		ClientTickEvents.END_CLIENT_TICK.register(WhisperingWoodsAmbience::tick);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(ForestMistRenderer::render);
	}
}
