package net.valor.client;

import net.fabricmc.api.ClientModInitializer;
import net.valor.client.network.ValorClientNetworking;
import net.valor.client.particle.ValorParticleProviders;
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
		ValorParticleProviders.init();
		ValorClientNetworking.init();
	}
}
