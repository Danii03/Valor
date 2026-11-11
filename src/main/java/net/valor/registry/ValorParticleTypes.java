package net.valor.registry;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.valor.Valor;

/**
 * All particle types added by Valor. Counterpart to vanilla's {@link net.minecraft.core.particles.ParticleTypes}.
 * <p>
 * Every type needs a sprite definition in {@code assets/valor/particles/<name>.json} and a
 * provider registered in {@code ValorParticleProviders} (client).
 */
public final class ValorParticleTypes {

	/** Tinted music note rising from the Echo Flute. Uses vanilla's note sprite. */
	public static final ParticleType<ColorParticleOption> ECHO_NOTE = register("echo_note",
			FabricParticleTypes.complex(ColorParticleOption::codec, ColorParticleOption::streamCodec));

	/** Tiny tinted glitter that trails an {@link #ECHO_NOTE}. Uses vanilla's end rod sprites. */
	public static final ParticleType<ColorParticleOption> ECHO_SPARKLE = register("echo_sparkle",
			FabricParticleTypes.complex(ColorParticleOption::codec, ColorParticleOption::streamCodec));

	private ValorParticleTypes() {}

	private static <T extends ParticleType<?>> T register(String name, T type) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Valor.id(name), type);
	}

	/** Forces class loading so all static fields get registered. */
	public static void init() {}
}
