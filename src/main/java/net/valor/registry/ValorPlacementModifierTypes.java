package net.valor.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.valor.Valor;
import net.valor.world.level.levelgen.placement.WhisperingWoodsZoneFilter;

/**
 * Placement modifier types added by Valor. Counterpart to vanilla's {@link PlacementModifierType} constants.
 */
public final class ValorPlacementModifierTypes {

	/** Thins out placement attempts per Whispering Woods zone (Outer Woods, Deepwood, Hidden Groves). */
	public static final PlacementModifierType<WhisperingWoodsZoneFilter> WHISPERING_WOODS_ZONE = register("whispering_woods_zone",
			WhisperingWoodsZoneFilter.CODEC);

	private ValorPlacementModifierTypes() {}

	private static <P extends PlacementModifier> PlacementModifierType<P> register(String name, MapCodec<P> codec) {
		return Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, Valor.id(name), () -> codec);
	}

	/** Forces class loading so all static fields get registered. */
	public static void init() {}
}
