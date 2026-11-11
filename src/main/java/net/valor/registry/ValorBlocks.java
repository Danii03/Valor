package net.valor.registry;

import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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

	/** Forces class loading so all static fields get registered. */
	public static void init() {}
}
