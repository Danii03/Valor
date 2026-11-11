package net.valor.world.item;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.valor.network.ClientboundEchoFluteNotePayload;
import net.valor.platform.ClientHooks;
import net.valor.registry.ValorItems;
import net.valor.util.PlayerRateLimiter;

/**
 * A flute that opens a small music interface on use (see {@code EchoFluteScreen}).
 * <p>
 * Note flow: the playing client plays the sound and spawns particles itself for instant feedback,
 * then tells the server. The server validates the note and relays sound and particles to all
 * <i>other</i> nearby players, so nothing is ever played or spawned twice.
 */
public class EchoFluteItem extends Item {

	/** Generous enough for fast playing, tight enough to stop packet spam. */
	private static final PlayerRateLimiter NOTE_RATE_LIMITER = new PlayerRateLimiter(16, 20);

	public EchoFluteItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			ClientHooks.get().openEchoFlute(player, hand);
		}
		// consume (not success): the use is handled, but the arm should not swing.
		return InteractionResultHolder.consume(player.getItemInHand(hand));
	}

	/** Server side: a player played {@code note}. Called from the network receiver. */
	public static void playNote(ServerPlayer player, InteractionHand hand, EchoFluteNote note) {
		if (!player.getItemInHand(hand).is(ValorItems.ECHO_FLUTE) || !NOTE_RATE_LIMITER.tryAcquire(player)) {
			return;
		}

		// Passing the player excludes them – their client already played the sound.
		player.level().playSound(player, player.getX(), player.getEyeY(), player.getZ(),
				note.sound(), SoundSource.RECORDS, 1.0F, note.pitch());

		ClientboundEchoFluteNotePayload payload = new ClientboundEchoFluteNotePayload(player.getId(), hand, note);
		for (ServerPlayer tracking : PlayerLookup.tracking(player)) {
			if (tracking != player) {
				ServerPlayNetworking.send(tracking, payload);
			}
		}
	}
}
