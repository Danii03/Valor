package net.valor.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.valor.world.level.block.EchoFluteAltarBlock;
import net.valor.world.level.block.entity.EchoFluteAltarBlockEntity;

/**
 * Draws the Echo Flute hovering above its altar: turning slowly about the vertical axis and gently rising
 * and sinking, like a dropped item but calmer, and lifted clear of the stone and the flames.
 * <p>
 * Purely client-side and driven by the game time, so every player sees the same, continuous motion without
 * any extra network traffic; the flute itself comes from the block entity's synced data.
 */
public final class EchoFluteAltarRenderer implements BlockEntityRenderer<EchoFluteAltarBlockEntity> {
	/** Center height of the hover, in blocks above the altar's base. The top of the altar is at 15/16. */
	private static final float HOVER_HEIGHT = 1.15F;
	/** Half the up-and-down travel, in blocks. */
	private static final float BOB_AMPLITUDE = 0.04F;
	/** Ticks for one rise and fall (about 6 seconds). */
	private static final float BOB_PERIOD = 120.0F;
	/** Degrees per tick: one full turn in 12 seconds. */
	private static final float SPIN_SPEED = 1.5F;
	/** The ground pose of a flat item is half a block; a little larger reads better on the altar. */
	private static final float SCALE = 1.2F;
	/** The flute glows faintly: never darker than this block light. */
	private static final int MIN_BLOCK_LIGHT = 10;

	private final ItemRenderer itemRenderer;

	public EchoFluteAltarRenderer(BlockEntityRendererProvider.Context context) {
		this.itemRenderer = context.getItemRenderer();
	}

	@Override
	public void render(EchoFluteAltarBlockEntity altar, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
			int packedLight, int packedOverlay) {
		ItemStack flute = altar.getFlute();
		// The block state is the authority on whether the altar is lit; never draw a flute over an empty altar.
		if (flute.isEmpty() || !altar.getBlockState().getValue(EchoFluteAltarBlock.HAS_FLUTE)) {
			return;
		}
		Level level = altar.getLevel();
		BlockPos pos = altar.getBlockPos();
		float time = (level != null ? level.getGameTime() % 24000L : 0L) + partialTick;
		// Each altar has its own phase, so neighboring flutes don't move in lockstep.
		float phase = (Mth.getSeed(pos) & 0xFFFFL) / 65535.0F;

		float bob = Mth.sin((time / BOB_PERIOD + phase) * Mth.TWO_PI) * BOB_AMPLITUDE;
		float spin = time * SPIN_SPEED + phase * 360.0F;

		int light = level != null ? LevelRenderer.getLightColor(level, pos.above()) : packedLight;
		light = LightTexture.pack(Math.max(LightTexture.block(light), MIN_BLOCK_LIGHT), LightTexture.sky(light));

		poseStack.pushPose();
		poseStack.translate(0.5F, HOVER_HEIGHT + bob, 0.5F);
		poseStack.mulPose(Axis.YP.rotationDegrees(spin));
		poseStack.scale(SCALE, SCALE, SCALE);
		this.itemRenderer.renderStatic(flute, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, poseStack, buffers,
				level, (int) pos.asLong());
		poseStack.popPose();
	}
}
