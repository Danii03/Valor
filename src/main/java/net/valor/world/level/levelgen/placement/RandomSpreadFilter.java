package net.valor.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.valor.registry.ValorPlacementModifierTypes;

/**
 * Spreads a rare feature out the way vanilla spreads structures
 * ({@link net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement}): the world
 * is divided into cells of {@code spacing × spacing} chunks, and in each cell one randomly chosen chunk may
 * hold the feature. Neighboring candidates are always at least {@code separation} chunks apart, so landmarks
 * never come in clusters, yet they follow no visible grid.
 * <p>
 * Place it first, before any position modifiers. Further filters (zones, terrain checks) thin the
 * candidates out as usual.
 */
public final class RandomSpreadFilter extends PlacementFilter {

	public static final MapCodec<RandomSpreadFilter> CODEC = RecordCodecBuilder.<RandomSpreadFilter>mapCodec(instance -> instance.group(
			Codec.intRange(1, 64).fieldOf("spacing").forGetter(filter -> filter.spacing),
			Codec.intRange(0, 63).fieldOf("separation").forGetter(filter -> filter.separation),
			Codec.INT.fieldOf("salt").forGetter(filter -> filter.salt)
	).apply(instance, RandomSpreadFilter::new)).validate(filter -> filter.separation < filter.spacing
			? DataResult.success(filter)
			: DataResult.error(() -> "separation must be smaller than spacing"));

	private final int spacing;
	private final int separation;
	private final int salt;

	private RandomSpreadFilter(int spacing, int separation, int salt) {
		this.spacing = spacing;
		this.separation = separation;
		this.salt = salt;
	}

	/**
	 * @param spacing    cell size, in chunks
	 * @param separation minimum distance between candidates of neighboring cells, in chunks
	 * @param salt       makes different features pick different chunks
	 */
	public static RandomSpreadFilter of(int spacing, int separation, int salt) {
		if (separation >= spacing) {
			throw new IllegalArgumentException("separation must be smaller than spacing");
		}
		return new RandomSpreadFilter(spacing, separation, salt);
	}

	@Override
	protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
		ChunkPos chunk = new ChunkPos(pos);
		int cellX = Math.floorDiv(chunk.x, this.spacing);
		int cellZ = Math.floorDiv(chunk.z, this.spacing);
		WorldgenRandom cellRandom = new WorldgenRandom(new LegacyRandomSource(0L));
		cellRandom.setLargeFeatureWithSalt(context.getLevel().getSeed(), cellX, cellZ, this.salt);
		int range = this.spacing - this.separation;
		int chosenX = cellX * this.spacing + cellRandom.nextInt(range);
		int chosenZ = cellZ * this.spacing + cellRandom.nextInt(range);
		return chunk.x == chosenX && chunk.z == chosenZ;
	}

	@Override
	public PlacementModifierType<?> type() {
		return ValorPlacementModifierTypes.RANDOM_SPREAD;
	}
}
