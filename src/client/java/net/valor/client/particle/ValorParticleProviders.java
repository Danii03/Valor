package net.valor.client.particle;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.valor.registry.ValorParticleTypes;

/** Connects each {@link ValorParticleTypes} entry to its client-side particle implementation. */
public final class ValorParticleProviders {

	private ValorParticleProviders() {}

	public static void init() {
		ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
		registry.register(ValorParticleTypes.ECHO_NOTE, EchoNoteParticle.Provider::new);
		registry.register(ValorParticleTypes.ECHO_SPARKLE, EchoSparkleParticle.Provider::new);
		registry.register(ValorParticleTypes.GLIMMER, GlimmerParticle.Provider::new);
	}
}
