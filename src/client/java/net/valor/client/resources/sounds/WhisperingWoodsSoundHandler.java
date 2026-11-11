package net.valor.client.resources.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.valor.registry.ValorSounds;

/**
 * Birds and crows of the Whispering Woods, scheduled on the client.
 * <p>
 * Vanilla's biome "additions" sound can only play one event at a fixed per-tick chance, without fades
 * or day/night; long recordings would also play from the start every time. Instead, each recording is
 * cut into ~15 second clips (see {@code sounds.json}); a random clip is played for a random 6–11 seconds
 * with soft fades, then a silence follows:
 * <ul>
 *     <li><b>Birds</b>: silence of 10–30 s between calls. At night and in rain they mostly stay quiet.</li>
 *     <li><b>Crows</b>: silence of 60–150 s; quieter, and never started while birds are playing.</li>
 * </ul>
 * Timers only run while their kind is silent, so silences are counted from the end of a clip and
 * clips of the same kind never overlap. Birds never start during crows and vice versa, so the two
 * at most overlap during a fade.
 */
public final class WhisperingWoodsSoundHandler {
	private static final float BIRDS_VOLUME = 0.35F;
	private static final int BIRDS_MIN_PLAY = 120;
	private static final int BIRDS_MAX_PLAY = 220;
	private static final int BIRDS_MIN_SILENCE = 200;
	private static final int BIRDS_MAX_SILENCE = 600;
	private static final float BIRDS_NIGHT_CHANCE = 0.15F;
	private static final float BIRDS_RAIN_CHANCE = 0.3F;

	private static final float CROWS_VOLUME = 0.25F;
	private static final int CROWS_MIN_PLAY = 120;
	private static final int CROWS_MAX_PLAY = 200;
	private static final int CROWS_MIN_SILENCE = 1200;
	private static final int CROWS_MAX_SILENCE = 3000;

	/** Birds wait at least this long after a crow call has ended. */
	private static final int AFTER_CROWS_PAUSE = 100;
	/** First sounds after entering the forest come after a short, natural pause. */
	private static final int ENTER_MIN_DELAY = 40;
	private static final int ENTER_MAX_DELAY = 200;
	/** New clips only start once the player is well inside; running ones fade with the presence. */
	private static final float MIN_PRESENCE_TO_START = 0.6F;

	private static FadingAmbientSoundInstance birds;
	private static FadingAmbientSoundInstance crows;
	private static int birdsDelay = -1;
	private static int crowsDelay = -1;

	private WhisperingWoodsSoundHandler() {}

	public static void tick(Minecraft minecraft, ClientLevel level, float presence) {
		RandomSource random = level.random;
		SoundManager sounds = minecraft.getSoundManager();
		boolean birdsPlaying = birds != null && sounds.isActive(birds);
		boolean crowsPlaying = crows != null && sounds.isActive(crows);

		if (presence <= 0.0F) {
			// Outside: the next visit starts with fresh, short delays.
			birdsDelay = -1;
			crowsDelay = -1;
			return;
		}
		if (birdsDelay < 0) {
			birdsDelay = Mth.nextInt(random, ENTER_MIN_DELAY, ENTER_MAX_DELAY);
		}
		if (crowsDelay < 0) {
			crowsDelay = Mth.nextInt(random, ENTER_MAX_DELAY, CROWS_MIN_SILENCE);
		}
		boolean canStart = presence >= MIN_PRESENCE_TO_START;

		if (!birdsPlaying && --birdsDelay <= 0 && canStart) {
			if (!crowsPlaying && random.nextFloat() < birdsChance(level)) {
				int play = Mth.nextInt(random, BIRDS_MIN_PLAY, BIRDS_MAX_PLAY);
				birds = new FadingAmbientSoundInstance(ValorSounds.AMBIENT_WHISPERING_WOODS_BIRDS, BIRDS_VOLUME, play, random);
				sounds.play(birds);
				birdsDelay = Mth.nextInt(random, BIRDS_MIN_SILENCE, BIRDS_MAX_SILENCE);
			} else {
				birdsDelay = Mth.nextInt(random, BIRDS_MIN_SILENCE, BIRDS_MAX_SILENCE);
			}
		}

		if (!crowsPlaying && --crowsDelay <= 0 && canStart) {
			if (birdsPlaying || birds != null && sounds.isActive(birds)) {
				crowsDelay = 40; // let the birds finish first
			} else {
				int play = Mth.nextInt(random, CROWS_MIN_PLAY, CROWS_MAX_PLAY);
				crows = new FadingAmbientSoundInstance(ValorSounds.AMBIENT_WHISPERING_WOODS_CROWS, CROWS_VOLUME, play, random);
				sounds.play(crows);
				crowsDelay = Mth.nextInt(random, CROWS_MIN_SILENCE, CROWS_MAX_SILENCE);
				birdsDelay = Math.max(birdsDelay, AFTER_CROWS_PAUSE);
			}
		}
	}

	/** Birds sing by day; at night and in rain only now and then. */
	private static float birdsChance(ClientLevel level) {
		long time = level.getDayTime() % 24000L;
		boolean night = time > 12500L && time < 23500L;
		float chance = night ? BIRDS_NIGHT_CHANCE : 1.0F;
		return level.isRaining() ? chance * BIRDS_RAIN_CHANCE : chance;
	}

	/** Forgets running clips and timers, e.g. after leaving the world. The sound manager stops the clips itself. */
	public static void reset() {
		birds = null;
		crows = null;
		birdsDelay = -1;
		crowsDelay = -1;
	}
}
