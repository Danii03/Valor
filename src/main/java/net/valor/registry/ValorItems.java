package net.valor.registry;

import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.valor.Valor;
import net.valor.world.item.EchoFluteItem;

/**
 * All standalone items added by Valor. Counterpart to vanilla's {@link net.minecraft.world.item.Items}.
 * Block items are registered automatically in {@link ValorBlocks}.
 * <p>
 * Example:
 * <pre>{@code
 * public static final Item RUBY = register("ruby", Item::new, new Item.Properties());
 * }</pre>
 */
public final class ValorItems {

	// ---- Items go here -------------------------------------------------------------------

	public static final Item ECHO_FLUTE = register("echo_flute", EchoFluteItem::new,
			new Item.Properties().stacksTo(1));

	private ValorItems() {}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory,
			Item.Properties properties) {
		return Registry.register(BuiltInRegistries.ITEM, Valor.id(name), factory.apply(properties));
	}

	/** Forces class loading so all static fields get registered. */
	public static void init() {}
}
