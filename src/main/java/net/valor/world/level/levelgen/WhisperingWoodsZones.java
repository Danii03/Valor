package net.valor.world.level.levelgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.valor.world.level.biome.ValorBiomes;

/**
 * The three zones inside the Whispering Woods, as smooth weights instead of hard areas:
 * <ul>
 *     <li><b>Outer Woods</b>: everything that is not Deepwood or a grove; strongest near the biome edge.</li>
 *     <li><b>Deepwood</b>: the heart of the forest. Grows with the distance to the biome edge, and a
 *     large-scale noise makes it swell and recede, so it is never a ring parallel to the border.</li>
 *     <li><b>Hidden Groves</b>: small clearings where a fine noise peaks, only away from the edge.</li>
 * </ul>
 * Features ask for the zone at their position through
 * {@link net.valor.world.level.levelgen.placement.WhisperingWoodsZoneFilter}. The zones are not biomes:
 * nothing is registered, they only change how often each feature is placed.
 */
public final class WhisperingWoodsZones {
	/** Distances (blocks) at which the biome is probed around a chunk to find out how deep inside it is. */
	private static final int[] EDGE_PROBE_DISTANCES = {24, 56};
	private static final int EDGE_PROBE_DIRECTIONS = 8;

	/** Size of the Deepwood swells, in blocks. */
	private static final double DEEPWOOD_NOISE_SCALE = 180.0;
	/** Size of a grove, in blocks. */
	private static final double GROVE_NOISE_SCALE = 34.0;
	private static final float GROVE_THRESHOLD_START = 0.5F;
	private static final float GROVE_THRESHOLD_FULL = 0.68F;

	private static final long DEEPWOOD_SALT = 0x5EED_DEE9_700DL;
	private static final long GROVE_SALT = 0x6E0F_E5C1_EA21L;

	private static volatile Noises noises = new Noises(0L);
	/** Edge probing is per chunk; all features of a chunk run on one thread, so one cached chunk per thread is enough. */
	private static final ThreadLocal<InteriorCache> INTERIOR_CACHE = ThreadLocal.withInitial(InteriorCache::new);

	private WhisperingWoodsZones() {}

	/**
	 * Zone weights at {@code pos}.
	 *
	 * @param deepwood 0 = Outer Woods, 1 = Deepwood
	 * @param grove    0 = no grove, 1 = center of a Hidden Grove (overrides the other two)
	 */
	public record Zone(float deepwood, float grove) {
		/** Blends one value per zone by this zone's weights. */
		public float blend(float outerWoods, float deepwood, float hiddenGrove) {
			float forest = Mth.lerp(this.deepwood, outerWoods, deepwood);
			return Mth.lerp(this.grove, forest, hiddenGrove);
		}
	}

	public static Zone sample(WorldGenLevel level, BlockPos pos) {
		long seed = level.getSeed();
		Noises noises = noises(seed);
		float interior = interior(level, pos, seed);

		float deepNoise = smoothstep(-0.35F, 0.4F, (float) noises.deepwood.getValue(pos.getX() / DEEPWOOD_NOISE_SCALE, pos.getZ() / DEEPWOOD_NOISE_SCALE));
		float deepwood = smoothstep(0.5F, 1.0F, interior) * (0.3F + 0.7F * deepNoise);

		float groveNoise = (float) noises.grove.getValue(pos.getX() / GROVE_NOISE_SCALE, pos.getZ() / GROVE_NOISE_SCALE);
		float grove = smoothstep(GROVE_THRESHOLD_START, GROVE_THRESHOLD_FULL, groveNoise) * smoothstep(0.4F, 0.8F, interior);

		return new Zone(deepwood, grove);
	}

	/** Share of probes around the chunk that are still Whispering Woods: 1 deep inside, about 0.5 at the edge. */
	private static float interior(WorldGenLevel level, BlockPos pos, long seed) {
		ChunkPos chunk = new ChunkPos(pos);
		InteriorCache cache = INTERIOR_CACHE.get();
		if (cache.valid && cache.seed == seed && cache.chunk == chunk.toLong()) {
			return cache.value;
		}

		int centerX = chunk.getMiddleBlockX();
		int centerZ = chunk.getMiddleBlockZ();
		int quartY = QuartPos.fromBlock(pos.getY());
		int inside = 0;
		int total = 0;
		for (int ring = 0; ring < EDGE_PROBE_DISTANCES.length; ring++) {
			for (int i = 0; i < EDGE_PROBE_DIRECTIONS; i++) {
				// Rings are rotated against each other so the probes don't line up.
				float angle = (i + 0.5F * ring) * Mth.TWO_PI / EDGE_PROBE_DIRECTIONS;
				int x = centerX + Mth.floor(Mth.cos(angle) * EDGE_PROBE_DISTANCES[ring]);
				int z = centerZ + Mth.floor(Mth.sin(angle) * EDGE_PROBE_DISTANCES[ring]);
				// Probes lie outside the chunks a feature may read (WorldGenRegion throws there), so ask the
				// biome source directly, like vanilla does for positions without a chunk.
				if (level.getLevel().getUncachedNoiseBiome(QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z)).is(ValorBiomes.WHISPERING_WOODS)) {
					inside++;
				}
				total++;
			}
		}

		cache.valid = true;
		cache.seed = seed;
		cache.chunk = chunk.toLong();
		cache.value = inside / (float) total;
		return cache.value;
	}

	private static Noises noises(long seed) {
		Noises current = noises;
		if (current.seed != seed) {
			current = new Noises(seed);
			noises = current;
		}
		return current;
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	/** World-seeded noises; immutable, so one instance can be shared by all worldgen threads. */
	private static final class Noises {
		private final long seed;
		private final SimplexNoise deepwood;
		private final SimplexNoise grove;

		private Noises(long seed) {
			this.seed = seed;
			this.deepwood = new SimplexNoise(RandomSource.create(seed ^ DEEPWOOD_SALT));
			this.grove = new SimplexNoise(RandomSource.create(seed ^ GROVE_SALT));
		}
	}

	private static final class InteriorCache {
		private boolean valid;
		private long seed;
		private long chunk;
		private float value;
	}
}
