package net.valor.client.resources.sounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.valor.client.WhisperingWoodsAmbience;

/**
 * An ambient clip played around the listener (not positional, like vanilla's biome ambience) that
 * fades in, plays for a set time and fades out again, ending before the clip itself does.
 * <p>
 * The volume also follows {@link WhisperingWoodsAmbience#presence()}, so the sound fades out when the
 * player leaves the forest or goes underground. It plays in the "Ambient/Environment" sound category.
 */
public final class FadingAmbientSoundInstance extends AbstractTickableSoundInstance {
	private static final int FADE_IN_TICKS = 30;
	private static final int FADE_OUT_TICKS = 50;

	private final float baseVolume;
	private final int playTicks;
	private int age;

	/**
	 * @param playTicks how long to play, fades included; must be shorter than the shortest clip of the event
	 */
	public FadingAmbientSoundInstance(SoundEvent event, float volume, int playTicks, RandomSource random) {
		super(event, SoundSource.AMBIENT, random);
		this.baseVolume = volume;
		this.playTicks = Math.max(playTicks, FADE_IN_TICKS + FADE_OUT_TICKS);
		this.looping = false;
		this.relative = true;
		this.attenuation = SoundInstance.Attenuation.NONE;
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;
		this.volume = 0.0F;
	}

	/** Starts at zero volume and fades in. */
	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public void tick() {
		this.age++;
		float presence = WhisperingWoodsAmbience.presence();
		if (this.age >= this.playTicks || presence <= 0.0F) {
			this.stop();
			return;
		}
		float fadeIn = Mth.clamp(this.age / (float) FADE_IN_TICKS, 0.0F, 1.0F);
		float fadeOut = Mth.clamp((this.playTicks - this.age) / (float) FADE_OUT_TICKS, 0.0F, 1.0F);
		this.volume = this.baseVolume * fadeIn * fadeOut * presence;
	}
}
