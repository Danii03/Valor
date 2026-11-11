package net.valor.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.valor.world.level.levelgen.feature.configurations.StreamConfiguration;

/**
 * A small, shallow forest stream: a meandering one-to-two block wide channel of still water, sunk
 * one block below the lowest ground along its course.
 * <p>
 * Minecraft has no flowing-river simulation in worldgen, so like vanilla lakes the water is level
 * along the whole stream. It is only placed where the ground is gentle enough
 * ({@link StreamConfiguration#maxBankHeight()}) and where every side and the bed are solid, so
 * the water never leaks or floats. Runs in the {@code LAKES} step, before vegetation and trees,
 * so sugar cane, clay and gravel disks and trees treat it like any other water.
 */
public class StreamFeature extends Feature<StreamConfiguration> {
	/** Keeps the stream (plus its banks) inside the area a feature may write to. */
	private static final int MARGIN = 14;
	private static final float MAX_TURN = 0.35F;
	private static final float GRAVEL_BED_CHANCE = 0.4F;

	public StreamFeature(Codec<StreamConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<StreamConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		StreamConfiguration config = context.config();

		ChunkPos chunk = new ChunkPos(origin);
		int minX = chunk.getMinBlockX() - MARGIN;
		int maxX = chunk.getMaxBlockX() + MARGIN;
		int minZ = chunk.getMinBlockZ() - MARGIN;
		int maxZ = chunk.getMaxBlockZ() + MARGIN;

		// Meandering course.
		Set<BlockPos> cells = new LinkedHashSet<>();
		double x = origin.getX() + 0.5;
		double z = origin.getZ() + 0.5;
		float angle = random.nextFloat() * Mth.TWO_PI;
		float turn = 0.0F;
		int length = config.length().sample(random);
		for (int i = 0; i < length; i++) {
			if (x < minX || x > maxX + 1 || z < minZ || z > maxZ + 1) {
				break;
			}
			cells.add(new BlockPos(Mth.floor(x), 0, Mth.floor(z)));
			if (random.nextFloat() < config.wideChance()) {
				int sideX = Mth.floor(x - Mth.sin(angle));
				int sideZ = Mth.floor(z + Mth.cos(angle));
				if (sideX >= minX && sideX <= maxX && sideZ >= minZ && sideZ <= maxZ) {
					cells.add(new BlockPos(sideX, 0, sideZ));
				}
			}
			turn = Mth.clamp(turn + (random.nextFloat() - 0.5F) * 0.25F, -MAX_TURN, MAX_TURN);
			angle += turn;
			x += Mth.cos(angle);
			z += Mth.sin(angle);
		}
		if (cells.size() < 4) {
			return false;
		}

		// Ground along the course; the water surface sits one block below its lowest point.
		int lowest = Integer.MAX_VALUE;
		int highest = Integer.MIN_VALUE;
		for (BlockPos cell : cells) {
			int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cell.getX(), cell.getZ()) - 1;
			BlockPos groundPos = new BlockPos(cell.getX(), ground, cell.getZ());
			if (!isSoil(level.getBlockState(groundPos)) || !level.getFluidState(groundPos.above()).isEmpty()) {
				return false;
			}
			lowest = Math.min(lowest, ground);
			highest = Math.max(highest, ground);
		}
		int waterY = lowest - 1;
		if (highest - waterY > config.maxBankHeight()) {
			return false;
		}

		// Bed and every side next to the water must hold it.
		for (BlockPos cell : cells) {
			if (!isWatertight(level.getBlockState(new BlockPos(cell.getX(), waterY - 1, cell.getZ())))) {
				return false;
			}
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				BlockPos side = new BlockPos(cell.getX(), 0, cell.getZ()).relative(direction);
				if (cells.contains(side)) {
					continue;
				}
				BlockPos sidePos = side.atY(waterY);
				BlockState sideState = level.getBlockState(sidePos);
				if (!isWatertight(sideState) && !level.getFluidState(sidePos).isSource()) {
					return false;
				}
			}
		}

		for (BlockPos cell : cells) {
			int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cell.getX(), cell.getZ()) - 1;
			for (int y = ground; y > waterY; y--) {
				level.setBlock(cell.atY(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
			level.setBlock(cell.atY(waterY), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);

			BlockPos bed = cell.atY(waterY - 1);
			BlockState bedState = level.getBlockState(bed);
			if (random.nextFloat() < GRAVEL_BED_CHANCE) {
				level.setBlock(bed, Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_CLIENTS);
			} else if (bedState.is(Blocks.GRASS_BLOCK) || bedState.is(Blocks.PODZOL) || bedState.is(Blocks.MYCELIUM)) {
				level.setBlock(bed, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		return true;
	}

	private static boolean isSoil(BlockState state) {
		return Feature.isDirt(state) || state.is(BlockTags.BASE_STONE_OVERWORLD);
	}

	private static boolean isWatertight(BlockState state) {
		return state.getFluidState().isEmpty() && !state.isAir() && !state.canBeReplaced() && !state.is(BlockTags.LEAVES);
	}
}
