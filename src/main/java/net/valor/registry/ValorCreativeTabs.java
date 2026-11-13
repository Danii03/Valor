package net.valor.registry;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

/**
 * Places Valor's content into the <b>vanilla</b> creative tabs, next to related vanilla entries,
 * so new content feels like part of the game instead of living in a separate mod tab.
 * <p>
 * Example:
 * <pre>{@code
 * ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries ->
 *         entries.addAfter(Items.DIAMOND_BLOCK, ValorBlocks.RUBY_BLOCK));
 * }</pre>
 */
public final class ValorCreativeTabs {

	private ValorCreativeTabs() {}

	public static void init() {
		// ---- Creative tab placements go here ---------------------------------------------

		// Leaves: next to the vanilla spruce leaves.
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries ->
				entries.addAfter(Items.SPRUCE_LEAVES, ValorBlocks.WHISPERING_LEAVES));

		// Instruments: after the goat horns, before the music discs.
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries ->
				entries.addBefore(Items.MUSIC_DISC_13, ValorItems.ECHO_FLUTE));

		// Altar: among the functional blocks, after the lectern (it holds an item the same way).
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries ->
				entries.addAfter(Items.LECTERN, ValorBlocks.ECHO_FLUTE_ALTAR));
	}
}
