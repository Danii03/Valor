package net.valor.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.valor.Valor;
import net.valor.world.level.levelgen.placement.RandomSpreadFilter;
import net.valor.world.level.levelgen.placement.RuinClearanceFilter;
import net.valor.world.level.levelgen.placement.WhisperingWoodsZoneFilter;

/**
 * Placement modifier types added by Valor. Counterpart to vanilla's {@link PlacementModifierType} constants.
 */
public final class ValorPlacementModifierTypes {

	/** Thins out placement attempts per Whispering Woods zone (Outer Woods, Deepwood, Hidden Groves). */
	public static final PlacementModifierType<WhisperingWoodsZoneFilter> WHISPERING_WOODS_ZONE = register("whispering_woods_zone",
			WhisperingWoodsZoneFilter.CODEC);

	/** Lets a rare feature appear in at most one chunk per cell of chunks, like vanilla spreads structures. */
	public static final PlacementModifierType<RandomSpreadFilter> RANDOM_SPREAD = register("random_spread",
			RandomSpreadFilter.CODEC);

	/** Keeps a feature away from Whispering Woods ruins. */
	public static final PlacementModifierType<RuinClearanceFilter> RUIN_CLEARANCE = register("ruin_clearance",
			RuinClearanceFilter.CODEC);

	private ValorPlacementModifierTypes() {}

	private static <P extends PlacementModifier> PlacementModifierType<P> register(String name, MapCodec<P> codec) {
		return Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, Valor.id(name), () -> codec);
	}

	/** Forces class loading so all static fields get registered. */
	public static void init() {}
}
