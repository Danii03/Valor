package net.valor.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.valor.Valor;

/**
 * Biome tags owned by Valor ({@code valor:...}). Vanilla tags are used directly via
 * {@link net.minecraft.tags.BiomeTags}. Contents are filled in the datagen tag provider.
 */
public final class ValorBiomeTags {

	/** Biomes the Whispering Woods ruins (Fallen Gate, Sunken Ruin, Tower Ruin) may generate in. */
	public static final TagKey<Biome> HAS_WHISPERING_WOODS_RUINS = create("has_structure/whispering_woods_ruins");

	private ValorBiomeTags() {}

	private static TagKey<Biome> create(String name) {
		return TagKey.create(Registries.BIOME, Valor.id(name));
	}
}
