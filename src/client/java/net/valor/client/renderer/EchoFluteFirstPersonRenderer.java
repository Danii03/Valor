package net.valor.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * First-person rendering of the Echo Flute while playing. Called from {@code ItemInHandRendererMixin}
 * instead of vanilla's {@code renderArmWithItem} for the flute hand.
 * <p>
 * The pose is blended from vanilla's normal held pose (including the item model's first-person
 * display transform) to the playing pose in front of the mouth ({@link EchoFluteFirstPersonLayout}),
 * so there is no jump at either end. Like every held item in vanilla, the flute is drawn without
 * the arm: Minecraft-sized arms would cover the flute and the music panel.
 */
public final class EchoFluteFirstPersonRenderer {
	private static final float PULSE_LIFT = 0.006F;
	private static final float PULSE_TILT_DEGREES = 2.0F;

	// Vanilla ItemInHandRenderer#applyItemArmTransform.
	private static final float VANILLA_X = 0.56F;
	private static final float VANILLA_Y = -0.52F;
	private static final float VANILLA_Z = -0.72F;
	private static final float VANILLA_EQUIP_DROP = -0.6F;

	private EchoFluteFirstPersonRenderer() {}

	public static void render(ItemInHandRenderer renderer, AbstractClientPlayer player, InteractionHand hand,
			ItemStack stack, float equippedProgress, float progress, PoseStack poseStack, MultiBufferSource buffers,
			int light) {
		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		boolean leftHand = arm == HumanoidArm.LEFT;

		Matrix4f heldPose = vanillaHeldPose(player, stack, leftHand, equippedProgress);
		Matrix4f playPose = playingPose();

		poseStack.pushPose();
		applyBlended(poseStack, heldPose, playPose, progress);
		renderer.renderItem(player, stack, ItemDisplayContext.NONE, leftHand, poseStack, buffers, light);
		poseStack.popPose();
	}

	/** The pose vanilla uses for a held item, including the model's first-person display transform. */
	private static Matrix4f vanillaHeldPose(AbstractClientPlayer player, ItemStack stack, boolean leftHand,
			float equippedProgress) {
		float side = leftHand ? -1.0F : 1.0F;
		PoseStack held = new PoseStack();
		held.translate(side * VANILLA_X, VANILLA_Y + equippedProgress * VANILLA_EQUIP_DROP, VANILLA_Z);
		ItemDisplayContext context = leftHand ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
		Minecraft.getInstance().getItemRenderer().getModel(stack, player.level(), player, player.getId())
				.getTransforms().getTransform(context).apply(leftHand, held);
		return new Matrix4f(held.last().pose());
	}

	/** Playing pose, including the small sideways slide and lift for each played note. */
	private static Matrix4f playingPose() {
		Vector3f center = EchoFluteFirstPersonLayout.center();
		float pulse = EchoFluteHandAnimation.notePulse();
		return new Matrix4f()
				.translate(center.x + EchoFluteHandAnimation.noteShift(), center.y + pulse * PULSE_LIFT, center.z)
				.rotateX(EchoFluteFirstPersonLayout.TILT_DEGREES * Mth.DEG_TO_RAD)
				.rotateZ(pulse * PULSE_TILT_DEGREES * Mth.DEG_TO_RAD)
				.scale(EchoFluteFirstPersonLayout.SCALE);
	}

	/** Blends two translate-rotate-scale poses: position and scale linearly, rotation spherically. */
	private static void applyBlended(PoseStack poseStack, Matrix4f from, Matrix4f to, float t) {
		Vector3f position = from.getTranslation(new Vector3f()).lerp(to.getTranslation(new Vector3f()), t);
		Quaternionf rotation = from.getNormalizedRotation(new Quaternionf())
				.slerp(to.getNormalizedRotation(new Quaternionf()), t);
		Vector3f scale = from.getScale(new Vector3f()).lerp(to.getScale(new Vector3f()), t);

		poseStack.translate(position.x, position.y, position.z);
		poseStack.mulPose(rotation);
		poseStack.scale(scale.x, scale.y, scale.z);
	}
}
