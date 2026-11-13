package net.valor.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.valor.world.level.levelgen.feature.configurations.StreamConfiguration;
import net.valor.world.level.levelgen.structure.structures.RuinReservations;

/**
 * A small forest stream that winds along the terrain, with banks that slope down to the water in
 * irregular one-block steps instead of standing as earthen walls.
 * <p>
 * Minecraft has no flowing-river simulation in worldgen, so like vanilla lakes the water is level along
 * the whole stream. That is why the stream follows the contour of the ground: at each step it turns
 * towards the side whose ground lies closest to the water, so it bends around hills and along hollows,
 * and it ends (narrowing to a trickle) where the ground rises too high, drops away or meets other water.
 * <ul>
 *     <li><b>Width</b> changes smoothly along the course: narrow runs of 1–2 blocks, mostly 3–4,
 *     occasionally a pool of 5–6. Wide parts are two blocks deep in the middle.</li>
 *     <li><b>Banks:</b> within {@link StreamConfiguration#bankWidth()} blocks of the water, the ground is
 *     lowered (never raised, apart from closing a dip next to the water) to a slope of about one block per
 *     one and a half blocks of distance, with some roughness. The lowered surface keeps its grass; right at
 *     the water a sparse mix of dirt, coarse dirt, gravel, a little stone and clay shows through.</li>
 *     <li><b>Safety:</b> nothing is changed under or right next to logs, rocks, leaves lying on the ground or
 *     other features, nor near ruins. Every water block has a solid bed and solid or water sides, so nothing
 *     leaks or floats. Where a bank would end in a cliff of more than {@link #MAX_OUTER_CUT} blocks, no
 *     stream is placed.</li>
 * </ul>
 * Everything happens inside the area a feature may write to (the chunk and its direct neighbors), and runs
 * in the {@code LAKES} step, before vegetation and trees of the chunk, so those treat it like any water.
 */
public class StreamFeature extends Feature<StreamConfiguration> {
	/** Features may write into the chunk they decorate and 16 blocks around it. */
	private static final int EDGE = TerrainFitting.WRITE_MARGIN;
	private static final int AREA = 16 + 2 * EDGE;
	/** Half of the widest pool. */
	private static final float MAX_HALF_WIDTH = 3.0F;
	private static final float MIN_WIDTH = 1.0F;
	private static final float MAX_WIDTH = 6.0F;
	private static final float POOL_WIDTH = 6.0F;
	private static final int MIN_COURSE = 10;
	/** Steps over which the stream narrows at its ends. */
	private static final int TAPER = 5;
	private static final float MAX_TURN = 0.35F;
	private static final float STEER = 0.08F;
	private static final float LOOKAHEAD = 4.0F;
	/** Ground height rise per block of distance from the water. */
	private static final float BANK_SLOPE = 0.65F;
	/** At the outer edge of a bank, the ground may be cut by this much at most. */
	private static final int MAX_OUTER_CUT = 2;
	/** A dip right next to the water may be filled up by this much. */
	private static final int MAX_FILL = 2;
	private static final int GROUND_SEARCH_ABOVE = 8;
	private static final int GROUND_SEARCH_DEPTH = 32;

