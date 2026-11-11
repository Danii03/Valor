package net.valor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.valor.client.particle.WhisperingWoodsParticles;
import net.valor.client.renderer.ForestMistRenderer;
import net.valor.client.resources.sounds.WhisperingWoodsSoundHandler;
import net.valor.world.level.biome.ValorBiomes;

/**
 * Client-side atmosphere of the Whispering Woods: ground mist, glimmers and birdsong.
 * <p>
 * Everything is driven by the local player's position only, so there is no network traffic and every
 * player experiences the forest they are standing in. A smoothed {@link #presence()} (0–1) tells how
 * much the player is "in" the forest: it rises when entering, falls when leaving or going underground,
 * and all effects scale with it, so they fade in and out instead of switching.
 */
public final class WhisperingWoodsAmbience {
	/** Probes at the player and in a ring around them, so presence already drops near the border. */
	private static final int PROBE_RADIUS = 10;
	private static final int PROBE_DIRECTIONS = 8;
	/** Presence changes by at most this much per tick: about 3 seconds from 0 to 1. */
	private static final float FADE_PER_TICK = 1.0F / 60.0F;
	/** Sky light below this means a cave or a closed room: no forest atmosphere. */
	private static final int MIN_SKY_LIGHT = 3;
	private static final int FULL_SKY_LIGHT = 8;

	private static float presence;

	private WhisperingWoodsAmbience() {}

	/** 0 = outside, 1 = fully inside the Whispering Woods. Smoothed over a few seconds. */
	public static float presence() {
		return presence;
	}

	/** Called at the end of every client tick. */
	public static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		LocalPlayer player = minecraft.player;
		if (level == null || player == null) {
			presence = 0.0F;
			ForestMistRenderer.clear();
			WhisperingWoodsSoundHandler.reset();
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}

		float target = biomeShare(level, player.blockPosition()) * openness(level, player.blockPosition());
		presence += Mth.clamp(target - presence, -FADE_PER_TICK, FADE_PER_TICK);

		ForestMistRenderer.tick(level, player, presence);
		if (presence > 0.0F) {
			WhisperingWoodsParticles.tick(level, player, presence);
		}
		WhisperingWoodsSoundHandler.tick(minecraft, level, presence);
	}

	/** Share of probes around {@code center} that lie in the Whispering Woods. */
	private static float biomeShare(ClientLevel level, BlockPos center) {
		int inside = level.getBiome(center).is(ValorBiomes.WHISPERING_WOODS) ? 2 : 0;
		int total = 2;
		BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
		for (int i = 0; i < PROBE_DIRECTIONS; i++) {
			float angle = i * Mth.TWO_PI / PROBE_DIRECTIONS;
			probe.set(center.getX() + Mth.floor(Mth.cos(angle) * PROBE_RADIUS), center.getY(),
					center.getZ() + Mth.floor(Mth.sin(angle) * PROBE_RADIUS));
			if (level.getBiome(probe).is(ValorBiomes.WHISPERING_WOODS)) {
				inside++;
			}
			total++;
		}
		return inside / (float) total;
	}

	/** 1 under the open sky or a canopy, 0 in caves. */
	private static float openness(ClientLevel level, BlockPos pos) {
		int skyLight = level.getBrightness(LightLayer.SKY, pos);
		return Mth.clamp((skyLight - MIN_SKY_LIGHT) / (float) (FULL_SKY_LIGHT - MIN_SKY_LIGHT), 0.0F, 1.0F);
	}
}
