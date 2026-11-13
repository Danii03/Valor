package net.valor.client.resources.sounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.valor.registry.ValorSounds;

/**
 * The soft crackle of burning Echo Flute altars.
 * <p>
 * Each lit altar reports itself every client tick (from its block entity ticker). An altar has a loudness
 * that fades in over {@link #FADE_TICKS} when it starts burning and fades out again when it stops, and while
 * it is above zero the altar keeps playing short, overlapping campfire crackles whose volume follows it. Vanilla
 * plays block sounds at a fixed volume, so the fades need these tickable instances.
 * <p>
 * An altar that stops reporting (its chunk unloaded, the block broken) fades out the same way. Changing
 * dimension or leaving the world silences everything at once. Only the {@link #MAX_AUDIBLE} nearest altars
 * crackle, so a row of altars is not louder than one or two.
 */
public final class EchoFluteAltarSoundHandler {
	/** 1.5 seconds. */
	private static final int FADE_TICKS = 30;
	private static final int MAX_AUDIBLE = 2;
	/** Matches the sound's attenuation distance in {@code sounds.json}; farther altars are not heard anyway. */
	private static final double HEARING_DISTANCE = 8.0;
	private static final float VOLUME = 0.45F;
	/** Ticks between the start of two crackles of one altar. The clips last one to two seconds, so they overlap a little. */
	private static final int MIN_CRACKLE_INTERVAL = 14;
	private static final int MAX_CRACKLE_INTERVAL = 30;

	private static final Map<BlockPos, Altar> ALTARS = new HashMap<>();
	private static final RandomSource RANDOM = RandomSource.create();
	private static Level level;
	private static long ticks;

	private EchoFluteAltarSoundHandler() {}

	/** Called by each loaded altar's block entity every client tick. */
	public static void altarTick(Level altarLevel, BlockPos pos, boolean lit) {
		if (altarLevel != level) {
			return;
		}
		Altar altar = ALTARS.computeIfAbsent(pos.immutable(), Altar::new);
		altar.reportedTick = ticks;
		altar.lit = lit;
	}

	/** End of every client tick, after the block entities have reported. */
	public static void tick(Minecraft minecraft) {
		if (minecraft.level != level || minecraft.player == null) {
			// Dimension change, or no world at all: stop at once, nothing to fade towards.
			silenceAll();
			level = minecraft.level;
			return;
		}
		if (minecraft.isPaused()) {
			// Block entities don't tick while the game is paused; keep everything as it is.
			return;
		}

		Vec3 listener = minecraft.gameRenderer.getMainCamera().getPosition();
		List<Altar> burning = new ArrayList<>();
		for (Altar altar : ALTARS.values()) {
			altar.audible = false;
			if (altar.reportedTick == ticks && altar.lit && altar.center.distanceToSqr(listener) <= HEARING_DISTANCE * HEARING_DISTANCE) {
				burning.add(altar);
			}
		}
		burning.sort(Comparator.comparingDouble(altar -> altar.center.distanceToSqr(listener)));
		for (int i = 0; i < Math.min(MAX_AUDIBLE, burning.size()); i++) {
			burning.get(i).audible = true;
		}

		Iterator<Altar> iterator = ALTARS.values().iterator();
		while (iterator.hasNext()) {
			Altar altar = iterator.next();
			float step = 1.0F / FADE_TICKS;
			altar.loudness = Mth.clamp(altar.loudness + (altar.audible ? step : -step), 0.0F, 1.0F);
			if (altar.loudness > 0.0F) {
				if (--altar.nextCrackle <= 0) {
					minecraft.getSoundManager().play(new CrackleSoundInstance(altar));
					altar.nextCrackle = Mth.nextInt(RANDOM, MIN_CRACKLE_INTERVAL, MAX_CRACKLE_INTERVAL);
				}
			} else if (altar.reportedTick != ticks || !altar.audible) {
				// Silent, and gone or out of earshot: forget it until it reports again.
				altar.gone = true;
				iterator.remove();
			}
		}
		ticks++;
	}

	private static void silenceAll() {
		for (Altar altar : ALTARS.values()) {
			altar.gone = true;
		}
		ALTARS.clear();
	}

	private static final class Altar {
		final Vec3 center;
		long reportedTick = -1;
		boolean lit;
		boolean audible;
		/** 0 = silent, 1 = full volume. */
		float loudness;
		/** Set once the altar is forgotten; its sounds stop. */
		boolean gone;
		int nextCrackle;

		Altar(BlockPos pos) {
			this.center = Vec3.atCenterOf(pos).add(0.0, 0.45, 0.0);
		}
	}

	/** One crackle clip; its volume follows the altar's loudness while it plays. */
	private static final class CrackleSoundInstance extends AbstractTickableSoundInstance {
		private final Altar altar;
		private final float baseVolume;

		CrackleSoundInstance(Altar altar) {
			super(ValorSounds.BLOCK_ECHO_FLUTE_ALTAR_CRACKLE, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
			this.altar = altar;
			this.baseVolume = VOLUME * (0.7F + RANDOM.nextFloat() * 0.6F);
			// Lower than a campfire: soul fire burns calmer.
			this.pitch = 0.75F + RANDOM.nextFloat() * 0.3F;
			this.x = altar.center.x;
			this.y = altar.center.y;
			this.z = altar.center.z;
			this.looping = false;
			this.attenuation = SoundInstance.Attenuation.LINEAR;
			this.volume = this.baseVolume * altar.loudness;
		}

		/** Starts with the altar's current loudness, which may still be close to zero. */
		@Override
		public boolean canStartSilent() {
			return true;
		}

		@Override
		public void tick() {
			if (this.altar.gone || this.altar.loudness <= 0.0F) {
				this.stop();
				return;
			}
			this.volume = this.baseVolume * this.altar.loudness;
		}
	}
}
