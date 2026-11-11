package net.valor.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.valor.client.renderer.EchoFluteFirstPersonRenderer;
import net.valor.client.renderer.EchoFluteHandAnimation;
import net.valor.registry.ValorItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First-person Echo Flute playing pose. While the flute is raised, the flute hand is rendered by
 * {@link EchoFluteFirstPersonRenderer} (which also draws both arms), and the other hand is lowered
 * out of view so nothing is drawn twice or clips into the flute.
 */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
	@Unique
	private static final float OTHER_HAND_DROP = 0.7F;

	@Unique
	private boolean valor$otherHandLowered;

	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void valor$renderEchoFlute(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
			float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffers,
			int light, CallbackInfo ci) {
		float progress = EchoFluteHandAnimation.progress(partialTick);
		if (progress <= 0.0F) {
			return;
		}

		if (hand == EchoFluteHandAnimation.hand()) {
			if (stack.is(ValorItems.ECHO_FLUTE)) {
				EchoFluteFirstPersonRenderer.render((ItemInHandRenderer) (Object) this, player, hand, stack,
						equippedProgress, progress, poseStack, buffers, light);
				ci.cancel();
			}
		} else if (progress >= 1.0F) {
			ci.cancel();
		} else {
			poseStack.pushPose();
			poseStack.translate(0.0F, -progress * OTHER_HAND_DROP, 0.0F);
			this.valor$otherHandLowered = true;
		}
	}

	@Inject(method = "renderArmWithItem", at = @At("RETURN"))
	private void valor$restoreOtherHand(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
			float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffers,
			int light, CallbackInfo ci) {
		if (this.valor$otherHandLowered) {
			poseStack.popPose();
			this.valor$otherHandLowered = false;
		}
	}
}
