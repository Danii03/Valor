package net.valor.client.renderer;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.valor.registry.ValorItems;
import net.valor.world.item.EchoFluteNote;

/**
 * Animation state of the local player's Echo Flute in first person.
 * <ul>
 *     <li><b>Raise</b>: 0 = normal held position, 1 = playing position at the mouth. Follows the
 *     vanilla item-use state, so it rises when the music interface opens and lowers when it closes.</li>
 *     <li><b>Note motion</b>: on every note the flute slides slightly so the matching pipe sits at
 *     the mouth (low notes = long pipes), plus a tiny lift.</li>
 * </ul>
 * Raise is tick-based and interpolated with the partial tick; note motion is time-based so it
 * stays smooth regardless of tick rate.
 */
public final class EchoFluteHandAnimation {
	/** 6 ticks = 0.3 s from held to playing position and back. */
	private static final float RAISE_PER_TICK = 1.0F / 6.0F;
	private static final long PULSE_DURATION_MS = 180L;
	private static final long SHIFT_DURATION_MS = 120L;
	/** Sideways shift per note step, in blocks (camera space). */
	private static final float SHIFT_PER_NOTE = 0.012F;

	private static float raise;
	private static float raiseO;
	private static InteractionHand hand = InteractionHand.MAIN_HAND;

	private static long lastNoteMillis = Long.MIN_VALUE / 2;
	private static float shiftFrom;
	private static float shiftTo;
	private static long shiftChangedMillis = Long.MIN_VALUE / 2;

	private EchoFluteHandAnimation() {}

	/** Called at the end of every client tick. */
	public static void tick(Minecraft minecraft) {
		raiseO = raise;
		LocalPlayer player = minecraft.player;
		if (player == null) {
			reset();
			return;
		}

		boolean playing = isPlaying(player);
		if (playing) {
			hand = player.getUsedItemHand();
		}
		if (!player.getItemInHand(hand).is(ValorItems.ECHO_FLUTE)) {
			// Flute gone (slot switched, dropped): vanilla's equip animation takes over.
			reset();
			return;
		}

		raise = Mth.clamp(raise + (playing ? RAISE_PER_TICK : -RAISE_PER_TICK), 0.0F, 1.0F);
		if (!playing && shiftTo != 0.0F) {
			setShiftTarget(0.0F);
		}
	}

	public static boolean isPlaying(Player player) {
		return player.isUsingItem() && player.getUseItem().is(ValorItems.ECHO_FLUTE);
	}

	/** Eased raise progress, 0–1. */
	public static float progress(float partialTick) {
		float linear = Mth.lerp(partialTick, raiseO, raise);
		return linear * linear * (3.0F - 2.0F * linear);
	}

	/** The hand holding the flute that is (or was last) played. */
	public static InteractionHand hand() {
		return hand;
	}

	public static void onNotePlayed(EchoFluteNote note) {
		lastNoteMillis = Util.getMillis();
		int center = EchoFluteNote.values().length / 2;
		setShiftTarget((note.ordinal() - center) * SHIFT_PER_NOTE);
	}

	/** 1 right after a note, decaying to 0. */
	public static float notePulse() {
		float t = (Util.getMillis() - lastNoteMillis) / (float) PULSE_DURATION_MS;
		if (t >= 1.0F) {
			return 0.0F;
		}
		return (1.0F - t) * (1.0F - t);
	}

	/** Current sideways offset of the flute in blocks. */
	public static float noteShift() {
		float t = Mth.clamp((Util.getMillis() - shiftChangedMillis) / (float) SHIFT_DURATION_MS, 0.0F, 1.0F);
		float eased = 1.0F - (1.0F - t) * (1.0F - t);
		return Mth.lerp(eased, shiftFrom, shiftTo);
	}

	private static void setShiftTarget(float target) {
		shiftFrom = noteShift();
		shiftTo = target;
		shiftChangedMillis = Util.getMillis();
	}

	private static void reset() {
		raise = 0.0F;
		raiseO = 0.0F;
		shiftFrom = 0.0F;
		shiftTo = 0.0F;
	}
}
