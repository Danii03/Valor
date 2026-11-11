package net.valor.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.valor.registry.ValorPlacementModifierTypes;
import net.valor.world.level.levelgen.WhisperingWoodsZones;

/**
 * Keeps a placement attempt with a chance that depends on the Whispering Woods zone at its position
 * (see {@link WhisperingWoodsZones}). Zones blend smoothly, so the chance does too.
 * <p>
 * Place it after the heightmap modifier, so the zone is sampled at the surface.
 */
public final class WhisperingWoodsZoneFilter extends PlacementFilter {

	public static final MapCodec<WhisperingWoodsZoneFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.floatRange(0.0F, 1.0F).fieldOf("outer_woods").forGetter(filter -> filter.outerWoods),
			Codec.floatRange(0.0F, 1.0F).fieldOf("deepwood").forGetter(filter -> filter.deepwood),
			Codec.floatRange(0.0F, 1.0F).fieldOf("hidden_grove").forGetter(filter -> filter.hiddenGrove)
	).apply(instance, WhisperingWoodsZoneFilter::new));

	private final float outerWoods;
	private final float deepwood;
	private final float hiddenGrove;

	private WhisperingWoodsZoneFilter(float outerWoods, float deepwood, float hiddenGrove) {
		this.outerWoods = outerWoods;
		this.deepwood = deepwood;
		this.hiddenGrove = hiddenGrove;
	}

	/**
	 * @param outerWoods  chance in the Outer Woods
	 * @param deepwood    chance in the Deepwood
	 * @param hiddenGrove chance in the middle of a Hidden Grove
	 */
	public static WhisperingWoodsZoneFilter of(float outerWoods, float deepwood, float hiddenGrove) {
		return new WhisperingWoodsZoneFilter(outerWoods, deepwood, hiddenGrove);
	}

	@Override
	protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
		WhisperingWoodsZones.Zone zone = WhisperingWoodsZones.sample(context.getLevel(), pos);
		return random.nextFloat() < zone.blend(this.outerWoods, this.deepwood, this.hiddenGrove);
	}

	@Override
	public PlacementModifierType<?> type() {
		return ValorPlacementModifierTypes.WHISPERING_WOODS_ZONE;
	}
}
