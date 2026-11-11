package net.valor.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.valor.client.renderer.EchoFluteHandAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the crosshair while the Echo Flute is raised – there is nothing to aim at and it would sit on the flute. */
@Mixin(Gui.class)
abstract class GuiMixin {
	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void valor$hideCrosshairWhilePlaying(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (EchoFluteHandAnimation.progress(deltaTracker.getGameTimeDeltaPartialTick(false)) > 0.0F) {
			ci.cancel();
		}
	}
}
