package net.valor.client;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.util.FastColor;
import net.valor.registry.ValorBlocks;

/**
 * Block and item tints. Counterpart to vanilla's {@code BlockColors} / {@code ItemColors}.
 */
public final class ValorBlockColors {
	/**
	 * Fixed tint of Whispering Leaves, like vanilla's fixed spruce tint ({@code FoliageColor#getEvergreenColor}).
	 * The spruce leaves texture is grey with an average brightness of 126/255, and the tint multiplies it:
	 * this tint makes the leaves average {@code #213D32} on screen, a dark, cool forest green.
	 */
	public static final int WHISPERING_LEAVES = 0x437B65;

	private ValorBlockColors() {}

	public static void init() {
		int leaves = FastColor.ARGB32.opaque(WHISPERING_LEAVES);
		ColorProviderRegistry.BLOCK.register((state, level, pos, tintIndex) -> leaves, ValorBlocks.WHISPERING_LEAVES);
		ColorProviderRegistry.ITEM.register((stack, tintIndex) -> leaves, ValorBlocks.WHISPERING_LEAVES);
	}
}
