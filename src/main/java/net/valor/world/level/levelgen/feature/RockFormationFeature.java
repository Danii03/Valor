package net.valor.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.valor.world.level.levelgen.feature.configurations.RockFormationConfiguration;

/**
 * A small group of old, half-sunken boulders. Counterpart to vanilla's forest rock
 * ({@link net.minecraft.world.level.levelgen.feature.BlockBlobFeature}), but shaped to look weathered and
 * settled instead of dropped onto the grass:
 * <ul>
 *     <li>each boulder is a slightly irregular, flattened ellipsoid whose lower part lies in the ground;</li>
 *     <li>on slopes the downhill side is extended to the ground, so no boulder hangs in the air;</li>
 *     <li>exposed tops use {@link RockFormationConfiguration#topRock()} (mossier) and may carry moss carpets;</li>
 *     <li>a boulder that would touch water, leaves, logs or another feature is left out; if the first,
 *     largest one cannot be placed, nothing is.</li>
 * </ul>
 * Runs before vegetation and trees, so grass and ferns grow around the rocks and trees avoid them.
 */
public class RockFormationFeature extends Feature<RockFormationConfiguration> {
	private static final int MAX_GROUND_SEARCH = 40;
	/** A boulder may be extended this far down on its downhill side; deeper means a drop, and it is left out. */
	private static final int MAX_SUPPORT = 2;
	/** Roughness of a boulder's surface. */
	private static final float ROUGHNESS = 0.35F;

	public RockFormationFeature(Codec<RockFormationConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<RockFormationConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		RockFormationConfiguration config = context.config();

		int originGround = TerrainFitting.findGround(level, origin.getX(), origin.getZ(), origin.getY() + 1, MAX_GROUND_SEARCH);
		if (originGround == TerrainFitting.NO_GROUND) {
			return false;
		}
		if (random.nextFloat() >= config.flatGroundChance()
				&& !TerrainFitting.isNearSlopeOrWater(level, new BlockPos(origin.getX(), originGround, origin.getZ()))) {
			return false;
		}

		BoundingBox writable = TerrainFitting.writableArea(level, origin);
		int boulders = config.boulders().sample(random);
		float firstRadius = config.radius().sample(random);
		Set<BlockPos> rocks = new HashSet<>();
		for (int i = 0; i < boulders; i++) {
			int x = origin.getX();
			int z = origin.getZ();
			float radius = firstRadius;
			if (i > 0) {
				x += Mth.nextInt(random, -config.spread(), config.spread());
				z += Mth.nextInt(random, -config.spread(), config.spread());
				radius = Math.max(0.8F, firstRadius * Mth.nextFloat(random, 0.45F, 0.8F));
			}
			int ground = TerrainFitting.findGround(level, x, z, originGround + 4, 10);
			Set<BlockPos> boulder = ground == TerrainFitting.NO_GROUND ? null : boulder(level, random, x, ground, z, radius, rocks, writable);
			if (boulder == null) {
				if (i == 0) {
					return false;
				}
				continue;
			}
			rocks.addAll(boulder);
		}

		for (BlockPos pos : rocks) {
			boolean exposed = !rocks.contains(pos.above()) && TerrainFitting.isAirOrPlant(level.getBlockState(pos.above()));
			BlockState state = (exposed ? config.topRock() : config.rock()).getState(random, pos);
			level.setBlock(pos, state, Block.UPDATE_CLIENTS);
		}
		for (BlockPos pos : rocks) {
			BlockPos above = pos.above();
			if (!rocks.contains(above) && random.nextFloat() < config.mossChance() && level.getBlockState(above).isAir()) {
				level.setBlock(above, Blocks.MOSS_CARPET.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}

		BoundingBox.encapsulatingPositions(rocks).ifPresent(box ->
				TerrainFitting.removeUnsupportedPlants(level, TerrainFitting.clamp(box.inflatedBy(1, 2, 1), writable)));
		return true;
	}

	/**
	 * Works out one boulder centered on the ground at {@code x, z}: about half above ground, half sunk in.
	 *
	 * @return the boulder's blocks, or {@code null} if it cannot be placed cleanly
	 */
	private static Set<BlockPos> boulder(WorldGenLevel level, RandomSource random, int x, int ground, int z, float radius,
			Set<BlockPos> others, BoundingBox writable) {
		float radiusX = radius * Mth.nextFloat(random, 0.8F, 1.2F);
		float radiusZ = radius * Mth.nextFloat(random, 0.8F, 1.2F);
		float radiusY = radius * Mth.nextFloat(random, 0.6F, 0.85F);
		double centerX = x + 0.5;
		double centerY = ground + 0.5 + radiusY * Mth.nextFloat(random, -0.1F, 0.25F);
		double centerZ = z + 0.5;

		Set<BlockPos> blocks = new HashSet<>();
		int spanX = Mth.ceil(radiusX);
		int spanY = Mth.ceil(radiusY);
		int spanZ = Mth.ceil(radiusZ);
		for (int dx = -spanX; dx <= spanX; dx++) {
			for (int dy = -spanY; dy <= spanY; dy++) {
				for (int dz = -spanZ; dz <= spanZ; dz++) {
					BlockPos pos = new BlockPos(x + dx, Mth.floor(centerY) + dy, z + dz);
					double nx = (pos.getX() + 0.5 - centerX) / radiusX;
					double ny = (pos.getY() + 0.5 - centerY) / radiusY;
					double nz = (pos.getZ() + 0.5 - centerZ) / radiusZ;
					if (nx * nx + ny * ny + nz * nz <= 1.0 + (random.nextFloat() - 0.5F) * ROUGHNESS) {
						blocks.add(pos);
					}
				}
			}
		}
		if (blocks.isEmpty()) {
			return null;
		}

		// Rest on the ground: extend the lowest block of every column down to solid ground.
		Map<Long, Integer> columnBottom = new HashMap<>();
		for (BlockPos pos : blocks) {
			columnBottom.merge(BlockPos.asLong(pos.getX(), 0, pos.getZ()), pos.getY(), Math::min);
		}
		List<BlockPos> support = new ArrayList<>();
		for (Map.Entry<Long, Integer> entry : columnBottom.entrySet()) {
			BlockPos column = BlockPos.of(entry.getKey());
			for (int y = entry.getValue() - 1, depth = 0; ; y--, depth++) {
				BlockPos below = new BlockPos(column.getX(), y, column.getZ());
				BlockState state = level.getBlockState(below);
				if (!TerrainFitting.isAirOrPlant(state) || others.contains(below)) {
					break;
				}
				if (depth >= MAX_SUPPORT) {
					return null; // a drop: the boulder would overhang
				}
				support.add(below);
			}
		}
		blocks.addAll(support);

		// Only into air, plants and soil; never into water, trees or other features.
		Set<BlockPos> result = new HashSet<>();
		for (BlockPos pos : blocks) {
			if (!writable.isInside(pos)) {
				return null;
			}
			if (others.contains(pos)) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (!state.getFluidState().isEmpty() || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
					|| state.is(BlockTags.FEATURES_CANNOT_REPLACE)) {
				return null;
			}
			if (TerrainFitting.isAirOrPlant(state) || TerrainFitting.isSoil(state)) {
				result.add(pos);
			}
			// Anything else (gravel, clay, another rock) stays as it is.
		}
		return result;
	}
}
