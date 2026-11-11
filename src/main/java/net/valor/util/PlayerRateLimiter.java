package net.valor.util;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side guard against packet spam: allows at most {@code maxEvents} per player within a
 * rolling window of {@code windowTicks} server ticks. Only use on the server thread.
 */
public final class PlayerRateLimiter {
	private final int maxEvents;
	private final int windowTicks;
	/** Weak keys: entries disappear once a player object is no longer referenced. */
	private final Map<ServerPlayer, Window> windows = new WeakHashMap<>();

	public PlayerRateLimiter(int maxEvents, int windowTicks) {
		this.maxEvents = maxEvents;
		this.windowTicks = windowTicks;
	}

	/** Records an event and returns whether it is within the limit. */
	public boolean tryAcquire(ServerPlayer player) {
		int now = player.server.getTickCount();
		Window window = this.windows.computeIfAbsent(player, p -> new Window());
		if (now - window.startTick >= this.windowTicks) {
			window.startTick = now;
			window.count = 0;
		}
		return ++window.count <= this.maxEvents;
	}

	private static final class Window {
		private int startTick = Integer.MIN_VALUE / 2;
		private int count;
	}
}
