package net.valor.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.valor.Valor;

/**
 * Block tags owned by Valor ({@code valor:...}). Vanilla tags are used directly via
 * {@link net.minecraft.tags.BlockTags}. Contents are filled in the datagen tag provider.
 */
public final class ValorBlockTags {

	// ---- Tags go here --------------------------------------------------------------------

	private ValorBlockTags() {}

	private static TagKey<Block> create(String name) {
		return TagKey.create(Registries.BLOCK, Valor.id(name));
	}
}
