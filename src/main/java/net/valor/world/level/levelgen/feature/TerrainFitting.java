package net.valor.world.level.levelgen.feature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.valor.world.level.levelgen.feature.configurations.GroundCover;

/**
 * Shared helpers for features that sit on the terrain (template trees, deadwood, rocks): finding the
 * ground, telling hillsides and stream banks apart from flat ground, and blending the terrain around a
 * placed feature so it looks grown into the ground instead of set down on it.
 * <p>
 * All helpers only ever <i>add</i> soil where there is air or plants, never carve terrain, and never
 * touch fluids, logs or other features.
 */
public final class TerrainFitting {
	/** Returned by {@link #findGround} when there is no usable soil. */
	public static final int NO_GROUND = Integer.MIN_VALUE;
	/** Features may write into the chunk they decorate and its direct neighbors. */
	public static final int WRITE_MARGIN = 16;

	private static final int WATER_SEARCH_RADIUS = 6;
	private static final int SLOPE_SAMPLE_DISTANCE = 4;
	private static final int SLOPE_MIN_HEIGHT_DIFFERENCE = 2;
	private static final int COVER_SEARCH_DEPTH = 16;
	private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

	private TerrainFitting() {}

	/** The area a feature decorating the chunk of {@code origin} may read and write. */
	public static BoundingBox writableArea(WorldGenLevel level, BlockPos origin) {
		ChunkPos chunk = new ChunkPos(origin);
		return new BoundingBox(
				chunk.getMinBlockX() - WRITE_MARGIN, level.getMinBuildHeight(), chunk.getMinBlockZ() - WRITE_MARGIN,
				chunk.getMaxBlockX() + WRITE_MARGIN, level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ() + WRITE_MARGIN);
	}

	/** The part of {@code box} that lies inside {@code limit}. */
	public static BoundingBox clamp(BoundingBox box, BoundingBox limit) {
		return new BoundingBox(
				Math.max(box.minX(), limit.minX()), Math.max(box.minY(), limit.minY()), Math.max(box.minZ(), limit.minZ()),
				Math.min(box.maxX(), limit.maxX()), Math.min(box.maxY(), limit.maxY()), Math.min(box.maxZ(), limit.maxZ()));
	}

