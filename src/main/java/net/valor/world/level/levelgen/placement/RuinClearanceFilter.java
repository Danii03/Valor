package net.valor.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.valor.registry.ValorPlacementModifierTypes;
import net.valor.world.level.levelgen.structure.structures.RuinReservations;

/**
 * Skips a placement attempt within {@code margin} blocks of a ruin, so trees, deadwood, rocks and streams
 * never grow into one. The margin should cover how far the feature reaches from its origin; a crown may still
 * spread over a ruin's wall, like in an old forest.
 */
public final class RuinClearanceFilter extends PlacementFilter {

	public static final MapCodec<RuinClearanceFilter> CODEC = Codec.intRange(0, 24).fieldOf("margin")
			.xmap(RuinClearanceFilter::new, filter -> filter.margin);

	private final int margin;

	private RuinClearanceFilter(int margin) {
		this.margin = margin;
	}

	public static RuinClearanceFilter of(int margin) {
		return new RuinClearanceFilter(margin);
	}

	@Override
	protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
		return !RuinReservations.isNearRuin(context.getLevel(), pos.getX(), pos.getZ(), this.margin);
	}

	@Override
	public PlacementModifierType<?> type() {
		return ValorPlacementModifierTypes.RUIN_CLEARANCE;
	}
}