	private static final int NOT_SAMPLED = Integer.MIN_VALUE;
	/** Logs, rocks, other features, fluids: never touched, and nothing is changed right next to them. */
	private static final int UNUSABLE = Integer.MIN_VALUE + 1;

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
		// Covers the whole writable area: banks never cut into a ruin's blended ground.
		if (RuinReservations.isNearRuin(level, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), EDGE + 8)) {
			return false;
		}
		Area area = new Area(level, chunk.getMinBlockX() - EDGE, chunk.getMinBlockZ() - EDGE);
		int bankWidth = config.bankWidth();

		// ---- Course along the contour of the ground.
		List<float[]> course = this.planCourse(area, random, config, origin);
		if (course == null) {
			return false;
		}
		int waterY = (int) course.get(0)[3];

		// ---- Water cells: discs along the course, sampled every half block.
		float[] edgeDistance = new float[AREA * AREA];
		Arrays.fill(edgeDistance, Float.MAX_VALUE);
		for (int i = 0; i < course.size(); i++) {
			float[] point = course.get(i);
			float[] next = course.get(Math.min(i + 1, course.size() - 1));
			for (float t = 0.0F; t < 1.0F; t += 0.5F) {
				float x = Mth.lerp(t, point[0], next[0]);
				float z = Mth.lerp(t, point[1], next[1]);
				float half = Mth.lerp(t, point[2], next[2]);
				markDisc(edgeDistance, x, z, half);
			}
		}
		boolean[] water = new boolean[AREA * AREA];
		int minX = AREA, maxX = -1, minZ = AREA, maxZ = -1;
		for (int lx = 0; lx < AREA; lx++) {
			for (int lz = 0; lz < AREA; lz++) {
				if (edgeDistance[index(lx, lz)] <= 0.0F) {
					int ground = area.ground(lx, lz);
					if (!area.canHoldWater(lx, lz, waterY, config.maxBankHeight())) {
						return false;
					}
					water[index(lx, lz)] = true;
					minX = Math.min(minX, lx);
					maxX = Math.max(maxX, lx);
					minZ = Math.min(minZ, lz);
					maxZ = Math.max(maxZ, lz);
				}
			}
		}
		int[] depth = new int[AREA * AREA];
		for (int lx = minX; lx <= maxX; lx++) {
			for (int lz = minZ; lz <= maxZ; lz++) {
				int i = index(lx, lz);
				if (!water[i]) {
					continue;
				}
				// The middle of wide parts is two blocks deep, if the ground below holds.
				boolean deep = edgeDistance[i] <= -1.3F && surroundedByWater(water, lx, lz)
						&& isWatertight(level.getBlockState(area.pos(lx, waterY - 2, lz)));
				depth[i] = deep ? 2 : 1;
				if (!deep && !holdsWater(level.getBlockState(area.pos(lx, waterY - 1, lz)))) {
					return false;
				}
			}
		}

		// ---- Banks: distance of each land column from the water, and the height it is lowered to.
		int[] newTop = new int[AREA * AREA];
		float[] bankDistance = new float[AREA * AREA];
		Arrays.fill(newTop, NOT_SAMPLED);
		for (int lx = Math.max(0, minX - bankWidth); lx <= Math.min(AREA - 1, maxX + bankWidth); lx++) {
			for (int lz = Math.max(0, minZ - bankWidth); lz <= Math.min(AREA - 1, maxZ + bankWidth); lz++) {
				int i = index(lx, lz);
				if (water[i]) {
					continue;
				}
				float distance = distanceToWater(water, lx, lz, bankWidth, minX, maxX, minZ, maxZ);
				if (distance > bankWidth + 0.5F) {
					continue;
				}
				boolean touchesWater = isOrthogonallyNextToWater(water, lx, lz);
				int ground = area.ground(lx, lz);
				if (ground == UNUSABLE || area.isObstacle(lx, lz)) {
					// Trunks, rocks and fluids stay as they are; next to the water they must hold it.
					if (touchesWater && (ground == UNUSABLE || !holdsWater(level.getBlockState(area.pos(lx, waterY, lz))))) {
						return false;
					}
					continue;
				}
				int target = waterY + Mth.floor(distance * BANK_SLOPE + 0.6F * roughness(area.baseX + lx, area.baseZ + lz));
				// Keep the ground under tree roots and rocks: at most one block lower right beside them.
				target = Math.max(target, area.obstacleGroundAround(lx, lz) - 1);
				int top = Math.min(ground, target);
				if (ground - top > MAX_OUTER_CUT && distance > bankWidth - 1.0F) {
					return false; // the bank would end in a cliff
				}
				if (touchesWater && top < waterY) {
					if (waterY - top > MAX_FILL) {
						return false;
					}
					top = waterY; // close a dip next to the water
				}
				if (touchesWater && top > waterY && !holdsWater(level.getBlockState(area.pos(lx, waterY, lz)))) {
					return false; // a cave or a gap right beside the water
				}
				newTop[i] = top;
				bankDistance[i] = distance;
			}
		}

		// ---- Build: water, then banks.
		for (int lx = minX; lx <= maxX; lx++) {
			for (int lz = minZ; lz <= maxZ; lz++) {
				int i = index(lx, lz);
				if (water[i]) {
					this.carveWater(level, random, area, lx, lz, area.ground(lx, lz), waterY, depth[i]);
				}
			}
		}
		for (int lx = 0; lx < AREA; lx++) {
			for (int lz = 0; lz < AREA; lz++) {
				int i = index(lx, lz);
				if (newTop[i] != NOT_SAMPLED) {
					this.shapeBank(level, random, area, lx, lz, area.ground(lx, lz), newTop[i], bankDistance[i]);
				}
			}
		}
		return true;
	}

	/**
	 * The course as points of x, z (local), half width and water height. Returns {@code null} if the
	 * stream would be too short.
	 */
	private List<float[]> planCourse(Area area, RandomSource random, StreamConfiguration config, BlockPos origin) {
		int pad = Mth.ceil(MAX_HALF_WIDTH) + config.bankWidth() + 1;
		float low = pad;
		float high = AREA - pad;
		float x = Mth.clamp(origin.getX() - area.baseX + 0.5F, low, high - 0.01F);
		float z = Mth.clamp(origin.getZ() - area.baseZ + 0.5F, low, high - 0.01F);
		// The water lies one block below the lowest ground around the start; the course then follows that level.
		int lowest = Integer.MAX_VALUE;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				int ground = area.ground(Mth.floor(x) + dx, Mth.floor(z) + dz);
				if (ground == UNUSABLE || area.isObstacle(Mth.floor(x) + dx, Mth.floor(z) + dz)) {
					return null;
				}
				lowest = Math.min(lowest, ground);
			}
		}
		int waterY = lowest - 1;

		int length = config.length().sample(random);
		float angle = random.nextFloat() * Mth.TWO_PI;
		float turn = 0.0F;
		float phase1 = random.nextFloat() * Mth.TWO_PI;
		float phase2 = random.nextFloat() * Mth.TWO_PI;
		int pool = random.nextFloat() < config.wideChance() ? Mth.nextInt(random, MIN_COURSE / 2, Math.max(MIN_COURSE / 2, length - TAPER)) : -1;

		List<float[]> course = new ArrayList<>();
		for (int step = 0; step < length; step++) {
			if (x < low || x >= high || z < low || z >= high) {
				break;
			}
			// Smooth width: two slow waves, plus at most one pool.
			float wave = 0.6F * Mth.sin(step * 0.23F + phase1) + 0.4F * Mth.sin(step * 0.09F + phase2);
			float width = 3.5F + 1.8F * wave;
			if (pool >= 0) {
				width = Math.max(width, POOL_WIDTH - Math.abs(step - pool) * 0.7F);
			}
			float half = Mth.clamp(width, MIN_WIDTH, MAX_WIDTH) / 2.0F;
			if (!this.fits(area, x, z, half, waterY, config.maxBankHeight())) {
				break;
			}
			course.add(new float[] {x, z, half, waterY});

			// Turn towards the side whose ground lies closest to the water, plus a gentle random meander.
			float left = levelError(area, x + Mth.cos(angle - 0.6F) * LOOKAHEAD, z + Mth.sin(angle - 0.6F) * LOOKAHEAD, waterY);
			float right = levelError(area, x + Mth.cos(angle + 0.6F) * LOOKAHEAD, z + Mth.sin(angle + 0.6F) * LOOKAHEAD, waterY);
			float steer = left < right ? -STEER : right < left ? STEER : 0.0F;
			turn = Mth.clamp(turn * 0.8F + (random.nextFloat() - 0.5F) * 0.3F + steer, -MAX_TURN, MAX_TURN);
			angle += turn;
			x += Mth.cos(angle);
			z += Mth.sin(angle);
		}
		if (course.size() < MIN_COURSE) {
			return null;
		}
		// Both ends narrow to a trickle instead of stopping abruptly.
		for (int k = 0; k < TAPER; k++) {
			float factor = (k + 1.0F) / (TAPER + 1.0F);
			float[] first = course.get(k);
			float[] last = course.get(course.size() - 1 - k);
			first[2] = Math.max(MIN_WIDTH / 2.0F, Math.min(first[2], first[2] * factor + 0.25F));
			last[2] = Math.max(MIN_WIDTH / 2.0F, Math.min(last[2], last[2] * factor + 0.25F));
		}
		return course;
	}

	/** Whether every column of the disc is usable ground the water can lie in. */
	private boolean fits(Area area, float x, float z, float half, int waterY, int maxBankHeight) {
		int radius = Mth.ceil(half);
		for (int lx = Mth.floor(x) - radius; lx <= Mth.floor(x) + radius; lx++) {
			for (int lz = Mth.floor(z) - radius; lz <= Mth.floor(z) + radius; lz++) {
				if (!inDisc(lx, lz, x, z, half)) {
					continue;
				}
				if (!area.canHoldWater(lx, lz, waterY, maxBankHeight)) {
					return false;
				}
			}
		}
		return true;
	}

	/** How far the ground at a point lies from the ideal bank height (one block above the water). */
	private static float levelError(Area area, float x, float z, int waterY) {
		int ground = area.ground(Mth.floor(x), Mth.floor(z));
		return ground == UNUSABLE || area.isObstacle(Mth.floor(x), Mth.floor(z)) ? 100.0F : Math.abs(ground - (waterY + 1));
	}

	private static boolean inDisc(int lx, int lz, float x, float z, float half) {
		return Mth.floor(x) == lx && Mth.floor(z) == lz || Mth.square(lx + 0.5F - x) + Mth.square(lz + 0.5F - z) <= half * half;
	}

	/** Records, per column, how far inside the water's edge it lies (negative = water). */
	private static void markDisc(float[] edgeDistance, float x, float z, float half) {
		int radius = Mth.ceil(half);
		for (int lx = Mth.floor(x) - radius; lx <= Mth.floor(x) + radius; lx++) {
			for (int lz = Mth.floor(z) - radius; lz <= Mth.floor(z) + radius; lz++) {
				if (lx < 0 || lz < 0 || lx >= AREA || lz >= AREA) {
					continue;
				}
				float distance = Mth.sqrt(Mth.square(lx + 0.5F - x) + Mth.square(lz + 0.5F - z)) - half;
				if (Mth.floor(x) == lx && Mth.floor(z) == lz) {
					distance = Math.min(distance, 0.0F);
				}
				int i = index(lx, lz);
				edgeDistance[i] = Math.min(edgeDistance[i], distance);
			}
		}
	}

	private static float distanceToWater(boolean[] water, int lx, int lz, int bankWidth, int minX, int maxX, int minZ, int maxZ) {
		float best = Float.MAX_VALUE;
		for (int wx = Math.max(minX, lx - bankWidth - 1); wx <= Math.min(maxX, lx + bankWidth + 1); wx++) {
			for (int wz = Math.max(minZ, lz - bankWidth - 1); wz <= Math.min(maxZ, lz + bankWidth + 1); wz++) {
				if (water[index(wx, wz)]) {
					best = Math.min(best, Mth.square(wx - lx) + Mth.square(wz - lz));
				}
			}
		}
		return best == Float.MAX_VALUE ? best : Mth.sqrt(best);
	}

	private static boolean surroundedByWater(boolean[] water, int lx, int lz) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (!water[index(lx + dx, lz + dz)]) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isOrthogonallyNextToWater(boolean[] water, int lx, int lz) {
		return lx > 0 && water[index(lx - 1, lz)] || lx < AREA - 1 && water[index(lx + 1, lz)]
				|| lz > 0 && water[index(lx, lz - 1)] || lz < AREA - 1 && water[index(lx, lz + 1)];
	}

	/** Replaces the column above the bed with water, removing the plants on it. */
	private void carveWater(WorldGenLevel level, RandomSource random, Area area, int lx, int lz, int ground, int waterY, int depth) {
		clearPlantsAbove(level, area.pos(lx, ground, lz));
		for (int y = ground; y > waterY; y--) {
			level.setBlock(area.pos(lx, y, lz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		for (int y = waterY; y > waterY - depth; y--) {
			level.setBlock(area.pos(lx, y, lz), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		BlockPos bed = area.pos(lx, waterY - depth, lz);
		if (level.getBlockState(bed).is(BlockTags.DIRT)) {
			float roll = random.nextFloat();
			BlockState bedState = roll < 0.45F ? Blocks.GRAVEL.defaultBlockState()
					: roll < 0.55F ? Blocks.CLAY.defaultBlockState()
					: Blocks.DIRT.defaultBlockState();
			level.setBlock(bed, bedState, Block.UPDATE_CLIENTS);
		}
	}

	/** Lowers (or, right at the water, fills up) a bank column to {@code top} and gives it its surface. */
	private void shapeBank(WorldGenLevel level, RandomSource random, Area area, int lx, int lz, int ground, int top, float distance) {
		BlockPos groundPos = area.pos(lx, ground, lz);
		BlockState surface = level.getBlockState(groundPos);
		if (top == ground && !surface.is(BlockTags.DIRT)) {
			return; // untouched stone or gravel
		}
		if (top < ground) {
			clearPlantsAbove(level, groundPos);
			for (int y = ground; y > top; y--) {
				level.setBlock(area.pos(lx, y, lz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		} else if (top > ground) {
			clearPlantsAbove(level, groundPos);
			for (int y = ground; y < top; y++) {
				level.setBlock(area.pos(lx, y, lz), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		BlockPos topPos = area.pos(lx, top, lz);
		BlockState exposed = level.getBlockState(topPos);
		if (top != ground && !exposed.is(BlockTags.DIRT) && !exposed.isAir()) {
			return; // stone or gravel came to light: keep it, a natural rocky bank
		}
		// The forest floor keeps its own surface; only right at the water some bare ground shows through.
		BlockState newSurface = bankSurface(random, distance, surface.is(BlockTags.DIRT) ? surface : Blocks.GRASS_BLOCK.defaultBlockState());
		if (top != ground || newSurface != surface) {
			level.setBlock(topPos, newSurface, Block.UPDATE_CLIENTS);
			if (!newSurface.is(BlockTags.DIRT)) {
				clearPlantsAbove(level, topPos);
			}
		}
		if (top != ground && newSurface.is(Blocks.GRASS_BLOCK) && distance <= 3.5F) {
			growBankPlant(level, random, topPos.above());
		}
	}

	/** A sparse mix right at the water, mostly the forest's own surface further away. */
	private static BlockState bankSurface(RandomSource random, float distance, BlockState forestFloor) {
		float roll = random.nextFloat();
		if (distance <= 1.5F) {
			if (roll < 0.10F) return Blocks.DIRT.defaultBlockState();
			if (roll < 0.20F) return Blocks.COARSE_DIRT.defaultBlockState();
			if (roll < 0.30F) return Blocks.GRAVEL.defaultBlockState();
			if (roll < 0.33F) return Blocks.CLAY.defaultBlockState();
			if (roll < 0.36F) return Blocks.STONE.defaultBlockState();
			return Blocks.GRASS_BLOCK.defaultBlockState();
		}
		if (distance <= 2.5F) {
			if (roll < 0.08F) return Blocks.COARSE_DIRT.defaultBlockState();
			if (roll < 0.14F) return Blocks.DIRT.defaultBlockState();
			if (roll < 0.18F) return Blocks.GRAVEL.defaultBlockState();
			return Blocks.GRASS_BLOCK.defaultBlockState();
		}
		return forestFloor;
	}

	/** Ferns, large ferns and tall grass on a freshly shaped bank (the forest's vegetation adds more later). */
	private static void growBankPlant(WorldGenLevel level, RandomSource random, BlockPos pos) {
		if (!level.isEmptyBlock(pos)) {
			return;
		}
		float roll = random.nextFloat();
		if (roll < 0.10F) {
			level.setBlock(pos, Blocks.FERN.defaultBlockState(), Block.UPDATE_CLIENTS);
		} else if (roll < 0.19F) {
			Block plant = roll < 0.15F ? Blocks.LARGE_FERN : Blocks.TALL_GRASS;
			if (level.isEmptyBlock(pos.above())) {
				DoublePlantBlock.placeAt(level, plant.defaultBlockState(), pos, Block.UPDATE_CLIENTS);
			}
		}
	}

	/** Removes plants (both halves of tall ones) standing on {@code ground}. */
	private static void clearPlantsAbove(WorldGenLevel level, BlockPos ground) {
		BlockPos.MutableBlockPos pos = ground.mutable();
		for (int i = 0; i < 2; i++) {
			pos.move(0, 1, 0);
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || !TerrainFitting.isPlant(state)) {
				return;
			}
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Small positional roughness in [0, 1), the same whichever chunk asks. */
	private static float roughness(int x, int z) {
		return ((Mth.getSeed(x, 0, z) >>> 20) & 0xFFL) / 256.0F;
	}

	private static boolean holdsWater(BlockState state) {
		return isWatertight(state) || state.getFluidState().isSource();
	}

	private static boolean isWatertight(BlockState state) {
		return state.getFluidState().isEmpty() && !state.isAir() && !state.canBeReplaced() && !state.is(BlockTags.LEAVES);
	}

	private static int index(int lx, int lz) {
		return lx * AREA + lz;
	}

	/** The writable area around the chunk, with the ground height of each column sampled once. */
	private static final class Area {
		final WorldGenLevel level;
		final int baseX;
		final int baseZ;
		private final int[] ground = new int[AREA * AREA];
		private final boolean[] obstacle = new boolean[AREA * AREA];

		Area(WorldGenLevel level, int baseX, int baseZ) {
			this.level = level;
			this.baseX = baseX;
			this.baseZ = baseZ;
			Arrays.fill(this.ground, NOT_SAMPLED);
		}

		BlockPos pos(int lx, int y, int lz) {
			return new BlockPos(this.baseX + lx, y, this.baseZ + lz);
		}

		/** Height of the top natural ground block, or {@link #UNUSABLE}. */
		int ground(int lx, int lz) {
			if (lx < 0 || lz < 0 || lx >= AREA || lz >= AREA) {
				return UNUSABLE;
			}
			int i = index(lx, lz);
			if (this.ground[i] == NOT_SAMPLED) {
				this.ground[i] = this.sampleGround(i, this.baseX + lx, this.baseZ + lz);
			}
			return this.ground[i];
		}

		/** Whether a log, rock, leaves or another feature stands on this column's ground. */
		boolean isObstacle(int lx, int lz) {
			this.ground(lx, lz);
			return lx >= 0 && lz >= 0 && lx < AREA && lz < AREA && this.obstacle[index(lx, lz)];
		}

		/**
		 * The highest ground under an obstacle on this or a neighboring column ({@link Integer#MIN_VALUE} if
		 * there is none, {@link Integer#MAX_VALUE} next to fluids or columns without ground).
		 */
		int obstacleGroundAround(int lx, int lz) {
			int highest = Integer.MIN_VALUE;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					int ground = this.ground(lx + dx, lz + dz);
					if (ground == UNUSABLE) {
						return Integer.MAX_VALUE;
					}
					if (this.isObstacle(lx + dx, lz + dz)) {
						highest = Math.max(highest, ground);
					}
				}
			}
			return highest;
		}

		/**
		 * Whether this column can become water at {@code waterY}: free natural ground, not lower than the water
		 * and not too high above it, and no tree or rock beside it standing more than a block above the water
		 * (which would leave it on a pillar).
		 */
		boolean canHoldWater(int lx, int lz, int waterY, int maxBankHeight) {
			int ground = this.ground(lx, lz);
			return ground != UNUSABLE && !this.isObstacle(lx, lz) && ground >= waterY && ground - waterY <= maxBankHeight
					&& this.obstacleGroundAround(lx, lz) <= waterY + 1;
		}

		/**
		 * Height of the top natural ground block, or {@link #UNUSABLE} under fluids or without ground. Anything
		 * else standing on the ground (logs, rocks, leaves lying on it) marks the column as an obstacle.
		 */
		private int sampleGround(int i, int x, int z) {
			// Start a little above the generated surface: neighboring chunks may already have raised soil around a tree.
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x,
					this.level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + GROUND_SEARCH_ABOVE, z);
			BlockState above = Blocks.AIR.defaultBlockState();
			for (int step = 0; step < GROUND_SEARCH_DEPTH; step++, pos.move(0, -1, 0)) {
				BlockState state = this.level.getBlockState(pos);
				if (!state.getFluidState().isEmpty()) {
					return UNUSABLE;
				}
				if (state.isAir() || TerrainFitting.isPlant(state)) {
					above = state;
					continue;
				}
				if (isGround(state)) {
					if (above.is(BlockTags.LEAVES)) {
						this.obstacle[i] = true; // leaves lying on the ground: a bush or a low crown
					}
					return pos.getY();
				}
				// Leaves higher up are crowns; anything else (logs, rocks, other features) is in the way.
				if (!state.is(BlockTags.LEAVES)) {
					this.obstacle[i] = true;
				}
				above = state;
			}
			return UNUSABLE;
		}

		private static boolean isGround(BlockState state) {
			return TerrainFitting.isSoil(state) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(BlockTags.SAND);
		}
	}
}
