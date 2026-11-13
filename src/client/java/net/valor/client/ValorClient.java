package net.valor.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.valor.client.network.ValorClientNetworking;
import net.valor.client.particle.ValorParticleProviders;
import net.valor.client.renderer.EchoFluteHandAnimation;
import net.valor.client.renderer.ForestMistRenderer;
import net.valor.client.renderer.blockentity.EchoFluteAltarRenderer;
import net.valor.client.resources.sounds.EchoFluteAltarSoundHandler;
import net.valor.platform.ClientHooks;
import net.valor.registry.ValorBlockEntityTypes;

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
		ClientTickEvents.END_CLIENT_TICK.register(EchoFluteAltarSoundHandler::tick);
		BlockEntityRenderers.register(ValorBlockEntityTypes.ECHO_FLUTE_ALTAR, EchoFluteAltarRenderer::new);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(ForestMistRenderer::render);
	}
}
