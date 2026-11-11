package net.valor.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.valor.Valor;

/**
 * All sound events added by Valor. Counterpart to vanilla's {@link net.minecraft.sounds.SoundEvents}.
 * <p>
 * Every sound registered here also needs an entry in {@code assets/valor/sounds.json}
 * and its .ogg files in {@code assets/valor/sounds/}.
 * <p>
 * Example:
 * <pre>{@code
 * public static final SoundEvent RUBY_BLOCK_BREAK = register("block.ruby_block.break");
 * }</pre>
 */
public final class ValorSounds {

	// ---- Sounds go here ------------------------------------------------------------------

	/** Birdsong in the Whispering Woods. Several clips cut from one recording; played by the client's ambience handler. */
	public static final SoundEvent AMBIENT_WHISPERING_WOODS_BIRDS = register("ambient.whispering_woods.birds");
	/** Distant crows in the Whispering Woods. Much rarer than the birds. */
	public static final SoundEvent AMBIENT_WHISPERING_WOODS_CROWS = register("ambient.whispering_woods.crows");

	private ValorSounds() {}

	private static SoundEvent register(String name) {
		ResourceLocation id = Valor.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** Forces class loading so all static fields get registered. */
	public static void init() {}
}
