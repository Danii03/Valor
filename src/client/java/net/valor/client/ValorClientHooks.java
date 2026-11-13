package net.valor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.valor.client.gui.screens.EchoFluteScreen;
import net.valor.client.resources.sounds.EchoFluteAltarSoundHandler;
import net.valor.platform.ClientHooks;

/** Client implementation of {@link ClientHooks.Handler}, installed by {@link ValorClient}. */
final class ValorClientHooks implements ClientHooks.Handler {

	@Override
	public void openEchoFlute(Player player, InteractionHand hand) {
		Minecraft minecraft = Minecraft.getInstance();
		if (player == minecraft.player) {
			minecraft.setScreen(new EchoFluteScreen(hand));
		}
	}

	@Override
	public void tickEchoFluteAltar(Level level, BlockPos pos, boolean lit) {
		EchoFluteAltarSoundHandler.altarTick(level, pos, lit);
	}
}
