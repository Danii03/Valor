package net.valor.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.valor.client.WhisperingWoodsTerrain;
import net.valor.registry.ValorParticleTypes;

/**
 * Spawns the glimmers of the Whispering Woods around the local player. (The ground mist is not a
 * particle, see {@code ForestMistRenderer}.)
 * <p>
 * Client only, no network traffic. Particles go through {@code ClientLevel#addParticle}, so the
 * vanilla particle setting (All / Decreased / Minimal) and distance culling apply as usual. The
 * number alive at once is bounded by spawn rate × lifetime: about 21 on average at most.
 */
public final class WhisperingWoodsParticles {
	/** Glimmers per tick at full presence (6 per second). */
	private static final float GLIMMER_RATE = 0.3F;
	private static final float GLIMMER_MIN_DISTANCE = 2.5F;
	private static final float GLIMMER_MAX_DISTANCE = 14.0F;
	/** Glimmers under a closed canopy are kept with this chance; under open sky (light shafts, groves) always. */
	private static final float GLIMMER_SHADED_CHANCE = 0.7F;
	/** Spawn height range above the ground, in blocks. */
	private static final int GLIMMER_MAX_HEIGHT = 4;

	private WhisperingWoodsParticles() {}

	public static void tick(ClientLevel level, LocalPlayer player, float presence) {
		RandomSource random = level.random;
		if (random.nextFloat() < GLIMMER_RATE * presence) {
			spawnGlimmer(level, player, random);
		}
	}

	private static void spawnGlimmer(ClientLevel level, LocalPlayer player, RandomSource random) {
		float angle = random.nextFloat() * Mth.TWO_PI;
		float distance = Mth.lerp(random.nextFloat(), GLIMMER_MIN_DISTANCE, GLIMMER_MAX_DISTANCE);
		int x = Mth.floor(player.getX() + Mth.cos(angle) * distance);
		int z = Mth.floor(player.getZ() + Mth.sin(angle) * distance);
		int ground = WhisperingWoodsTerrain.findGround(level, x, z, player.getBlockY());
		if (ground == WhisperingWoodsTerrain.NO_GROUND) {
			return;
		}

		BlockPos pos = new BlockPos(x, ground + random.nextInt(GLIMMER_MAX_HEIGHT), z);
		BlockState state = level.getBlockState(pos);
		if (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty()) {
			return;
		}
		if (!level.canSeeSky(pos) && random.nextFloat() > GLIMMER_SHADED_CHANCE) {
			return;
		}
		level.addParticle(ValorParticleTypes.GLIMMER, x + random.nextDouble(), pos.getY() + 0.3 + random.nextDouble() * 0.7,
				z + random.nextDouble(), 0.0, 0.0, 0.0);
	}
}
