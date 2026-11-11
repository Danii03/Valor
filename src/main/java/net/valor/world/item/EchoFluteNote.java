package net.valor.world.item;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ByIdMap;

/**
 * The five notes of the Echo Flute, ordered from lowest to highest.
 * <p>
 * Lives in common code on purpose: color and pitch are shared by the client UI and
 * future server-side features (note particles seen by other players, melody recognition).
 * <p>
 * The notes form a major pentatonic scale. Pitches use the note block formula
 * {@code 2^((semitone - 12) / 12)}, so {@code semitone} matches note block clicks (0–24).
 */
public enum EchoFluteNote {
	TURQUOISE(0x54D6CF, 6),
	GREEN(0x8EDD75, 8),
	GOLD(0xF3C66D, 10),
	PINK(0xE78CB7, 13),
	LAVENDER(0xA69BEC, 15);

	/** Network codec. Out-of-range ids from malformed packets clamp to a valid note. */
	public static final StreamCodec<ByteBuf, EchoFluteNote> STREAM_CODEC = ByteBufCodecs.idMapper(
			ByIdMap.continuous(EchoFluteNote::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP),
			EchoFluteNote::ordinal);

	private final int color;
	private final float pitch;

	EchoFluteNote(int color, int semitone) {
		this.color = color;
		this.pitch = (float) Math.pow(2.0, (semitone - 12) / 12.0);
	}

	/** RGB color without alpha, e.g. {@code 0x54D6CF}. */
	public int color() {
		return color;
	}

	/** Playback pitch for {@link #sound()}. */
	public float pitch() {
		return pitch;
	}

	/** Placeholder instrument until Valor ships its own flute sounds. */
	public Holder<SoundEvent> sound() {
		return SoundEvents.NOTE_BLOCK_FLUTE;
	}
}
