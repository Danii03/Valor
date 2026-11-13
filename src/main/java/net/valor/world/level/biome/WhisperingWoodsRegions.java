package net.valor.world.level.biome;

import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Decides where in the Overworld the Whispering Woods may appear: <b>rare, but large</b>.
 * <p>
 * Rarity and size are two separate properties:
 * <ul>
 *     <li><b>Rarity</b>: a very large-scale, world-seeded <i>region noise</i>. Only where it is above
 *     {@link #THRESHOLD} can there be Whispering Woods at all. Elsewhere the Dark Forest climate slot stays
 *     Dark Forest.</li>
 *     <li><b>Size</b>: inside a region, the forest is not limited to the (often small and patchy) temperate,
 *     very humid climate slot that vanilla fills with Dark Forest. Towards the region's heart the climate is
 *     treated as a little more humid and more temperate ({@link #shift}), so the slot grows along the
 *     natural climate gradient into the neighboring temperate forests and their patches join into one big
 *     forest. Only forests can be taken over ({@link #canGrowInto}); plains, rivers, beaches, mountains and
 *     all other biomes, and the terrain itself, stay vanilla.</li>
 * </ul>
 * Measured on several seeds (16 384² blocks each): about 2 % of all land, mostly in forests 500 to over
 * 1 000 blocks across, framed by the same vanilla neighbors a Dark Forest has.
 * <p>
 * Tuning:
 * <ul>
 *     <li>{@link #THRESHOLD} – rarity. Higher = rarer regions.</li>
 *     <li>{@link #GROWTH_RAMP}, {@link #HUMIDITY_SHIFT}, {@link #TEMPERATURE_PULL} – size. How quickly and how
 *     far a region's forest grows beyond the Dark Forest slot.</li>
 *     <li>{@link #REGION_SCALE} – how far apart regions are.</li>
 * </ul>
 * Applied by {@code MultiNoiseBiomeSourceMixin}; the climate slot itself comes from
 * {@code OverworldBiomeBuilderMixin}.
 */
public final class WhisperingWoodsRegions {
	/** Wavelength of the main region noise, in blocks. */
	private static final double REGION_SCALE = 1600.0;
	/** Finer noise that roughens region outlines, in blocks. */
	private static final double DETAIL_SCALE = 420.0;
	private static final double DETAIL_WEIGHT = 0.2;
	private static final double THRESHOLD = 0.3;
	/** Region noise above the threshold over which the forest grows to its full extent. */
	private static final double GROWTH_RAMP = 0.3;
	/** At full growth, the climate counts as this much more humid (vanilla humidity runs from -1 to 1). */
	private static final float HUMIDITY_SHIFT = 0.25F;
	/** At full growth, the temperature is pulled at most this far towards the Dark Forest band's middle. */
	private static final float TEMPERATURE_PULL = 0.08F;
	/** Middle of vanilla's temperate band (-0.15 to 0.2), where the Dark Forest slot lies. */
	private static final float TEMPERATE_MIDDLE = 0.025F;
	private static final long SALT = 0x3A11_0F_7EE5L;

	/** Temperate forests the Whispering Woods may grow into; the Dark Forest itself is handled separately. */
	private static final Set<ResourceKey<Biome>> GROWS_INTO = Set.of(
			Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
			Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA);

	private static volatile Noise noise = new Noise(0L);

	private WhisperingWoodsRegions() {}

	public static void init() {
		// The biome source has no access to the world seed, so the noise is reseeded before any chunk generates.
		ServerLifecycleEvents.SERVER_STARTING.register(server -> setSeed(server.getWorldData().worldGenOptions().seed()));
	}

	public static void setSeed(long seed) {
		if (noise.seed != seed) {
			noise = new Noise(seed);
		}
	}

	/** Whether a Dark-Forest-climate position at the given quart coordinates belongs to the Whispering Woods. */
	public static boolean contains(int quartX, int quartZ) {
		return regionValue(quartX, quartZ) > THRESHOLD;
	}

	/**
	 * How far the forest grows beyond the Dark Forest slot at the given quart coordinates: 0 outside a region,
	 * rising smoothly to 1 towards its heart.
	 */
	public static float growth(int quartX, int quartZ) {
		double t = Mth.clamp((regionValue(quartX, quartZ) - THRESHOLD) / GROWTH_RAMP, 0.0, 1.0);
		return (float) (t * t * (3.0 - 2.0 * t));
	}

	/** Whether the Whispering Woods may take over this vanilla biome when the forest grows. */
	public static boolean canGrowInto(Holder<Biome> biome) {
		return biome.unwrapKey().filter(GROWS_INTO::contains).isPresent();
	}

	/** The climate as the growing forest sees it: a little more humid and more temperate. */
	public static Climate.TargetPoint shift(Climate.TargetPoint point, float growth) {
		float temperature = Climate.unquantizeCoord(point.temperature());
		float humidity = Climate.unquantizeCoord(point.humidity());
		float pull = Mth.clamp(TEMPERATE_MIDDLE - temperature, -TEMPERATURE_PULL * growth, TEMPERATURE_PULL * growth);
		return new Climate.TargetPoint(
				Climate.quantizeCoord(temperature + pull),
				Climate.quantizeCoord(humidity + HUMIDITY_SHIFT * growth),
				point.continentalness(), point.erosion(), point.depth(), point.weirdness());
	}

	private static double regionValue(int quartX, int quartZ) {
		Noise current = noise;
		double x = QuartPos.toBlock(quartX);
		double z = QuartPos.toBlock(quartZ);
		return (1.0 - DETAIL_WEIGHT) * current.region.getValue(x / REGION_SCALE, z / REGION_SCALE)
				+ DETAIL_WEIGHT * current.detail.getValue(x / DETAIL_SCALE, z / DETAIL_SCALE);
	}

	private static final class Noise {
		private final long seed;
		private final SimplexNoise region;
		private final SimplexNoise detail;

		private Noise(long seed) {
			this.seed = seed;
			RandomSource random = RandomSource.create(seed ^ SALT);
			this.region = new SimplexNoise(random);
			this.detail = new SimplexNoise(random);
		}
	}
}