	/**
	 * Searches down from {@code fromY} for soil, passing through air, leaves and plants.
	 * Returns {@link #NO_GROUND} if it meets anything else first (water, logs, rocks) or finds nothing.
	 */
	public static int findGround(WorldGenLevel level, int x, int z, int fromY, int depth) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, fromY, z);
		for (int i = 0; i <= depth; i++, pos.move(0, -1, 0)) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.is(BlockTags.LEAVES) || isPlant(state)) {
				continue;
			}
			return isSoil(state) ? pos.getY() : NO_GROUND;
		}
		return NO_GROUND;
	}

	public static boolean isSoil(BlockState state) {
		return Feature.isDirt(state) || state.is(BlockTags.BASE_STONE_OVERWORLD);
	}

	public static boolean isAirOrPlant(BlockState state) {
		return state.isAir() || isPlant(state);
	}

	/** Small ground vegetation a feature may grow through or replace. Fluids never count. */
	public static boolean isPlant(BlockState state) {
		if (!state.getFluidState().isEmpty()) {
			return false;
		}
		return state.canBeReplaced()
				|| state.is(BlockTags.REPLACEABLE_BY_TREES) && !state.is(BlockTags.LEAVES)
				|| state.is(BlockTags.SMALL_FLOWERS)
				|| state.is(Blocks.MOSS_CARPET)
				|| state.is(Blocks.AZALEA)
				|| state.is(Blocks.FLOWERING_AZALEA)
				|| state.is(Blocks.BROWN_MUSHROOM)
				|| state.is(Blocks.RED_MUSHROOM)
				|| state.is(Blocks.SWEET_BERRY_BUSH);
	}

	/** Whether {@code ground} lies on a hillside or within a few blocks of water (a stream bank). */
	public static boolean isNearSlopeOrWater(WorldGenLevel level, BlockPos ground) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -WATER_SEARCH_RADIUS; dx <= WATER_SEARCH_RADIUS; dx++) {
			for (int dz = -WATER_SEARCH_RADIUS; dz <= WATER_SEARCH_RADIUS; dz++) {
				for (int dy = -2; dy <= 1; dy++) {
					if (level.getFluidState(pos.setWithOffset(ground, dx, dy, dz)).is(FluidTags.WATER)) {
						return true;
					}
				}
			}
		}
		for (int[] direction : DIRECTIONS) {
			int x = ground.getX() + direction[0] * SLOPE_SAMPLE_DISTANCE;
			int z = ground.getZ() + direction[1] * SLOPE_SAMPLE_DISTANCE;
			int height = findGround(level, x, z, ground.getY() + 8, 16);
			if (height != NO_GROUND && Math.abs(height - ground.getY()) >= SLOPE_MIN_HEIGHT_DIFFERENCE) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Fills a column with soil from above {@code groundY} up to {@code topY}, like ground that has built
	 * up under and around an old feature. Stops at anything that is not air or a plant. The new top
	 * takes over the old surface block (grass, podzol, moss, …); the covered surface turns to dirt.
	 *
	 * @return the new surface position, or {@code null} if nothing was filled
	 */
	public static BlockPos fillSoil(WorldGenLevel level, int x, int z, int groundY, int topY) {
		if (topY <= groundY) {
			return null;
		}
		BlockPos ground = new BlockPos(x, groundY, z);
		BlockState surface = level.getBlockState(ground);
		BlockState top = surface.is(BlockTags.DIRT) ? surface : Blocks.GRASS_BLOCK.defaultBlockState();
		BlockPos lastFilled = null;
		for (int y = groundY + 1; y <= topY; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!isAirOrPlant(level.getBlockState(pos))) {
				break;
			}
			level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
			lastFilled = pos;
		}
		if (lastFilled == null) {
			return null;
		}
		if (surface.is(Blocks.GRASS_BLOCK) || surface.is(Blocks.PODZOL) || surface.is(Blocks.MYCELIUM) || surface.is(Blocks.MOSS_BLOCK)) {
			level.setBlock(ground, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		level.setBlock(lastFilled, top, Block.UPDATE_CLIENTS);
		return lastFilled;
	}

	/**
	 * Raises the ground around a footprint into a gentle mound, so soil filled under a feature on the low
	 * side of a slope does not stand there as a wall. The target height drops by one block per block of
	 * distance (with a little roughness), and no column is raised by more than {@code maxRaise}.
	 *
	 * @param footprint ground height directly under the feature, per column ({@link BlockPos#asLong} of x/0/z)
	 * @return the new surface positions
	 */
	public static List<BlockPos> raiseSurroundings(WorldGenLevel level, RandomSource random, Map<Long, Integer> footprint,
			int radius, int maxRaise, BoundingBox writable) {
		List<BlockPos> surfaces = new ArrayList<>();
		if (footprint.isEmpty()) {
			return surfaces;
		}
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, top = Integer.MIN_VALUE;
		for (Map.Entry<Long, Integer> entry : footprint.entrySet()) {
			BlockPos column = BlockPos.of(entry.getKey());
			minX = Math.min(minX, column.getX());
			maxX = Math.max(maxX, column.getX());
			minZ = Math.min(minZ, column.getZ());
			maxZ = Math.max(maxZ, column.getZ());
			top = Math.max(top, entry.getValue());
		}
		for (int x = minX - radius; x <= maxX + radius; x++) {
			for (int z = minZ - radius; z <= maxZ + radius; z++) {
				if (footprint.containsKey(BlockPos.asLong(x, 0, z)) || !writable.isInside(x, writable.minY(), z)) {
					continue;
				}
				int target = Integer.MIN_VALUE;
				for (Map.Entry<Long, Integer> entry : footprint.entrySet()) {
					BlockPos column = BlockPos.of(entry.getKey());
					float distance = Mth.sqrt((float) (Mth.square(column.getX() - x) + Mth.square(column.getZ() - z)));
					if (distance <= radius + 0.5F) {
						target = Math.max(target, entry.getValue() - Math.round(distance));
					}
				}
				if (target == Integer.MIN_VALUE) {
					continue;
				}
				if (target < top && random.nextFloat() < 0.3F) {
					target--; // roughness, so the mound is not a perfect cone
				}
				int ground = findGround(level, x, z, top + 2, top + 2 - target + maxRaise + 4);
				if (ground == NO_GROUND || ground >= target) {
					continue;
				}
				BlockPos surface = fillSoil(level, x, z, ground, Math.min(target, ground + maxRaise));
				if (surface != null) {
					surfaces.add(surface);
				}
			}
		}
		return surfaces;
	}

	/**
	 * Scatters {@link GroundCover} blocks over the exposed grass around a footprint, thinning out with
	 * distance: the darker, decayed soil around old wood.
	 */
	public static void coverGround(WorldGenLevel level, RandomSource random, Iterable<Long> footprint, int searchFromY,
			GroundCover cover, BoundingBox writable) {
		int radius = cover.radius();
		Set<Long> columns = new HashSet<>();
		footprint.forEach(columns::add);
		Set<Long> visited = new HashSet<>();
		for (long key : columns) {
			BlockPos column = BlockPos.of(key);
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					int x = column.getX() + dx;
					int z = column.getZ() + dz;
					long target = BlockPos.asLong(x, 0, z);
					if (columns.contains(target) || !visited.add(target) || !writable.isInside(x, writable.minY(), z)) {
						continue;
					}
					float distance = Mth.sqrt((float) nearestDistanceSqr(columns, x, z));
					if (distance > radius + 0.5F || random.nextFloat() >= cover.chance() * (1.0F - distance / (radius + 1.0F))) {
						continue;
					}
					int ground = findGround(level, x, z, searchFromY, COVER_SEARCH_DEPTH);
					if (ground == NO_GROUND) {
						continue;
					}
					BlockPos pos = new BlockPos(x, ground, z);
					if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK) && isAirOrPlant(level.getBlockState(pos.above()))) {
						level.setBlock(pos, cover.state().getState(random, pos), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	private static int nearestDistanceSqr(Iterable<Long> columns, int x, int z) {
		int best = Integer.MAX_VALUE;
		for (long key : columns) {
			BlockPos column = BlockPos.of(key);
			best = Math.min(best, Mth.square(column.getX() - x) + Mth.square(column.getZ() - z));
		}
		return best;
	}

	/** Lets grass and ferns grow on freshly raised soil, for features placed after the undergrowth. */
	public static void revegetate(WorldGenLevel level, RandomSource random, List<BlockPos> surfaces, float chance) {
		for (BlockPos surface : surfaces) {
			BlockPos above = surface.above();
			if (random.nextFloat() < chance && level.getBlockState(surface).is(Blocks.GRASS_BLOCK) && level.getBlockState(above).isAir()) {
				BlockState plant = (random.nextFloat() < 0.35F ? Blocks.FERN : Blocks.SHORT_GRASS).defaultBlockState();
				if (plant.canSurvive(level, above)) {
					level.setBlock(above, plant, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/**
	 * Removes plants in {@code box} that lost their footing: halves of tall grass or large ferns whose other
	 * half was covered, grass standing on a log, and so on. Runs bottom-up so upper halves follow lower ones.
	 */
	public static void removeUnsupportedPlants(WorldGenLevel level, BoundingBox box) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int y = box.minY(); y <= box.maxY(); y++) {
			for (int x = box.minX(); x <= box.maxX(); x++) {
				for (int z = box.minZ(); z <= box.maxZ(); z++) {
					BlockState state = level.getBlockState(pos.set(x, y, z));
					if (isGroundPlant(state) && (!state.canSurvive(level, pos) || isLowerHalfWithoutTop(level, pos, state))) {
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	/** {@code canSurvive} only checks a double plant's upper half against its lower half, not the other way round. */
	private static boolean isLowerHalfWithoutTop(WorldGenLevel level, BlockPos pos, BlockState state) {
		return state.getBlock() instanceof DoublePlantBlock
				&& state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER
				&& !level.getBlockState(pos.above()).is(state.getBlock());
	}

	/** Plants whose survival only depends on the block below (or their other half); no light checks. */
	private static boolean isGroundPlant(BlockState state) {
		if (state.isAir() || !state.getFluidState().isEmpty()) {
			return false;
		}
		return state.getBlock() instanceof DoublePlantBlock
				|| state.is(Blocks.SHORT_GRASS)
				|| state.is(Blocks.FERN)
				|| state.is(Blocks.DEAD_BUSH)
				|| state.is(BlockTags.SMALL_FLOWERS)
				|| state.is(Blocks.MOSS_CARPET)
				|| state.is(Blocks.SWEET_BERRY_BUSH);
	}
}
