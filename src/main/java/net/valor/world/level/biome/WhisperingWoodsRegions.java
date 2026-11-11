package net.valor.world.level.biome;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Decides where in the Overworld the Whispering Woods may appear: <b>rare, but large</b>.
 * <p>
 * Rarity and size are two separate properties:
 * <ul>
 *     <li><b>Climate</b> (vanilla) decides <i>where a forest of this kind fits</i>: the temperate, very
 *     humid slot that vanilla fills with Dark Forest. Its regions are the size of a vanilla Dark Forest.</li>
 *     <li><b>Region noise</b> (this class) decides <i>which of those forests are Whispering Woods</i>. It is
 *     a very large-scale, world-seeded noise: where it is above {@link #THRESHOLD}, the <i>whole</i> Dark
 *     Forest climate slot becomes Whispering Woods; elsewhere it stays Dark Forest.</li>
 * </ul>
 * Because the noise varies much more slowly than the climate, a Dark Forest region is usually taken
 * completely or not at all. The result is few, but big and contiguous forests, framed by the same
 * vanilla neighbors (Forest, Birch Forest, Old Growth Taiga, …) a Dark Forest has, so the terrain and
 * transitions stay vanilla.
 * <p>
 * Tuning:
 * <ul>
 *     <li>{@link #THRESHOLD} – rarity. Higher = rarer. About 0.3 converts roughly a quarter of all Dark
 *     Forest climate; a Pale-Garden-like "lucky find".</li>
 *     <li>{@link #REGION_SCALE} – how far apart Whispering Woods regions are and how reliably a whole Dark
 *     Forest region is converted. Larger = rarer clusters, cleaner whole regions.</li>
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
	private static final long SALT = 0x3A11_0F_7EE5L;

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
		Noise current = noise;
		double x = QuartPos.toBlock(quartX);
		double z = QuartPos.toBlock(quartZ);
		double value = (1.0 - DETAIL_WEIGHT) * current.region.getValue(x / REGION_SCALE, z / REGION_SCALE)
				+ DETAIL_WEIGHT * current.detail.getValue(x / DETAIL_SCALE, z / DETAIL_SCALE);
		return value > THRESHOLD;
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
