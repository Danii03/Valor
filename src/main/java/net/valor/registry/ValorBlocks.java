package net.valor.registry;

import java.util.function.Function;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.valor.Valor;

/**
 * All blocks added by Valor. Counterpart to vanilla's {@link net.minecraft.world.level.block.Blocks}.
 * <p>
 * Example:
 * <pre>{@code
 * public static final Block RUBY_BLOCK = register("ruby_block", Block::new,
 *         BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK));
 * }</pre>
 */
public final class ValorBlocks {

	// ---- Blocks go here ------------------------------------------------------------------

	/**
	 * Dark, cool-green spruce foliage of the Whispering Woods trees. Same behavior as spruce leaves
	 * (decay, light, sapling drops); the color comes from a fixed tint, like vanilla's spruce and birch
	 * leaves, so it looks the same wherever the block is.
	 */
	public static final Block WHISPERING_LEAVES = register("whispering_leaves", LeavesBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES));

	private ValorBlocks() {}

	/** Registers a block together with its matching {@link BlockItem}. */
	private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory,
			BlockBehaviour.Properties properties) {
		return register(name, factory, properties, new Item.Properties());
	}

	/** Registers a block together with a {@link BlockItem} that uses custom item properties. */
	private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory,
			BlockBehaviour.Properties properties, Item.Properties itemProperties) {
		T block = registerWithoutItem(name, factory, properties);
		Registry.register(BuiltInRegistries.ITEM, Valor.id(name), new BlockItem(block, itemProperties));
		return block;
	}

	/** Registers a block without an item (e.g. wall variants, crops, technical blocks). */
	private static <T extends Block> T registerWithoutItem(String name, Function<BlockBehaviour.Properties, T> factory,
			BlockBehaviour.Properties properties) {
		return Registry.register(BuiltInRegistries.BLOCK, Valor.id(name), factory.apply(properties));
	}

	/** Registers the blocks and their vanilla interactions (fire, composter). */
	public static void init() {
		// Same values as vanilla leaves (FireBlock#bootStrap, ComposterBlock#bootStrap).
		FlammableBlockRegistry.getDefaultInstance().add(WHISPERING_LEAVES, 30, 60);
		CompostingChanceRegistry.INSTANCE.add(WHISPERING_LEAVES, 0.3F);
	}
}
