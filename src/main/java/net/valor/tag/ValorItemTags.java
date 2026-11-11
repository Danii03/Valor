package net.valor.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.valor.Valor;

/**
 * Item tags owned by Valor ({@code valor:...}). Vanilla tags are used directly via
 * {@link net.minecraft.tags.ItemTags}. Contents are filled in the datagen tag provider.
 */
public final class ValorItemTags {

	// ---- Tags go here --------------------------------------------------------------------

	private ValorItemTags() {}

	private static TagKey<Item> create(String name) {
		return TagKey.create(Registries.ITEM, Valor.id(name));
	}
}
