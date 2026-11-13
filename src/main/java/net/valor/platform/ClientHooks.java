package net.valor.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Bridge from common code to client-only code.
 * <p>
 * The {@code main} source set must never reference client classes (they don't exist on a
 * dedicated server). Common code calls {@link #get()} instead, and the client entrypoint
 * installs the real implementation via {@link #set(Handler)}. On a dedicated server the
 * default no-op handler stays active.
 */
public final class ClientHooks {

	/** Client-side actions that common code may trigger. All methods default to no-ops. */
	public interface Handler {
		/** Opens the Echo Flute music screen for the local player. */
		default void openEchoFlute(Player player, InteractionHand hand) {}

		/** Called every client tick by each loaded Echo Flute altar; drives its crackling sound. */
		default void tickEchoFluteAltar(Level level, BlockPos pos, boolean lit) {}
	}

	private static Handler handler = new Handler() {};

	private ClientHooks() {}

	public static Handler get() {
		return handler;
	}

	public static void set(Handler newHandler) {
		handler = newHandler;
	}
}
